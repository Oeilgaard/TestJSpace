package mainSystemFiles.Server_Part.Gameplay;

import javax.crypto.Cipher;

public class Player {

    private Hand hand;
    private int affection;
    private String name;
    private DiscardPile discardPile;
    private boolean handmaidProtection;
    private boolean inRound;
    private int playerIndex;
    private Cipher playerCipher;

    public Player(String name, int playerIndex, Cipher playerCipher) {
        this.affection = 0;
        this.name = name;
        this.hand = new Hand();
        this.discardPile = new DiscardPile();
        this.handmaidProtection = false;
        this.inRound = true;
        this.playerIndex = playerIndex;
        this.playerCipher = playerCipher;
    }

    public String getName() {
        return name;
    }

    /*public void setName(String name) {
        this.name = name;
    }*/

    public Hand getHand() {
        return hand;
    }

    public int getAffection() {
        return affection;
    }

    public void incrementAffection() {
        this.affection++;
    }

    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public void setInRound(boolean inRound) {
        this.inRound = inRound;
    }

    public boolean isInRound() {
        return inRound;
    }

    public boolean isHandMaidProtected() {
        return handmaidProtection;
    }

    public boolean isMe(String name) {
        return this.name.equals(name);
    }

    public void discardCard(int index) {
        System.out.println(name + " discards " + hand.getCards().get(index).getRole() + Game.newLine);
        discardPile.addToDiscardPile(hand.getCards().get(index));
        discardPile.addToDiscardPile(hand.getCards().get(index));
        hand.getCards().remove(index);
    }

    public void discardHand() {
        //System.out.println(name + " discards hand: ");
        discardPile.addToDiscardPile(hand.getCards().get(0));
        hand.getCards().remove(0);
    }

    public void activateHandmaid(){
        this.handmaidProtection = true;
    }

    public void deactivateHandmaid(){
        this.handmaidProtection = false;
    }

    public int discardPileSum(){
        int sum = 0;
        for(Card c : discardPile.getCards()){
            sum += c.getValue();
        }
        return sum;
    }

    public int getPlayerIndex(){
        return playerIndex;
    }

    public Cipher getPlayerCipher(){
        return playerCipher;
    }


//    public void discardHand() {
//        System.out.println(name + " discards hand: " + Game.newLine);
//
//        for(Card c : hand.getCards()){
//            System.out.println(c.getRole() + Game.newLine);
//            discardPile.getCards().add(c);
//            hand.getCards().remove(0);
//
//        }
//    }

}
