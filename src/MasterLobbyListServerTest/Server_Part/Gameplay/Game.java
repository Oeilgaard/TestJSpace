package MasterLobbyListServerTest.Server_Part.Gameplay;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import java.util.ArrayList;

public class Game {

    public final static String newLine = System.getProperty("line.separator");

    private Model model;
    private SequentialSpace lobbySpace;

    private Character guardGuessCharacter;
    private Character chosenCharacter;
    private Player currentPlayer;
    private Object[] tuple;

    public Game(ArrayList<String> players, SequentialSpace lobbySpace) {
        this.model = new Model(players, lobbySpace);
        this.lobbySpace = lobbySpace;
    }

    public void newRound(){

        System.out.println("Shuffling cards...");

        for(Player p : model.players) {
            p.getHand().getCards().clear();
            p.getDiscardPile().getCards().clear();
            p.setInRound(true);
            p.deactivateHandmaid();
        }

        model.turn = 0;
        model.playerPointer = 0;
        model.round++;

        model.deck.getCards().clear();
        model.deck.fillDeck();
        model.deck.shuffle();
        model.setRoundWon(false);

        System.out.println("The revealed cards are:");
        for(int i = 0; i < model.REVEALED_CARDS_TWO_PLAYER; i++) {
            model.deck.drawCard(model.revealedCards);
            System.out.println(model.revealedCards.get(i).getCharacter());
        }

        // Secret card
        System.out.println("and a secret card is set aside..." + newLine);
        model.secretCard = model.deck.drawCard();
        //System.out.println(model.secretCard.getCharacter() + newLine);

        System.out.println("Each player draws a card..." + newLine);
        //Both players draw a card
        for(Player p : model.players) {
            model.deck.drawCard(p.getHand());
            System.out.println(p.getName() + " start with a " + p.getHand().getCards().get(0).getCharacter());
            try {
                lobbySpace.put(Model.CLIENT_UPDATE, Model.GAME_START_UPDATE, p.getName(),
                        p.getHand().getCards().get(0).getCharacter().toString(), "", "");
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
                Card one = currentPlayer.getHand().getCards().get(0);
                Card two = currentPlayer.getHand().getCards().get(1);

                if(currentPlayer.isInRound()) {

                    if(currentPlayer.isHandMaidProtected()) {
                        currentPlayer.deactivateHandmaid();
                    }

                    // States current player's turn
                    System.out.println("Round no. " + model.round + newLine + "Turn no. "
                            + (model.turn+1) + newLine + currentPlayer.getName() + "'s turn" + newLine);

                    // 1. DRAW

                    model.deck.drawCard(currentPlayer.getHand());
                    System.out.println(currentPlayer.getName() + " drew a " + two.getCharacter() + newLine);

                    System.out.println(currentPlayer.getName() + "'s current hand: ");
                    currentPlayer.getHand().printHand();
                    System.out.print(newLine);

                    // TODO: TURN TUPLE TO CURRENT PLAYER + INFO TO REST

                    for(Player p : model.players){
                        if(p.getName()==currentPlayer.getName()) {
                            try {
                                // [0] Update, [1] update type, [2] receiver, [3] drawn card
                                lobbySpace.put(Model.CLIENT_UPDATE, Model.NEW_TURN, p.getName(), two.toString(), "");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                // [0] Update, [1] update type, [2] receiver, [3] ...
                                lobbySpace.put(Model.CLIENT_UPDATE, Model.NEW_TURN, p.getName(), "", "");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // 2. DISCARD

                    // TODO: 'GET' RESPOND FROM SPECIFIC PLAYER
                    // TODO: DO SANITY CHECK OF PLAY
                    //cardPick = scanner.nextInt();

                    try {
                        // [0] Update, [1] update type, [2] sender, [3] card pick index, [4] target (situational) , [5] guess (situational)
                        //TODO: Lyt efter disconnect også
                        Object[] tuple = lobbySpace.get(new ActualField(Model.SERVER_UPDATE), new ActualField(Model.DISCARD),
                                new FormalField(String.class), new FormalField(String.class),
                                new FormalField(String.class), new FormalField(String.class));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

//                    if(validTarget((int) tuple[4], (String) tuple[3])){
//                        chosenCharacter = currentPlayer.getHand().getCards().get((int) tuple[3]).getCharacter();
//                    } else {
//                        // Send tuple der requester nyt kort...
//                    }

                    // 2. PLAY CARD

                    playCard(currentPlayer);

                    // TODO: INFO TO ALL

                    // 3. ROUND END CHECKS
                    terminalTest();
                    // TODO: IF GAME OVER, INFO TO ALL - SPECIAL TUPLE TO WINNER

                    model.turn++; // turn only increments if a turn is executed
                }
                model.playerPointer++; // player pointer increments for every index in the players array
            }
            System.out.println("Game is over");
        }
    }

    private boolean validTarget(int targetPlayerIndex, Character character){
        Player target = model.players.get(targetPlayerIndex);
        if(character == Character.PRINCE){
            return !target.isHandMaidProtected() && target.isInRound();
        } else {
            return !target.isHandMaidProtected() && target.isInRound() && !target.isMe(currentPlayer.getName());
        }
    }

    private void playCard(Player currentPlayer){
        if(!legalCardIndex((int) tuple[3])){
            // send error tuple eller vælg random...
        }
        // if Countess-rule is occurring, we force the play
        if(model.countessRule(currentPlayer)){
            if(currentPlayer.getHand().getCards().get(0).getCharacter() == Character.COUNTESS){
                playUntargettedCard(currentPlayer.getHand().getCards().get(1).getCharacter(),currentPlayer,1);
            } else {
                playUntargettedCard(currentPlayer.getHand().getCards().get(0).getCharacter(),currentPlayer,0);
            }
        } else {
            chosenCharacter = currentPlayer.getHand().getCards().get((int) tuple[3]).getCharacter();
            int cardIndex = (int) tuple[3];

            if(!currentPlayer.getHand().getCards().get((int) tuple[3]).getCharacter().isTargeted()){
                playUntargettedCard(chosenCharacter, currentPlayer, cardIndex);
            } else {
                playTargettedCard(chosenCharacter, currentPlayer, cardIndex, (int) tuple[4], (int) tuple[5]);
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

        if(!possibleTargets()){
            // play targetted card with no action
            model.noAction(model.indexOfCurrentPlayersTurn(), cardIndex);
        } else {
            if(validTarget(playerTargetIndex, currentPlayer.getHand().getCards().get(cardIndex).getCharacter())) {
                if(chosenCharacter == Character.GUARD) {
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

            }
        }
    }

    private void terminalTest(){
        // 3.1 LMS
        if(model.isLastManStanding()) {

            for(Player p : model.players) {
                System.out.println(p.getName() + p.isInRound());
            }

            System.out.println(model.lastMan().getName() + " won the round as last man standing!");
            model.lastMan().incrementAffection();
            System.out.println(model.lastMan().getName() + "'s affection is now " + model.lastMan().getAffection() + newLine);
            model.setRoundWon(true);
            //model.players.
            // 3.2 EMPTY DECK
        } else if(model.deck.getCards().isEmpty()) {
            if(model.nearestToPrincess().size() == 1){
                System.out.println("The deck is empty! " + model.nearestToPrincess().get(0).getName() + " won the round with the highest card!");
                model.nearestToPrincess().get(0).incrementAffection();
                System.out.println(model.nearestToPrincess().get(0).getName() + "'s affection is now " + model.nearestToPrincess().get(0).getAffection() + newLine);
                model.setRoundWon(true);
            } else if(model.deck.getCards().size() > 0) {
                if(model.compareHandWinners().size() == 1) {
                    System.out.println("The deck is empty! " + model.nearestToPrincess().get(0).getName() + " won the round with the highest discard pile!");
                    model.compareHandWinners().get(0).incrementAffection();
                    System.out.println(model.nearestToPrincess().get(0).getName() + "'s affection is now " + model.nearestToPrincess().get(0).getAffection() + newLine);
                    model.setRoundWon(true);
                }
            } else {
                model.setRoundWon(true); // corresponds to draw...
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
