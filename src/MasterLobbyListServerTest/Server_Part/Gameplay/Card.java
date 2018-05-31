package MasterLobbyListServerTest.Server_Part.Gameplay;

public class Card {

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
