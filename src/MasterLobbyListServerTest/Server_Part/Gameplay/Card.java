package MasterLobbyListServerTest.Server_Part.Gameplay;

public class Card {
    //TODO Replace with just Role.java?
    private final Role role;

    public Card(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public int getValue() {
        return role.getValue();
    }

    public String toString(){ return role.toString(); }
}
