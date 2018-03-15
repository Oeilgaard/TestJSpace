package MasterLobbyListServerTest.Server_Part.Gameplay;

import java.util.ArrayList;

public class Model {

    public final static int AFFECTION_GOAL_TWO_PLAYERS = 7;
    public final static int AFFECTION_GOAL_THREE_PLAYERS = 5;
    public final static int AFFECTION_GOAL_FOUR_PLAYERS = 5;

    public final static int REVEALED_CARDS_TWO_PLAYER = 3;

    protected static ArrayList<Player> players;
    protected static int turn;
    protected static int affectionGoal;
    protected static int playerPointer; // index of the current player's turn
    protected static int round;
    protected static Deck deck;
    protected static boolean roundWon;
    protected static ArrayList<Card> revealedCards;
    protected static Card secretCard;

    public Model(ArrayList<String> players){

        this.turn = 0;
        this.round = 0;
        this.deck = new Deck();
        this.roundWon = false;
        this.revealedCards = new ArrayList<Card>();

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

    public void knockOut(Player p) {
        System.out.println("Player " + p.getName() + " is out of the round" + Game.newLine);
        p.discardHand();
        p.setInRound(false);
    }

    public void drawSecretCard(Hand hand) {
        hand.getCards().add(secretCard);
        secretCard = null;
    }

    // CARD ACTIONS

    public void noAction(int sender, int index){
        players.get(sender).discardCard(index);
    }

    public void guardAction(int sender, int index, int targetPlayer, Character guess){

        players.get(sender).discardCard(index);

        if(players.get(targetPlayer).getHand().getCards().get(0).getCharacter() == guess) {
            System.out.println("Correct!");
            knockOut(players.get(targetPlayer));
//         players.get(sender).getDiscardPile().addToDiscardPile(players.get(sender).getHand().getCards().get(index));
//         players.get(sender).getHand().getCards().remove(index);
        } else {
            System.out.println("Incorrect!");
        }
    }

    public void priestAction(int sender, int index, int targetPlayer) {
        players.get(sender).discardCard(index);
        System.out.print("Priest finds " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter() + Game.newLine);
        //players.get(targetPlayer).getHand().printHand();
    }
    //                    PLAYER INDEX  CARD INDEX  PLAYER INDEX
    public void baronAction(int sender, int index, int targetPlayer) {

        players.get(sender).discardCard(index);

        System.out.println(players.get(sender).getName() + " has a " + players.get(sender).getHand().getCards().get(0).getCharacter() +
        " of value " + players.get(sender).getHand().getCards().get(0).getCharacter().getValue());
        System.out.println(players.get(targetPlayer).getName() + " has a " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter() +
                " of value " + players.get(targetPlayer).getHand().getCards().get(0).getCharacter().getValue() + Game.newLine);

        if(players.get(sender).getHand().getCards().get(0).getCharacter().getValue() > players.get(targetPlayer).getHand().getCards().get(0).getCharacter().getValue()){
            knockOut(players.get(targetPlayer));
        } else if(players.get(sender).getHand().getCards().get(0).getCharacter().getValue() < players.get(targetPlayer).getHand().getCards().get(0).getCharacter().getValue()){
            knockOut(players.get(sender));
        } else { System.out.println("Tie! No one is knocked out..."); }

    }

    public void handmaidAction(int sender, int index){
        players.get(sender).discardCard(index);
        players.get(sender).activateHandmaid();
    }

    public void princeAction(int sender, int index, int targetPlayer) {
        players.get(sender).discardCard(index);
        players.get(targetPlayer).discardHand();

        if ((!deck.getCards().isEmpty())) {
            deck.drawCard(players.get(targetPlayer).getHand());
        } else {
            drawSecretCard(players.get(targetPlayer).getHand());
        }
    }

    public void kingAction(int sender, int index, int targetPlayer) {

        players.get(sender).discardCard(index);

        Card senderCard = players.get(sender).getHand().getCards().get(0);
        Card targetCard = players.get(targetPlayer).getHand().getCards().get(0);

        players.get(sender).discardHand();
        players.get(targetPlayer).discardHand();
        players.get(sender).getHand().getCards().add(targetCard);
        players.get(targetPlayer).getHand().getCards().add(senderCard);
    }

    public void countessAction(int sender, int index){
        players.get(sender).discardCard(index);
    }

    public void princessAction(int sender, int index) {
        players.get(sender).discardCard(index);
        players.get(sender).setInRound(false);
        players.get(sender).discardHand();
    }

    public void setRoundWon(boolean roundWon){
        this.roundWon = roundWon;
    }
}
