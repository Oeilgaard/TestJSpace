package MasterLobbyListServerTest.Server_Part.Gameplay;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
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

    public Game(ArrayList<String> players, SequentialSpace lobbySpace) {
        this.model = new Model(players, lobbySpace);
        this.lobbySpace = lobbySpace;
        posTargets = new Thread(new PossibleTargetsThread(lobbySpace,model));
        posTargets.start();
    }

    public void newRound(){
        String msg = "";

        //msg += "Shuffling cards...";

        for(Player p : model.players) {
            p.getHand().getCards().clear();
            p.getDiscardPile().getCards().clear();
            p.setInRound(true);
            p.deactivateHandmaid();
        }

        model.turn = 1;
        model.playerPointer = 0;
        Model.nextRound();

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
        msg += newLine + "and a secret card is set aside..." + newLine;
        model.secretCard = model.deck.drawCard();


        msg += "Each player draws a card..." + newLine;
        System.out.println(msg);
        // Each players draw a card
        for(Player p : model.players) {
            model.deck.drawCard(p.getHand());
            //System.out.println(p.getName() + " start with a " + p.getHand().getCards().get(0).getCharacter());
            msg += "you start with " + p.getHand().getCards().get(0).getCharacter().toString();
            try {
                lobbySpace.put(Model.CLIENT_UPDATE, Model.GAME_START_UPDATE, p.getName(),
                        p.getHand().getCards().get(0).getCharacter().toString(), msg, "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.print(newLine);
    }

    public void startGame() {

        model.determineAffectionGoal();

        // Game loop
        while (model.currentMaxAffection()< model.affectionGoal) {

            newRound();

            // Round loop
            while(!model.roundWon) {

                // temp. variables for current round
                currentPlayer = model.players.get(model.indexOfCurrentPlayersTurn());

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
                        if(p.getName()==currentPlayer.getName()) {
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
                    try {
                        // [0] Update, [1] update type, [2] sender, [3] card pick index, [4] target (situational) , [5] guess (situational)
                        //TODO: Lyt efter disconnect også
                        Object[] tuple = lobbySpace.get(new ActualField(Model.SERVER_UPDATE), new ActualField(Model.DISCARD),
                                new FormalField(String.class), new FormalField(String.class),
                                new FormalField(String.class), new FormalField(String.class));


//                        if(validTarget((int) tuple[4], (String) tuple[3])){
//                            chosenCharacter = currentPlayer.getHand().getCards().get((int) tuple[3]).getCharacter();
//                        } else {
//                            // Send tuple der requester nyt kort...
//                        }
                        if(legalCardIndex(Integer.parseInt((String)tuple[3]))){
                            if(currentPlayer.getHand().getCards().get(Integer.parseInt((String) tuple[3])).getCharacter().isTargeted()){
                                //TODO: no possible target case could automitically launch 'noAction' (currently double checking in playCard)
                                if(!possibleTargets() || (validTarget(Integer.parseInt((String) tuple[4]),
                                        currentPlayer.getHand().getCards().get(Integer.parseInt((String) tuple[3])).getCharacter()))){
                                    playCard(currentPlayer, tuple);
                                } else {
                                    // request ny tuple
                                    System.out.println("Fejl: Action denied linje 156");
                                    lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Target is unvalid.", "", "");
                                }
                            } else {
                                System.out.println("Linje 160 ");
                                playCard(currentPlayer, tuple);
                            }
                        } else {
                            System.out.println("Fejl: Action denied");
                            lobbySpace.put(Model.CLIENT_UPDATE, Model.ACTION_DENIED, currentPlayer.getName(), "Card pick is unvalid.", "", "");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 3. ROUND END CHECKS
                    terminalTest();

                    Model.nextTurn(); //turn only increments if a turn is executed
                }
                model.playerPointer++; // player pointer increments for every index in the players array
            }
            System.out.println("Game is over");

        }

        posTargets.interrupt();

    }

    private boolean validTarget(int targetPlayerIndex, Character character){
        Player target = model.players.get(targetPlayerIndex);
        if(character == Character.PRINCE){
            return !target.isHandMaidProtected() && target.isInRound();
        } else {
            return !target.isHandMaidProtected() && target.isInRound() && !target.isMe(currentPlayer.getName());
        }
    }

    private void playCard(Player currentPlayer, Object[] tuple){
        if(!legalCardIndex(Integer.parseInt((String)tuple[3]))){
            // send error tuple eller vælg random...
        } else if(model.countessRule(currentPlayer)){ // if Countess-rule is occurring, we force the play
            if(currentPlayer.getHand().getCards().get(0).getCharacter() == Character.COUNTESS){
                playUntargettedCard(currentPlayer.getHand().getCards().get(1).getCharacter(),currentPlayer,1);
            } else {
                playUntargettedCard(currentPlayer.getHand().getCards().get(0).getCharacter(),currentPlayer,0);
            }
        } else {
            chosenCharacter = currentPlayer.getHand().getCards().get(Integer.parseInt((String)tuple[3])).getCharacter();
            int cardIndex = Integer.parseInt((String)tuple[3]);

            if(!currentPlayer.getHand().getCards().get(Integer.parseInt((String)tuple[3])).getCharacter().isTargeted()){
                playUntargettedCard(chosenCharacter, currentPlayer, cardIndex);
            } else {
                if(tuple[5].equals("")){
                    playTargettedCard(chosenCharacter, currentPlayer, cardIndex, Integer.parseInt((String)tuple[4]), 0);
                } else {
                    playTargettedCard(chosenCharacter, currentPlayer, cardIndex, Integer.parseInt((String) tuple[4]), Integer.parseInt((String) tuple[5]));
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
    }

    private void playTargettedCard(Character chosenCharacter, Player currentPlayer, int cardIndex, int playerTargetIndex, int guardGuess) {

        System.out.println("Targetted card was played");

        if(!possibleTargets()){
            // play targetted card with no action
            System.out.println("FØRSTE GUARD FEJL");
            model.noAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        } else {
            if(validTarget(playerTargetIndex, currentPlayer.getHand().getCards().get(cardIndex).getCharacter())) {
                if(chosenCharacter == Character.GUARD) {
                    System.out.println("It was a guard");
                    guardGuessCharacter = Character.values()[guardGuess];
                    //System.out.println("You guessed " + Character.values()[guardGuess]);
                    model.guardAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndex, guardGuessCharacter);
                } else if(chosenCharacter == Character.PRIEST) {
                    model.priestAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndex);
                } else if(chosenCharacter == Character.BARON) {
                    //System.out.println("Player index " + model.indexOfCurrentPlayersTurn() + " card index " + (cardPick-1) + " player index" + (playerPick-1));
                    model.baronAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndex);
                } else if(chosenCharacter == Character.PRINCE) {
                    model.princeAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndex);
                } else { // i.e. chosenCharacter == Character.KING
                    //currentPlayer.getHand().printHand();
                    System.out.println(currentPlayer.getName() + " gets " + model.players.get(playerTargetIndex).getHand().getCards().get(0).getCharacter());
                    System.out.println(model.players.get(playerTargetIndex).getName() + " gets " + currentPlayer.getHand().getCards().get(cardIndex%2).getCharacter());
                    model.kingAction(model.indexOfCurrentPlayersTurn(), cardIndex, playerTargetIndex);
                }
            } else {
                System.out.println("Seems like an unvalid target");
            }
        }
    }

    private void terminalTest(){
        // 3.1 LMS
        if(model.isLastManStanding()) {

            model.lastMan().incrementAffection();
            String msg = model.lastMan().getName() + " won the round as last man standing!" +
                    model.lastMan().getName() + "'s affection is now " + model.lastMan().getAffection() + newLine;

            model.setRoundWon(true);

            for(Player p : model.players){
                try {
                    // [0] update [1] type [2] tuple recipient [3] winner's name [4] winner's affection [5] Game Log msg
                    lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), model.lastMan().getName(), Integer.toString(model.lastMan().getAffection()), "");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 3.2 EMPTY DECK
        } else if(model.deck.getCards().isEmpty()) {
            if(model.nearestToPrincess().size() == 1){
                model.nearestToPrincess().get(0).incrementAffection();

                String msg = "The deck is empty! " + model.nearestToPrincess().get(0).getName() + " won the round with the highest card!" +
                        model.nearestToPrincess().get(0).getName() + "'s affection is now " + model.nearestToPrincess().get(0).getAffection() + newLine;
                System.out.println(msg);

                model.setRoundWon(true);

                for(Player p : model.players){
                    try {
                        // [0] update [1] type [2] tuple recipient [3] winner's name [4] winner's affection [5] Message for GameLog
                        lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(),
                                model.nearestToPrincess().get(0).getName(), Integer.toString(model.nearestToPrincess().get(0).getAffection()), msg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if(model.nearestToPrincess().size() > 1) {
                if(model.compareHandWinners().size() == 1) {
                    model.compareHandWinners().get(0).incrementAffection();
                    String msg = "The deck is empty! " + model.compareHandWinners().get(0).getName() + " won the round with the highest discard pile! " +
                    model.compareHandWinners().get(0).getName() + "'s affection is now " + model.compareHandWinners().get(0).getAffection() + newLine;
                    System.out.println(msg);
                    model.setRoundWon(true);
                    for(Player p : model.players){
                        try {
                            // [0] update [1] type [2] tuple recipient [3] winner's name [4] winner's affection [5] empty
                            lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(),
                                    model.compareHandWinners().get(0).getName(), Integer.toString(model.compareHandWinners().get(0).getAffection()), msg);
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
                        // [0] update [1] type [2] tuple recipient [3] winner's name [4] winner's affection [5] Game log msg
                        lobbySpace.put(Model.CLIENT_UPDATE, Model.WIN, p.getName(), "", "", msg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    private boolean possibleTargets(){
        for(Player p : model.players){
            if(p.isInRound() && !p.isHandMaidProtected() && !p.isMe(currentPlayer.getName())){
                return true;
            }
        }
        return false;
    }

    private boolean legalCardIndex(int index){
        return (index == 0 || index == 1);
    }
}
