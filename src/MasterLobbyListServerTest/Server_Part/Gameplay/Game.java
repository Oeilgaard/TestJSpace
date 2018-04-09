package MasterLobbyListServerTest.Server_Part.Gameplay;

import MasterLobbyListServerTest.Server_Part.ServerData;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.ArrayList;

public class Game {

    public final static String newLine = System.getProperty("line.separator");

    private Model model;
    private SequentialSpace lobbySpace;

    private Thread posTargets;

    private Character guardGuessCharacter;
    private Character chosenCharacter;
    private Player currentPlayer;
    private Object[] tuple;
    private boolean legalPlay;
    private int latestWinnerIndex; // used to determine who goes first - initialized to zero

    private ServerData serverData;

    public Game(ArrayList<String> players, SequentialSpace lobbySpace, ServerData serverData) {
        this.model = new Model(players, lobbySpace);
        this.lobbySpace = lobbySpace;
        this.latestWinnerIndex = 0;
        posTargets = new Thread(new PossibleTargetsThread(lobbySpace,model,serverData));
        posTargets.start();
        this.serverData = serverData;
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

        msg += "The revealed cards are:";
        for(int i = 0; i < model.REVEALED_CARDS_TWO_PLAYER; i++) {
            model.deck.drawCard(model.revealedCards);
            msg += " " + model.revealedCards.get(i).getCharacter().toString() + " ";
        }

        // Secret card
        msg += newLine + "and a SECRET CARD is set aside..." + newLine;
        model.secretCard = model.deck.drawCard();


        //msg += "Each player draws a card..." + newLine;
        System.out.println(msg);
        // Each players draw a card
        for(Player p : model.players) {
            model.deck.drawCard(p.getHand());
            //System.out.println(p.getName() + " start with a " + p.getHand().getCards().get(0).getCharacter());
            //msg += "you start with " + p.getHand().getCards().get(0).getCharacter().toString();
            try {
                lobbySpace.put(Model.CLIENT_UPDATE, Model.GAME_START_UPDATE, p.getName(),
                        p.getHand().getCards().get(0).getCharacter().toString(), msg, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.print(newLine);
    }

    public void startGame() throws InterruptedException {

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

                    if(currentPlayer.isHandMaidProtected()) {
                        currentPlayer.deactivateHandmaid();
                    }

                    // States current player's turn
                    System.out.println("Round no. " + model.round + newLine + "Turn no. "
                            + (model.turn) + newLine + currentPlayer.getName() + "'s turn" + newLine);

                    // 1. DRAW
                    model.deck.drawCard(currentPlayer.getHand());
                    Card two = currentPlayer.getHand().getCards().get(1);
                    System.out.println(currentPlayer.getName() + " drew a " + two.getCharacter() + newLine);

                    System.out.println(currentPlayer.getName() + "'s current hand: ");
                    currentPlayer.getHand().printHand();
                    System.out.print(newLine);

                    for(Player p : model.players){
                        String msg = "Round " + model.round + ", " + "Turn " + model.turn + " - ";
                        if(p.getName().equals(currentPlayer.getName())) {
                            try {
                                msg += "Your turn";
                                // [0] Update, [1] update type, [2] receiver, [3] Drawn card, [4] message, [5] -
                                lobbySpace.put(Model.CLIENT_UPDATE, Model.NEW_TURN, p.getName(), two.toString(), msg, "");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                msg += model.removeIDFromPlayername(currentPlayer.getName()) + "'s turn";
                                // [0] Update, [1] update type, [2] receiver, [3] - , [4] msg, [5] -
                                lobbySpace.put(Model.CLIENT_UPDATE, Model.NEW_TURN, p.getName(), "", msg, "");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // 2. DISCARD

                    // TODO: DO SANITY CHECK OF PLAY

                    while(!legalPlay){
                        waitForDiscard();
                    }
                    legalPlay = false;

                    // 3. ROUND END CHECKS
                    terminalTest();

                    model.nextTurn(); //turn only increments if a turn is executed
                }
                model.playerPointer++; // player pointer increments for every index in the players array
            }
            System.out.println("Game is over");
        }

        posTargets.interrupt();

        String winner = model.getWinner();
        winner = winner.substring(0,winner.indexOf("#"));

        for(Player p : model.players) {
            lobbySpace.put(Model.CLIENT_UPDATE, Model.GAME_ENDING, p.getName(),winner,"","");
        }

        serverData.decrementCurrentNoThreads();

        // Players have 30 s to receive the GAME_ENDING-tuple before the space closes
        Thread.sleep(30000);

    }

    private void waitForDiscard() throws InterruptedException {
        try {
            // [0] Update, [1] update type, [2] sender, [3] card pick index, [4] target (situational) , [5] guess (situational)
            //TODO: Lyt efter disconnect også
            //TODO: Er Model.DISCARD nødvendigt at lytte på ?
            Object[] tuple = lobbySpace.get(new ActualField(Model.SERVER_UPDATE), new FormalField(SealedObject.class));

            String decryptedMessage = (String) ((SealedObject)tuple[1]).getObject(serverData.cipher);

            String field1 = decryptedMessage.substring(0,decryptedMessage.indexOf('!'));
            String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!')+1,decryptedMessage.indexOf('?'));
            String field3 = decryptedMessage.substring(decryptedMessage.indexOf('?')+1,decryptedMessage.indexOf('='));
            String field4 = decryptedMessage.substring(decryptedMessage.indexOf('=')+1,decryptedMessage.indexOf('*'));
            String field5 = decryptedMessage.substring(decryptedMessage.indexOf('*')+1,decryptedMessage.length());

            String[] decryptedTuple = new String[6];
            decryptedTuple[0] = "filler";
            decryptedTuple[1] = field1;
            decryptedTuple[2] = field2;
            decryptedTuple[3] = field3;
            decryptedTuple[4] = field4;
            decryptedTuple[5] = field5;

            if(Integer.parseInt(field1)==model.DISCARD) {
                // If we receive a tuple matching the pattern, however, the sender is not the current player, we do nothing
                if(!(field2).equals(currentPlayer.getName())){
                    return;
                }

                // If the card index is legal, we proceed, else we send ACTION_DENIED tuple
                if(legalCardIndex(Integer.parseInt((String)field3))){

                    // temp. variable for the Character corresponding to the card index sent
                    Character c = currentPlayer.getHand().getCards().get(Integer.parseInt((String) field3)).getCharacter();

                    if(c.isTargeted()){
                        //TODO: no possible target case could automitically launch 'noAction' (currently double checking in playCard)
                        if(!possibleTargets(c) || (validTarget(Integer.parseInt((String) field4),
                                currentPlayer.getHand().getCards().get(Integer.parseInt((String) field3)).getCharacter()))){
                            playCard(currentPlayer, decryptedTuple);
                        } else {
                            // Unvalid target case
                            lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Target is unvalid.",
                                    currentPlayer.getHand().getCards().get(0).getCharacter().toString(),
                                    currentPlayer.getHand().getCards().get(1).getCharacter().toString());
                        }
                    } else {
                        playCard(currentPlayer, decryptedTuple);
                    }

                } else {
                    System.out.println("Fejl: Action denied - illegal card index");
                    // [0] update, [1] type, [2] recipient, [3] msg, [4] card one (String), [5] card two (String)
                    lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Card index is unvalid.",
                            currentPlayer.getHand().getCards().get(0).getCharacter().toString(),
                            currentPlayer.getHand().getCards().get(1).getCharacter().toString());
                }
            } else if(Integer.parseInt(field1)==Model.GAME_DISCONNECT){
                System.out.println("Oh-oh... " + field2 + " disconnected");

                // Notify everyone, except the disconnected user
                for(Player p : model.players){
                    if(!p.getName().equals(field2)){
                        //TODO: Encrypt?
                        lobbySpace.put(Model.CLIENT_UPDATE, Model.GAME_DISCONNECT, p.getName(), currentPlayer.getName(), "", "");
                    }
                }
                serverData.decrementCurrentNoThreads();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean validTarget(int targetPlayerIndex, Character character){
        if(targetPlayerIndex > model.players.size()-1 || targetPlayerIndex < 0){
            return false;
        } else {
            Player target = model.players.get(targetPlayerIndex);
            if(character == Character.PRINCE){
                return !target.isHandMaidProtected() && target.isInRound();
            } else {
                return !target.isHandMaidProtected() && target.isInRound() && !target.isMe(currentPlayer.getName());
            }
        }
    }

    private void playCard(Player currentPlayer, Object[] tuple){
        if(!legalCardIndex(Integer.parseInt((String)tuple[3]))){
            // send error tuple eller vælg random...
            try {
                lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Card index is unvalid.",
                        currentPlayer.getHand().getCards().get(0).getCharacter().toString(),
                        currentPlayer.getHand().getCards().get(1).getCharacter().toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if(model.countessRule(currentPlayer) &&
                (currentPlayer.getHand().getCards().get(Integer.parseInt((String) tuple[3])).getCharacter() == Character.PRINCE ||
                currentPlayer.getHand().getCards().get(Integer.parseInt((String) tuple[3])).getCharacter() == Character.KING)){

            System.out.println("linie 236");

            try {
                lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Card index is unvalid.",
                        currentPlayer.getHand().getCards().get(0).getCharacter().toString(),
                        currentPlayer.getHand().getCards().get(1).getCharacter().toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /*
            if(currentPlayer.getHand().getCards().get(0).getCharacter() == Character.COUNTESS){
                System.out.println("We force " + currentPlayer.getHand().getCards().get(0).getCharacter() + " discard");
                playUntargettedCard(currentPlayer.getHand().getCards().get(0).getCharacter(),currentPlayer,0);
            } else {
                System.out.println("We force " + currentPlayer.getHand().getCards().get(1).getCharacter() + " discard");
                playUntargettedCard(currentPlayer.getHand().getCards().get(1).getCharacter(),currentPlayer,1);
            }
            */

        } else {
            chosenCharacter = currentPlayer.getHand().getCards().get(Integer.parseInt((String)tuple[3])).getCharacter();
            int cardIndex = Integer.parseInt((String)tuple[3]);

            if(!currentPlayer.getHand().getCards().get(Integer.parseInt((String)tuple[3])).getCharacter().isTargeted()){
                playUntargettedCard(chosenCharacter, currentPlayer, cardIndex);
            } else {
                // ikke-Guard
                if(tuple[5].equals("")){
                    playTargettedCard(chosenCharacter, currentPlayer, cardIndex, ((String) tuple[4]), 0);
                } else { //Guard
                    playTargettedCard(chosenCharacter, currentPlayer, cardIndex, ((String) tuple[4]), Integer.parseInt((String) tuple[5]));
                }
            }
        }
    }

    private void playUntargettedCard(Character chosenCharacter, Player currentPlayer, int cardIndex){
        if(chosenCharacter == Character.HANDMAID){
            System.out.println(currentPlayer.getName() + " is handmaid protected until next turn...");
            model.handmaidAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        } else if(chosenCharacter == Character.COUNTESS) {
            model.countessAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        } else {
            // Princess
            model.princessAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        }
        legalPlay = true;
    }

    private void playTargettedCard(Character chosenCharacter, Player currentPlayer, int cardIndex, String playerTargetIndex, int guardGuess) {

        System.out.println("Targetted card was played");

        if(!possibleTargets(chosenCharacter)){
            // play targetted card with no action
            System.out.println("FØRSTE GUARD FEJL");
            model.noAction(model.indexOfCurrentPlayersTurn(), cardIndex);
            legalPlay = true;
        } else {

            int playerTargetIndexInt = Integer.parseInt(playerTargetIndex);

            if(validTarget(playerTargetIndexInt, currentPlayer.getHand().getCards().get(cardIndex).getCharacter())) {
                if(chosenCharacter == Character.GUARD && (guardGuess >= 1 && guardGuess <= 7)) {
                    System.out.println("It was a guard");
                    guardGuessCharacter = Character.values()[guardGuess];
                    //System.out.println("You guessed " + Character.values()[guardGuess]);
                    model.guardAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt, guardGuessCharacter);
                    legalPlay = true;
                } else if(chosenCharacter == Character.PRIEST) {
                    model.priestAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else if(chosenCharacter == Character.BARON) {
                    //System.out.println("Player index " + model.indexOfCurrentPlayersTurn() + " card index " + (cardPick-1) + " player index" + (playerPick-1));
                    model.baronAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else if(chosenCharacter == Character.PRINCE) {
                    model.princeAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else if(chosenCharacter == Character.KING) { // i.e. chosenCharacter == Character.KING
                    //currentPlayer.getHand().printHand();
                    System.out.println(currentPlayer.getName() + " gets " + model.players.get(playerTargetIndexInt).getHand().getCards().get(0).getCharacter());
                    System.out.println(model.players.get(playerTargetIndexInt).getName() + " gets " + currentPlayer.getHand().getCards().get(cardIndex%2).getCharacter());
                    model.kingAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndexInt);
                    legalPlay = true;
                } else {
                    //ACTION DENIED
                    System.out.println("Invalid Guard guess");
                    try {
                        lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Invalid Guard guess.",
                                currentPlayer.getHand().getCards().get(0).getCharacter().toString(),
                                currentPlayer.getHand().getCards().get(1).getCharacter().toString());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            } else {
                //ACTION DENIED
                System.out.println("Seems like an unvalid target");
                try {
                    lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Card index is unvalid.",
                            currentPlayer.getHand().getCards().get(0).getCharacter().toString(),
                            currentPlayer.getHand().getCards().get(1).getCharacter().toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void terminalTest(){
        // 3.1 LMS
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
                        lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), msgWinner, model.lastMan().getName(),
                                Integer.toString(model.lastMan().getAffection()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        // [0] update [1] type [2] tuple recipient [3] Game Log msg [4] winner's name [5] winner's affection
                        lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), msgOthers, model.lastMan().getName(),
                                Integer.toString(model.lastMan().getAffection()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // 3.2 EMPTY DECK
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
                            lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), msgWinner,
                                    model.nearestToPrincess().get(0).getName(), Integer.toString(model.nearestToPrincess().get(0).getAffection()));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), msgOthers,
                                    model.nearestToPrincess().get(0).getName(), Integer.toString(model.nearestToPrincess().get(0).getAffection()));
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
                            + ". Just " + (model.affectionGoal-model.compareHandWinners().get(0).getAffection()) + " points away from winning!";;
                    model.setRoundWon(true);

                    for(Player p : model.players){
                        if(p.getName().equals(model.lastMan().getName())) {
                            try {
                                // [0] update [1] type [2] tuple recipient [3] Message for GameLog [4] winner's name [5] winner's affection
                                lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), msgWinner,
                                        model.compareHandWinners().get(0).getName(), Integer.toString(model.compareHandWinners().get(0).getAffection()));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // [0] update [1] type [2] tuple recipient [3] Message for GameLog [4] winner's name [5] winner's affection
                            try {
                                lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), msgOthers,
                                        model.compareHandWinners().get(0).getName(), Integer.toString(model.compareHandWinners().get(0).getAffection()));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                model.setRoundWon(true); // corresponds to draw...
                String msg = "Cards and discard piles were tied! No one wins the round.";
                for(Player p : model.players){
                    try {
                        // [0] update [1] type [2] tuple recipient [3] Game log msg [4] - [5] -
                        lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), msg, "", "");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    private boolean possibleTargets(Character c){
        if(c == Character.PRINCE){
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
