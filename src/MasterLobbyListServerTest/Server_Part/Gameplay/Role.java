package MasterLobbyListServerTest.Server_Part.Gameplay;

public enum Role {

    GUARD (1, true), PRIEST (2, true), BARON (3, true), HANDMAID (4, false),
    PRINCE (5, true), KING (6, true), COUNTESS (7, false), PRINCESS (8, false);

    private final int value;
    private final boolean targeted;

    Role(int value, boolean targeted){
        this.value = value;
        this.targeted = targeted;
    }

    public int getValue(){
        return value;
    }

    public boolean isTargeted(){
        return targeted;
    }

}
