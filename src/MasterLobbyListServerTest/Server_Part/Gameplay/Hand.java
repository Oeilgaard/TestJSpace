package MasterLobbyListServerTest.Server_Part.Gameplay;

import java.util.ArrayList;

public class Hand {

    private ArrayList<Card> cards;

    public Hand(){
        cards = new ArrayList<Card>();
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void printHand() {
        int i;
        for (i = 0; i < cards.size(); i++){
            System.out.println("Card number " + (i+1) + " is a " + cards.get(i).getCharacter());
        }
        if (i == 0) { System.out.println("Hand is empty"); }
    }

    public void setCards(int cardNr, Character chr){
        cards.remove(cardNr);
        if(cardNr == 1){
            cards.add(new Card(chr));
        } else {
            if (!cards.isEmpty()) {
                Card card = cards.get(0);
                cards.remove(0);
                cards.add(new Card(chr));
                cards.add(card);
            } else {
                cards.add(new Card(chr));
            }
        }
    }
}
