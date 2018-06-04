package main_system_files.server.gameplay;

import java.util.ArrayList;
import java.util.Collections;

public class Deck {

    private ArrayList<Card> cards = new ArrayList<>();

    public Deck() {}

    public ArrayList<Card> getCards(){
        return cards;
    }

    // "Draw Card"-methods take a
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
        for(Role r : Role.values()) {
            if(r == Role.GUARD){
                for(int i = 0; i < Model.NO_OF_GUARD; i++) {
                    cards.add(new Card(r));
                }
            } else if (r == Role.PRIEST || r == Role.BARON || r == Role.PRINCE || r == Role.HANDMAID) {
                for (int i = 0; i < Model.NO_OF_PRIEST; i++) { // Model.NO_OF_PRIEST
                    cards.add(new Card(r));
                }
            } else {
                cards.add(new Card(r));
            }
        }
    }

    /*
    // SPECIAL DECK TO TEST COUNTESS-RULE
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
    }
    */

    public void shuffle(){
        Collections.shuffle(this.cards);
    }

    /*public void printDeck() {
        int i;
        for (i = 0; i < cards.size(); i++){
            System.out.println("Card number " + (i+1) + " is a " + cards.get(i).getRole());
        }
        if (i == 0) { System.out.println("Deck is empty"); }
    }*/

}
