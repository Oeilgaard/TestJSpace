package MasterLobbyListServerTest.Server_Part.Gameplay;

public class Card {

    private final Character character;

    Card(Character character) {
        this.character = character;
    }

    public Character getCharacter() {
        return character;
    }

    public int getValue() {
        return character.getValue();
    }

    public String toString(){
        return character.toString(); //TODO: is it acutally e.g. "BARON"?
    }
}
