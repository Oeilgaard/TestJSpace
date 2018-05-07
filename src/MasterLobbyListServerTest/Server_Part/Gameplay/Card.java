package MasterLobbyListServerTest.Server_Part.Gameplay;

public class Card {
    //TODO Replace with just Character.java?
    private final Character character;

    public Card(Character character) {
        this.character = character;
    }

    public Character getCharacter() {
        return character;
    }

    public int getValue() {
        return character.getValue();
    }

    public String toString(){ return character.toString(); }
}
