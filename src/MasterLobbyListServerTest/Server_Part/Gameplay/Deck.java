package MasterLobbyListServerTest.Server_Part.Gameplay;

import javax.lang.model.element.ModuleElement;
import java.util.ArrayList;
import java.util.Collections;

public class Deck {
    public static final int COPIES_OF_PRINCESS= 1;
    public static final int COPIES_OF_COUNTESS = 1;
    public static final int COPIES_OF_KING = 1;
    public static final int COPIES_OF_PRINCE = 2;
    public static final int COPIES_OF_HANDMAID = 2;
    public static final int COPIES_OF_BARON = 2;
    public static final int COPIES_OF_PRIEST = 2;
    public static final int COPIES_OF_GUARD = 5;

    //private static final int[] copies = {COPIES_OF_GUARD, COPIES_OF_PRIEST, COPIES_OF_BARON,
    //        COPIES_OF_BARON, COPIES_OF_HANDMAID, COPIES_OF_PRINCE, COPIES_OF_KING, COPIES_OF_COUNTESS, COPIES_OF_PRINCESS};

    private ArrayList<Card> cards = new ArrayList<>();

    public Deck() {
    }

    public ArrayList<Card> getCards(){
        return cards;
    }

    public void drawCard(Hand hand) {
        if(cards.size() > 0) {
            Card drawnCard = cards.get(0);
            hand.getCards().add(drawnCard);
            cards.remove(0);
        } else { System.out.println("Cannot draw, deck is empty"); }
    }

    public void drawCard(ArrayList<Card> deck) {
        if(cards.size() > 0) {
            Card drawnCard = cards.get(0);
            deck.add(drawnCard);
            cards.remove(0);
        } else { System.out.println("Cannot draw, deck is empty"); }

    }

    public Card drawCard() {
        Card drawnCard = null;
        if(cards.size() > 0) {
            drawnCard = cards.get(0);
            cards.remove(0);
            return drawnCard;
        } else {
            System.out.println("Cannot draw, deck is empty");
            return drawnCard;
        }
    }

    public void fillDeck(){
        for(Character ch : Character.values()) {
            if(ch == Character.GUARD){
                for(int i = 0; i < Model.NO_OF_GUARD; i++) {
                    cards.add(new Card(ch));
                }
            } else if (ch == Character.PRIEST || ch == Character.BARON || ch == Character.PRINCE || ch == Character.HANDMAID) {
                for (int i = 0; i < Model.NO_OF_PRIEST; i++) { // Model.NO_OF_PRIEST
                    cards.add(new Card(ch));
                }
            } else {
                cards.add(new Card(ch));
            }
        }

    }

/*
    public void fillDeck(){
        for(Character ch : Character.values()) {
            if(ch == Character.COUNTESS){
                for(int i = 0; i < 4; i++) {
                    cards.add(new Card(ch));
                }
            } else if(ch == Character.PRINCE){
                for(int i = 0; i < 4; i++) {
                    cards.add(new Card(ch));
                }
            } else if(ch == Character.KING){
                for(int i = 0; i < 4; i++) {
                    cards.add(new Card(ch));
                }
            }
        }


    }*/

    public void shuffle(){
        Collections.shuffle(this.cards);
    }

    public void printDeck() {
        int i;
        for (i = 0; i < cards.size(); i++){
            System.out.println("Card number " + (i+1) + " is a " + cards.get(i).getCharacter());
        }
        if (i == 0) { System.out.println("Deck is empty"); }
    }

}
