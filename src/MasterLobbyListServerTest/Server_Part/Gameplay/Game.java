package MasterLobbyListServerTest.Server_Part.Gameplay;

import MasterLobbyListServerTest.Server_Part.Lobby;
import MasterLobbyListServerTest.Server_Part.LobbyUser;
import MasterLobbyListServerTest.Server_Part.ServerData;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.ArrayList;

public class Game {

    public final static String newLine = System.getProperty("line.separator");

    public Model model;
    private SequentialSpace lobbySpace;

    private Thread posTargets;

    private Role guardGuessRole;
    private Role chosenRole;
    private Player currentPlayer;
    private Object[] tuple;
    private boolean legalPlay; // flag to determine of the player sends a 'legal discard'-play
    private int latestWinnerIndex; // used to determine who goes first - initialized to zero
    public Cipher cipher;

    private boolean disconnection = false;

    private ServerData serverData;

    public Game(ArrayList<LobbyUser> users, SequentialSpace lobbySpace, ServerData serverData, Cipher cipher) {
        this.model = new Model(users, lobbySpace);
        this.lobbySpace = lobbySpace;
        this.latestWinnerIndex = 0;
        posTargets = new Thread(new PossibleTargetsThread(lobbySpace,model,cipher));
        posTargets.start();
        this.serverData = serverData;
        this.cipher = cipher;
    }

