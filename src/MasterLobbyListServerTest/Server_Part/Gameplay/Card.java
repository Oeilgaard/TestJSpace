package MasterLobbyListServerTest.Server_Part.Gameplay;

public class Card {

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
}
