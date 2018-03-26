package MasterLobbyListServerTest.Server_Part.Gameplay;

import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class Model {

    public final static int AFFECTION_GOAL_TWO_PLAYERS = 7;
    public final static int AFFECTION_GOAL_THREE_PLAYERS = 5;
    public final static int AFFECTION_GOAL_FOUR_PLAYERS = 5;

    public final static int REVEALED_CARDS_TWO_PLAYER = 3;

    public final static int NO_OF_GUARD = 5;
    public final static int NO_OF_PRIEST = 2;
    public final static int NO_OF_BARON = 2;
    public final static int NO_OF_HANDMAID = 2;
    public final static int NO_OF_PRINCE = 2;
    public final static int NO_OF_KING = 1;
    public final static int NO_OF_COUNTESS = 1;
    public final static int NO_OF_PRINCESS = 1;

    public final static int CLIENT_UPDATE = 10;
    public final static int NEW_TURN = 11;
    public final static int DISCARD = 12;
    public final static int TARGETTED = 121;
    public final static int UNTARGETTED = 122;
    public final static int OUTCOME = 13;
    public final static int KNOCK_OUT = 14;
    public final static int WIN = 15;
    public final static int GAME_START_UPDATE = 16;
    public final static int ACTION_DENIED = 17;


    public final static int SERVER_UPDATE = 20;
    //public final static int DISCARD = 21;

    protected static ArrayList<Player> players;
    protected static int turn;
    protected static int affectionGoal;
    protected static int playerPointer; // index of the current player's turn
    protected static int round;
    protected static Deck deck;
    protected static boolean roundWon;
    protected static ArrayList<Card> revealedCards;
    protected static Card secretCard;
    private static SequentialSpace lobbySpace;

    public Model(ArrayList<String> players, SequentialSpace lobbySpace){

        this.turn = 0;
        this.round = 0;
        this.deck = new Deck();
        this.roundWon = false;
        this.revealedCards = new ArrayList<Card>();
        this.lobbySpace = lobbySpace;

        this.players = new ArrayList<Player>();
        for(String p : players) {
            this.players.add(new Player(p));
        }
        //Model.players.add(new Player("Alice"));
        //Model.players.add(new Player("Bob"));
    }

    public static void determineAffectionGoal(){
        switch (players.size()) {
            case 2:
                affectionGoal = AFFECTION_GOAL_TWO_PLAYERS;
                break;
            case 3:
                affectionGoal = AFFECTION_GOAL_THREE_PLAYERS;
                break;
            case 4:
                affectionGoal = AFFECTION_GOAL_FOUR_PLAYERS;
                break;
            default:
                break;
        }
    }

    public static ArrayList<Player> getPlayers() {
        return players;
    }

    public static int getTurn() {
        return turn;
    }

    public static void nextTurn() {
        turn++;
    }

    public static void nextRound() {
        round++;
    }

    public static Deck getDeck() {
        return deck;
    }

    public int currentMaxAffection(){
        int max = 0;
        for(Player p : players){
            if(max < p.getAffection()) { max = p.getAffection(); }
        }
        return max;
    }

    public int indexOfCurrentPlayersTurn(){
        return playerPointer % players.size();
    }

    public boolean isTargeted(Card card){
        return card.getCharacter().isTargeted();
    }

    public boolean isLastManStanding() {
        int counter = 0;
        for(Player p : players) {
            if(p.isInRound()){ counter++; }
        }
        if(counter == 1) {
            return true;
        } else { return false; }
    }

    public Player lastMan() {
        for(Player p : players) {
            if(p.isInRound()){ return p; }
        }
        return null;
    }

    public ArrayList<Player> nearestToPrincess(){
        ArrayList<Player> winners = new ArrayList<>();
        for(Player p : players){
            if(p.getHand().getCards().get(0).getValue() == highestCard()) {
                winners.add(p);
            }
        }
        return winners;
    }

    public int highestCard(){
        int max = -1;
        for(Player p : players){
            if(p.getHand().getCards().get(0).getValue() > max) {
                max = p.getHand().getCards().get(0).getValue();
            }
        }
        return max;
    }

    public int highestDiscardPile(){
        int max = -1;
        for(Player p : nearestToPrincess()){
            if(p.discardPileSum() > max) {
                max = p.getHand().getCards().get(0).getValue();
            }
        }
        return max;
    }

    public ArrayList<Player> compareHandWinners(){
        ArrayList<Player> winners = new ArrayList<>();
        for(Player p : nearestToPrincess()){
            if(p.discardPileSum() == highestDiscardPile()){
                winners.add(p);
            }
        }
        return winners;
    }

    public void knockOut(Player knockedOut) {
        System.out.println("Player " + knockedOut.getName() + " is out of the round" + Game.newLine);
        knockedOut.discardHand();
        knockedOut.setInRound(false);
        for(Player p : players) {
            // [0]: update, [1]: type, [2]: recipient name, [3]: knocked out player's name
            try {
                lobbySpace.put(CLIENT_UPDATE, KNOCK_OUT, p.getName(), knockedOut.getName(), "");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    public void drawSecretCard(Hand hand) {
        hand.getCards().add(secretCard);
        secretCard = null;
    }

    // CARD ACTIONS

    public void noAction(int sender, int index){
        players.get(sender).discardCard(index);
    }

    public void informPlayers(String card, String msgSender, String msgTarget, String msgOthers, int senderIndex, int receiverIndex, String kingCardToSender, String kingCardToTarget){
        for(Player p : players){
            if(p.getName() == players.get(senderIndex).getName()){
                try {
                    lobbySpace.put(CLIENT_UPDATE, OUTCOME, p.getName(), card, msgSender, kingCardToSender);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if(p.getName() == players.get(receiverIndex).getName()){
                try {
                    lobbySpace.put(CLIENT_UPDATE, OUTCOME, p.getName(), card, msgTarget, kingCardToTarget);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    lobbySpace.put(CLIENT_UPDATE, OUTCOME, p.getName(), card, msgOthers, "");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void informPlayersUntargetted(String card, int senderIndex, String msgOthers){
        for(Player p : players){
            if(p.getName() != players.get(senderIndex).getName()){
                try {
                    lobbySpace.put(CLIENT_UPDATE, OUTCOME, p.getName(), card, msgOthers, "");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void guardAction(int sender, int index, int targetPlayer, Character guess){

        String senderName = removeIDFromPlayername(players.get(sender).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayer).getName());

        players.get(sender).discardCard(index);

        if(players.get(targetPlayer).getHand().getCards().get(0).getCharacter() == guess) {
            knockOut(players.get(targetPlayer));

            String msgSender = "You guessed correct! " + targetName + " is out of the round.";
            String msgTarget = senderName + " correctly guessed that you have a " + guess.toString() + "! + " +
                    "You are out of the round.";
            String msgOthers = senderName + " correctly guessed that " +
                    targetName + " has a " + guess.toString() + "! + " +
                    targetName + " is out of the round.";

            informPlayers(Character.GUARD.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");

//         players.get(sender).getDiscardPile().addToDiscardPile(players.get(sender).getHand().getCards().get(index));
//         players.get(sender).getHand().getCards().remove(index);
        } else {
            String msgSender = "You guessed incorrect!";
            String msgTarget = senderName + " uses GUARD on you and incorrectly guesses that you have a " + guess.toString();
            String msgOthers = senderName + " uses GUARD on + " + targetName
                    + " and incorrectly guesses " + guess.toString();
            informPlayers(Character.GUARD.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");
        }
    }

    public void priestAction(int sender, int index, int targetPlayer) {

        String senderName = removeIDFromPlayername(players.get(sender).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayer).getName());

        players.get(sender).discardCard(index);
        //System.out.print("Priest finds " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter() + Game.newLine);
        //players.get(targetPlayer).getHand().printHand();
        //TODO: custom messages
        String msgSender = targetName + " has a " + players.get(targetPlayer).getHand().getCards().get(0);
        String msgTarget = senderName + " uses Priest";
        String msgOthers = "moo";
        informPlayers(Character.PRIEST.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");
    }

    // PLAYER INDEX  CARD INDEX  PLAYER INDEX
    public void baronAction(int sender, int index, int targetPlayer) {

        String senderName = removeIDFromPlayername(players.get(sender).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayer).getName());

        players.get(sender).discardCard(index);

        System.out.println(senderName + " has a " + players.get(sender).getHand().getCards().get(0).getCharacter() +
        " of value " + players.get(sender).getHand().getCards().get(0).getCharacter().getValue());
        System.out.println(targetName + " has a " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter() +
                " of value " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter().getValue() + Game.newLine);

        Character senderPlayerCharater = players.get(sender).getHand().getCards().get(0).getCharacter();
        Character targetPlayerCharater = players.get(targetPlayer).getHand().getCards().get(0).getCharacter();

        if(players.get(sender).getHand().getCards().get(0).getCharacter().getValue() > players.get(targetPlayer).getHand().getCards().get(0).getCharacter().getValue()){
            knockOut(players.get(targetPlayer));

            String msgSender = "Target player has a " + targetPlayerCharater.toString() + ". You win the duel!";
            String msgTarget = senderName + " has a " + senderPlayerCharater.toString() + ". You are out of the round!";
            String msgOthers = senderName + " wins the duel, and " + targetName + " is out of the round";
            informPlayers(Character.BARON.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");
        } else if(players.get(sender).getHand().getCards().get(0).getCharacter().getValue() < players.get(targetPlayer).getHand().getCards().get(0).getCharacter().getValue()){
            knockOut(players.get(sender));

            String msgSender = "Target player has a " + targetPlayerCharater.toString() + ". You are out of the round!";
            String msgTarget = senderName + " has a " + senderPlayerCharater.toString() + ". You win the duel!";
            String msgOthers = targetName + " wins the duel, and " + senderName + " is out of the round";
            informPlayers(Character.BARON.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");
        } else {
            System.out.println("Tie! No one is knocked out...");

            String msgSender = "Target player has a " + targetPlayerCharater.toString() + ". It's a draw!";
            String msgTarget = senderName + " has a " + senderPlayerCharater.toString() + ". It's a draw!";
            String msgOthers = "It's a draw! No one is knocked out!";
            informPlayers(Character.BARON.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");}
    }

    public void handmaidAction(int sender, int index){
        players.get(sender).discardCard(index);
        players.get(sender).activateHandmaid();

        String msgOthers = "moo";
        informPlayersUntargetted(Character.HANDMAID.toString(), sender, msgOthers);

    }

    public void princeAction(int sender, int index, int targetPlayer) {

        String senderName = removeIDFromPlayername(players.get(sender).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayer).getName());


        players.get(sender).discardCard(index);
        players.get(targetPlayer).discardHand();

        if ((!deck.getCards().isEmpty())) {
            deck.drawCard(players.get(targetPlayer).getHand());

            String msgSender = "You play PRINCE on " + players.get(targetPlayer) + " who discard the hand and draws a new card.";
            String msgTarget = senderName + " plays PRINCE on you. You discard" +
                    " your hand and draw a " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter().toString();
            String msgOthers = senderName + " played PRINCE on " + targetName + " who draw a new card.";
            informPlayers(Character.PRINCE.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");
        } else {
            drawSecretCard(players.get(targetPlayer).getHand());

            String msgSender = "You play PRINCE on " + players.get(targetPlayer) + " who draws the secret card.";
            String msgTarget = senderName + " plays PRINCE on you. You discard" +
                    " your hand and draw the secret card " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter().toString();
            String msgOthers = senderName + " played PRINCE on " + targetName + " who draw the secret card.";
            informPlayers(Character.PRINCE.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, "", "");
        }
    }

    public void kingAction(int sender, int index, int targetPlayer) {

        String senderName = removeIDFromPlayername(players.get(sender).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayer).getName());

        players.get(sender).discardCard(index);

        Card senderCard = players.get(sender).getHand().getCards().get(0);
        Card targetCard = players.get(targetPlayer).getHand().getCards().get(0);

        players.get(sender).discardHand();
        players.get(targetPlayer).discardHand();
        players.get(sender).getHand().getCards().add(targetCard);
        players.get(targetPlayer).getHand().getCards().add(senderCard);

        //TODO: custom messages
        String msgSender = "You played KING on " + targetName + " and got a " + targetCard.getCharacter().toString();
        String msgTarget = senderName + " played KING on you. You now have a " + senderCard.getCharacter().toString();
        String msgOthers = targetName + " played KING on " + senderName;

        informPlayers(Character.KING.toString(), msgSender, msgTarget, msgOthers, sender, targetPlayer, targetCard.getCharacter().toString(), senderCard.getCharacter().toString());

    }

    public void countessAction(int sender, int index){

        String senderName = removeIDFromPlayername(players.get(sender).getName());

        players.get(sender).discardCard(index);

        String msgOthers = senderName + " played COUNTESS.";
        informPlayersUntargetted(Character.COUNTESS.toString(),sender,msgOthers);
    }

    public void princessAction(int sender, int index) {

        String senderName = removeIDFromPlayername(players.get(sender).getName());

        players.get(sender).discardCard(index);
        players.get(sender).setInRound(false);
        players.get(sender).discardHand();

        knockOut(players.get(sender));

        String msgOthers = senderName + " played PRINCESS and is out of the round.";
        informPlayersUntargetted(Character.PRINCESS.toString(),sender,msgOthers);

    }

    public void setRoundWon(boolean roundWon){
        this.roundWon = roundWon;
    }

    public boolean countessRule(Player currentPlayer){
        return (currentPlayer.getHand().getCards().contains(new Card(Character.COUNTESS)) && currentPlayer.getHand().getCards().contains(new Card(Character.PRINCE))) ||
                (currentPlayer.getHand().getCards().contains(new Card(Character.COUNTESS)) && currentPlayer.getHand().getCards().contains(new Card(Character.KING)));
    }

    public String removeIDFromPlayername(String username){
        String newName = username.substring(0, username.indexOf("#"));
        return newName;
    }

}