    public void newRound(){
        String msg = "";

        for(Player p : model.players) {
            p.getHand().getCards().clear();
            p.getDiscardPile().getCards().clear();
            p.setInRound(true);
            p.deactivateHandmaid();
        }

        model.turn = 1;
        model.playerPointer = latestWinnerIndex;
        model.nextRound();

        model.revealedCards.clear();
        model.secretCard = null;
        model.deck.getCards().clear();
        model.deck.fillDeck();
        model.deck.shuffle();
        model.setRoundWon(false);

        if (model.players.size() == 2) {
            msg += "The revealed cards are:";
            for (int i = 0; i < model.REVEALED_CARDS_TWO_PLAYER; i++) {
                model.deck.drawCard(model.revealedCards);
                msg += " " + model.revealedCards.get(i).getRole().toString() + " ";
            }
        }

        // Secret card
        msg += newLine + "and a SECRET CARD is set aside..." + newLine;
        model.secretCard = model.deck.drawCard();

        //msg += "Each player draws a card..." + newLine;
        System.out.println(msg);
        // Each players draw a card
        for(Player p : model.players) {
            model.deck.drawCard(p.getHand());
            //System.out.println(p.getName() + " start with a " + p.getHand().getCards().get(0).getRole());
            try {
                SealedObject encryptedMessage = new SealedObject(Model.GAME_START_UPDATE + "!" + p.getHand().getCards().get(0).getRole().toString() + "?" + msg + "=", p.getPlayerCipher());

                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
            } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
        System.out.print(newLine);
    }

    public void startGame() throws InterruptedException, IOException, IllegalBlockSizeException {

        model.determineAffectionGoal();

        // Game loop
        while (model.currentMaxAffection() < model.affectionGoal) {

            newRound();

            // Round loop
            while(!model.roundWon) {

                // temp. variables for current round
                currentPlayer = model.players.get(model.indexOfCurrentPlayersTurn());
                legalPlay = false;

                if(currentPlayer.isInRound()) {
                    // States current player's turn
                    System.out.println("Round no. " + model.round + newLine + "Turn no. "
                            + (model.turn) + newLine + currentPlayer.getName() + "'s turn" + newLine);

                    // 1. DEACTIVATE POTENTIAL HANDMAID EFFECT
                    if(currentPlayer.isHandMaidProtected()) {
                        currentPlayer.deactivateHandmaid();
                    }

                    // 2. DRAW
                    model.deck.drawCard(currentPlayer.getHand());
                    Card two = currentPlayer.getHand().getCards().get(1); // temp. variable for drawn card
                    System.out.println(currentPlayer.getName() + " drew a " + two.getRole() + newLine);

                    System.out.println(currentPlayer.getName() + "'s current hand: ");
                    currentPlayer.getHand().printHand();
                    System.out.print(newLine);

                    // Inform all players about the new turn, and also send the drawn card to current player's turn
                    for(Player p : model.players){
                        String msg = "Round " + model.round + ", " + "Turn " + model.turn + " - ";
                        if(p.getName().equals(currentPlayer.getName())) {
                            try {
                                msg += "Your turn";
                                // [0] Update, [1] update type, [2] receiver, [3] Drawn card, [4] message, [5] -
                                SealedObject encryptedMessage = new SealedObject(Model.NEW_TURN + "!" + two.toString() + "?" + msg + "=", p.getPlayerCipher());
                                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                            } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                msg += model.removeIDFromPlayername(currentPlayer.getName()) + "'s turn";
                                // [0] Update, [1] update type, [2] receiver, [3] - , [4] msg, [5] -
                                SealedObject encryptedMessage = new SealedObject(Model.NEW_TURN + "!?" + msg + "=", p.getPlayerCipher());
                                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                            } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // 3. DISCARD
                    // We wait for a legal discard to happen
                    while(!legalPlay){
                        waitForDiscard();
                    }
                    legalPlay = false; // flip the flag for the next turn

                    // 4. ROUND END CHECKS
                    terminalTest();

                    model.nextTurn(); //turn only increments if a turn is executed
                }
                model.playerPointer++; // player pointer increments for every index in the players array, also knocked out players
            }
            System.out.println("Game is over");
        }

        System.out.println("Interrupting posTargets");
        Thread.sleep(3000);
        posTargets.interrupt();

        if (!disconnection) {
            String winner = model.getWinner();
            winner = winner.substring(0, winner.indexOf("#"));

            for (Player p : model.players) {
                SealedObject encryptedMessage = new SealedObject(Model.GAME_ENDING + "!" + winner + "?=", p.getPlayerCipher());

                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
            }
        }

        // Players have 30 s to receive the GAME_ENDING-tuple before the space closes
        Thread.sleep(27000);

    }

    private void waitForDiscard() throws InterruptedException, IllegalBlockSizeException {
        try {
            // [0] Update, [1] update type, [2] sender, [3] card pick index, [4] target (situational) , [5] guess (situational)
            //TODO: Lyt efter disconnect også
            //TODO: Er Model.DISCARD nødvendigt at lytte på ?
            Object[] tuple = lobbySpace.get(new ActualField(Model.SERVER_UPDATE), new FormalField(SealedObject.class), new FormalField(SealedObject.class));

            String decryptedMessage = (String) ((SealedObject)tuple[1]).getObject(cipher);

            String field1 = decryptedMessage.substring(0,decryptedMessage.indexOf('!'));
            String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!')+1,decryptedMessage.indexOf('?'));
            String field3 = decryptedMessage.substring(decryptedMessage.indexOf('?')+1,decryptedMessage.indexOf('='));
            String field4 = decryptedMessage.substring(decryptedMessage.indexOf('=')+1,decryptedMessage.indexOf('*'));
            String field5 = decryptedMessage.substring(decryptedMessage.indexOf('*')+1,decryptedMessage.indexOf('¤'));

            String[] decryptedTuple = new String[6];
            decryptedTuple[0] = "filler";
            decryptedTuple[1] = field1;
            decryptedTuple[2] = field2;
            decryptedTuple[3] = field3;
            decryptedTuple[4] = field4;
            decryptedTuple[5] = field5;

            if(Integer.parseInt(field1)== Model.DISCARD) {
                // If we receive a tuple matching the pattern, however, the sender is not the current player, we do nothing
                if(!(field2).equals(currentPlayer.getName())){
                    return;
                }

                // If the card index is legal, we proceed, else we send ACTION_DENIED tuple
                if(legalCardIndex(Integer.parseInt(field3))){

                    // temp. variable for the Role corresponding to the card index sent
                    Role r = currentPlayer.getHand().getCards().get(Integer.parseInt(field3)).getRole();

                    if(r.isTargeted()) {
                        //TODO: no possible target case could automatically launch 'noAction' (currently double checking in playCard)
                        if (!possibleTargets(r) || (validTarget(Integer.parseInt(field4),
                                currentPlayer.getHand().getCards().get(Integer.parseInt(field3)).getRole()))) {
                            playCard(currentPlayer, decryptedTuple);
                        } else {
                            // Invalid target case
                            SealedObject encryptedMessage = new SealedObject(Model.ACTION_DENIED + "!Target is invalid.?" +
                                    currentPlayer.getHand().getCards().get(0).getRole().toString() + "=" +
                                    currentPlayer.getHand().getCards().get(1).getRole().toString(), currentPlayer.getPlayerCipher());

                            lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, currentPlayer.getPlayerIndex());
                        }
                    } else {
                        playCard(currentPlayer, decryptedTuple);
                    }

                } else {
                    System.out.println("Fejl: Action denied - illegal card index");
                    // [0] update, [1] type, [2] recipient, [3] msg, [4] card one (String), [5] card two (String)
                    SealedObject encryptedMessage = new SealedObject(Model.ACTION_DENIED + "!Card index is unvalid.?" +
                            currentPlayer.getHand().getCards().get(0).getRole().toString() + "=" +
                            currentPlayer.getHand().getCards().get(1).getRole().toString(), currentPlayer.getPlayerCipher());

                    lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, currentPlayer.getPlayerIndex());
                }

            } else if(Integer.parseInt(field1)== Lobby.DISCONNECT) {
                System.out.println("Oh-oh... " + field2 + " disconnected");

                // Notify everyone, except the disconnected user
                for (Player p : model.players) {
                    if (!p.getName().equals(field2)) {
                        SealedObject encryptedMessage = new SealedObject(Model.GAME_DISCONNECT + "!?=", p.getPlayerCipher());
                        lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                    }
                }

                legalPlay = true;
                disconnection = true;
                model.setRoundWon(true);
                for (int i = 0; i < model.affectionGoal + 1; i++) {
                    model.players.get(0).incrementAffection();
                }
                //serverData.decrementCurrentNoThreads();
            }

        } catch (InterruptedException | BadPaddingException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean validTarget(int targetPlayerIndex, Role role){
        if(targetPlayerIndex > model.players.size()-1 || targetPlayerIndex < 0){
            return false;
        } else {
            Player target = model.players.get(targetPlayerIndex);
            if(role == Role.PRINCE){
                return !target.isHandMaidProtected() && target.isInRound();
            } else {
                return !target.isHandMaidProtected() && target.isInRound() && !target.isMe(currentPlayer.getName());
            }
        }
    }

    private void playCard(Player currentPlayer, Object[] tuple) throws IOException, IllegalBlockSizeException {
        if(!legalCardIndex(Integer.parseInt((String)tuple[3]))){
            // send error tuple eller vælg random...
            try {
                SealedObject encryptedMessage = new SealedObject(Model.ACTION_DENIED + "!Card index is unvalid.?" +
                        currentPlayer.getHand().getCards().get(0).getRole().toString() + "=" +
                        currentPlayer.getHand().getCards().get(1).getRole().toString(), currentPlayer.getPlayerCipher());

                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, currentPlayer.getPlayerIndex());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if(model.countessRule(currentPlayer) &&
                (currentPlayer.getHand().getCards().get(Integer.parseInt((String) tuple[3])).getRole() == Role.PRINCE ||
                        currentPlayer.getHand().getCards().get(Integer.parseInt((String) tuple[3])).getRole() == Role.KING)){

            try {
                SealedObject encryptedMessage = new SealedObject(Model.ACTION_DENIED + "!Countess rule is in play?" +
                        currentPlayer.getHand().getCards().get(0).getRole().toString() + "=" +
                        currentPlayer.getHand().getCards().get(1).getRole().toString(), currentPlayer.getPlayerCipher());

                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, currentPlayer.getPlayerIndex());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /*
            if(currentPlayer.getHand().getCards().get(0).getRole() == Role.COUNTESS){
                System.out.println("We force " + currentPlayer.getHand().getCards().get(0).getRole() + " discard");
                playUntargettedCard(currentPlayer.getHand().getCards().get(0).getRole(),currentPlayer,0);
            } else {
                System.out.println("We force " + currentPlayer.getHand().getCards().get(1).getRole() + " discard");
                playUntargettedCard(currentPlayer.getHand().getCards().get(1).getRole(),currentPlayer,1);
            }
            */

        } else {
            chosenRole = currentPlayer.getHand().getCards().get(Integer.parseInt((String)tuple[3])).getRole();
            int cardIndex = Integer.parseInt((String)tuple[3]);

            if(!currentPlayer.getHand().getCards().get(Integer.parseInt((String)tuple[3])).getRole().isTargeted()){
                playUntargettedCard(chosenRole, currentPlayer, cardIndex);
            } else {
                // ikke-Guard
                if(tuple[5].equals("")){
                    playTargettedCard(chosenRole, currentPlayer, cardIndex, ((String) tuple[4]), 0);
                } else { //Guard
                    playTargettedCard(chosenRole, currentPlayer, cardIndex, ((String) tuple[4]), Integer.parseInt((String) tuple[5]));
                }
            }
        }
    }

    private void playUntargettedCard(Role chosenRole, Player currentPlayer, int cardIndex){
        if(chosenRole == Role.HANDMAID){
            System.out.println(currentPlayer.getName() + " is handmaid protected until next turn...");
            model.handmaidAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        } else if(chosenRole == Role.COUNTESS) {
            model.countessAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        } else {
            // Princess
            model.princessAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        }
        legalPlay = true;
    }

    private void playTargettedCard(Role chosenRole, Player currentPlayer, int cardIndex, String playerTargetIndex, int guardGuess) throws IOException, IllegalBlockSizeException {

        System.out.println("Targetted card was played");

        if(!possibleTargets(chosenRole)){
            // play targetted card with no action
            System.out.println("FØRSTE GUARD FEJL");
            model.noAction(model.indexOfCurrentPlayersTurn(), cardIndex);
            legalPlay = true;
        } else {

            int playerTargetIndexInt = Integer.parseInt(playerTargetIndex);

            if(validTarget(playerTargetIndexInt, currentPlayer.getHand().getCards().get(cardIndex).getRole())) {
                if(chosenRole == Role.GUARD && (guardGuess >= 1 && guardGuess <= 7)) {
                    System.out.println("It was a guard");
                    guardGuessRole = Role.values()[guardGuess];
                    //System.out.println("You guessed " + Role.values()[guardGuess]);
                    model.guardAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt, guardGuessRole);
                    legalPlay = true;
                } else if(chosenRole == Role.PRIEST) {
                    model.priestAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else if(chosenRole == Role.BARON) {
                    //System.out.println("Player index " + model.indexOfCurrentPlayersTurn() + " card index " + (cardPick-1) + " player index" + (playerPick-1));
                    model.baronAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else if(chosenRole == Role.PRINCE) {
                    model.princeAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else if(chosenRole == Role.KING) { // i.e. chosenRole == Role.KING
                    //currentPlayer.getHand().printHand();
                    System.out.println(currentPlayer.getName() + " gets " + model.players.get(playerTargetIndexInt).getHand().getCards().get(0).getRole());
                    System.out.println(model.players.get(playerTargetIndexInt).getName() + " gets " + currentPlayer.getHand().getCards().get(cardIndex%2).getRole());
                    model.kingAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else {
                    //ACTION DENIED
                    System.out.println("Invalid Guard guess");
                    try {
                        SealedObject encryptedMessage = new SealedObject(Model.ACTION_DENIED + "!Invalid guard guess.?" +
                                currentPlayer.getHand().getCards().get(0).getRole().toString() + "=" +
                                currentPlayer.getHand().getCards().get(1).getRole().toString(), currentPlayer.getPlayerCipher());

                        lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, currentPlayer.getPlayerIndex());

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            } else {
                //ACTION DENIED
                System.out.println("Seems like an unvalid target");
                try {
                    SealedObject encryptedMessage = new SealedObject(Model.ACTION_DENIED + "!Card index is unvalid.?" +
                            currentPlayer.getHand().getCards().get(0).getRole().toString() + "=" +
                            currentPlayer.getHand().getCards().get(1).getRole().toString(), currentPlayer.getPlayerCipher());

                    lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, currentPlayer.getPlayerIndex());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void terminalTest() throws IOException, IllegalBlockSizeException {
        // 4.1 LMS
        if(model.isLastManStanding()) {

            model.lastMan().incrementAffection();

            String msgOthers = model.removeIDFromPlayername(model.lastMan().getName()) + " won the round as last man standing!" +
                    model.removeIDFromPlayername(model.lastMan().getName()) + "'s affection is now " + model.lastMan().getAffection()
                    + ". Just " + (model.affectionGoal-model.lastMan().getAffection()) + " points away from winning!";
            String msgWinner = "You won the round as last man standing! Your affection is now " + model.lastMan().getAffection()
                    + ". Just " + (model.affectionGoal-model.lastMan().getAffection()) + " points away from winning!";

            model.setRoundWon(true);
            latestWinnerIndex = model.playersIndex(model.lastMan());

            for(Player p : model.players){
                if(p.getName().equals(model.lastMan().getName())){
                    try {
                        SealedObject encryptedMessage = new SealedObject(Model.WIN + "!" + msgWinner + "?=", p.getPlayerCipher());

                        lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // [0] update [1] type [2] tuple recipient [3] Game Log msg [4] winner's name [5] winner's affection
                        SealedObject encryptedMessage = new SealedObject(Model.WIN + "!" + msgOthers + "?=", p.getPlayerCipher());

                        lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // 4.2 EMPTY DECK
        } else if(model.deck.getCards().isEmpty()) {
            if(model.nearestToPrincess().size() == 1){
                model.nearestToPrincess().get(0).incrementAffection();

                String msgOthers = "The deck is empty! " + model.removeIDFromPlayername(model.nearestToPrincess().get(0).getName()) + " won the round with the highest card!" +
                        model.removeIDFromPlayername(model.nearestToPrincess().get(0).getName()) + "'s affection is now " + model.nearestToPrincess().get(0).getAffection()
                        + ". Just " + (model.affectionGoal-model.nearestToPrincess().get(0).getAffection()) + " points away from winning!";;
                String msgWinner = "The deck is empty! You won the round with the highest card!" +
                        "Your affection is now " + model.nearestToPrincess().get(0).getAffection()
                        + ". Just " + (model.affectionGoal-model.nearestToPrincess().get(0).getAffection()) + " points away from winning!";;

                model.setRoundWon(true);

                for(Player p : model.players){
                    if(p.getName().equals(model.lastMan().getName())){
                        try {
                            // [0] update [1] type [2] tuple recipient [3] Message for GameLog [4] winner's name [5] winner's affection
                            SealedObject encryptedMessage = new SealedObject(Model.WIN + "!" + msgWinner + "?=", p.getPlayerCipher());

                            lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            SealedObject encryptedMessage = new SealedObject(Model.WIN + "!" + msgOthers + "?=", p.getPlayerCipher());

                            lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if(model.nearestToPrincess().size() > 1) {
                if(model.compareHandWinners().size() == 1) {
                    model.compareHandWinners().get(0).incrementAffection();

                    String msgOthers = "The deck is empty! " + model.removeIDFromPlayername(model.compareHandWinners().get(0).getName()) +
                            " won the round with the highest discard pile! " +
                            model.removeIDFromPlayername(model.compareHandWinners().get(0).getName()) + "'s affection is now " +
                            model.compareHandWinners().get(0).getAffection()
                            + ". Just " + (model.affectionGoal-model.compareHandWinners().get(0).getAffection()) + " points away from winning!";
                    String msgWinner = "The deck is empty! " + " You won the round with the highest discard pile! " +
                            "Your affection is now " + model.compareHandWinners().get(0).getAffection()
                            + ". Just " + (model.affectionGoal-model.compareHandWinners().get(0).getAffection()) + " points away from winning!";
                    model.setRoundWon(true);

                    for(Player p : model.players){
                        if(p.getName().equals(model.lastMan().getName())) {
                            try {
                                // [0] update [1] type [2] tuple recipient [3] Message for GameLog [4] winner's name [5] winner's affection
                                SealedObject encryptedMessage = new SealedObject(Model.WIN + "!" + msgWinner + "?=", p.getPlayerCipher());

                                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // [0] update [1] type [2] tuple recipient [3] Message for GameLog [4] winner's name [5] winner's affection
                            try {
                                SealedObject encryptedMessage = new SealedObject(Model.WIN + "!" + msgOthers + "?=", p.getPlayerCipher());

                                lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    model.setRoundWon(true); // corresponds to draw...
                    String msg = "Cards and discard piles were tied! No one wins the round.";
                    for(Player p : model.players){
                        try {
                            // [0] update [1] type [2] tuple recipient [3] Game log msg [4] - [5] -
                            SealedObject encryptedMessage = new SealedObject(Model.WIN + "!" + msg + "?=", p.getPlayerCipher());

                            lobbySpace.put(Model.CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        }
    }

    private boolean possibleTargets(Role r){
        if(r == Role.PRINCE){
            return true;
        } else {
            for(Player p : model.players){
                if(p.isInRound() && !p.isHandMaidProtected() && !p.isMe(currentPlayer.getName())){
                    return true;
                }
            }
            return false;
        }
    }

    private boolean legalCardIndex(int index){
        return (index == 0 || index == 1);
    }
}
