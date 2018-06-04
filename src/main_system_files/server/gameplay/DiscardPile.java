package main_system_files.server.gameplay;

import java.util.ArrayList;

public class DiscardPile {

    private ArrayList<Card> cards;

    public DiscardPile(){
        this.cards = new ArrayList<Card>();
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    public void addToDiscardPile(Card card){
        cards.add(card);
    }

}
