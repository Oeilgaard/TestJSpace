package MasterLobbyListServerTest.JavaFXClient;

import javafx.scene.Parent;
import org.jspace.RemoteSpace;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.UUID;

public class Model {

    protected final static int REQUEST_CODE = 1;
    protected final static int CREATE_LOBBY_REQ = 11;
    protected final static int CREATE_USERNAME_REQ = 12;
    protected final static int PING_REQ = 14;

    protected final static int RESPONSE_CODE = 2;
    protected final static int ASSIGN_UNIQUE_USERNAME_RESP = 23;
    protected final static int PONG_RESP = 24;

    protected final static int LOBBY_REQ = 30;
    protected final static int CONNECT = 31;
    protected final static int LOBBY_DISCONNECT = 32;
    protected final static int BEGIN = 33;
    protected final static int CLOSE = 34;

    protected final static int LOBBY_RESP = 40;
    protected final static int CONNECT_DENIED = 41;
    protected final static int CONNECT_ACCEPTED = 42;

    protected final static int LOBBY_UPDATE = 50;
    protected final static int CHAT_MESSAGE = 51;


    // Query tuples
    protected final static int GAMEPLAY_INFO = 60;
    protected final static int GET_PLAYERLIST = 61;
    protected final static int PLAYERS_IN_ROUND = 62;

    protected final static int GAMEPLAY_ACTION = 70;
    protected final static int PLAY_CARD = 71;

    // 'http style'
    protected final static int OK = 200;
    protected final static int BAD_REQUEST = 400;

    // gameplay tuples
    public final static int CLIENT_UPDATE = 10;
    public final static int NEW_TURN = 11;
    public final static int DISCARD = 12;
    public final static int OUTCOME = 13;
    public final static int KNOCK_OUT = 14;
    public final static int WIN = 15;
    public final static int GAME_START_UPDATE = 16;
    public final static int ACTION_DENIED = 17;
    public final static int GAME_ENDING = 18;
    public final static int GAME_DISCONNECT = 19;

    public final static int SERVER_UPDATE = 20;

    private static RemoteSpace requestSpace, lobbyListSpace, lobbySpace, responseSpace;
    private static String serverIp;
    private int responseFromLobby = 0;
    private String uniqueName;
    private ServerResponseMonitor serverResponseMonitor;

    public Parent currentRoot;

    public ArrayList<String> cardsOnHand = new ArrayList<>();
    public ArrayList<String> actionHistory = new ArrayList<>();

    private PublicKey serverPublicKey;
    private Cipher serverCipher;
    public Key key;
    public Cipher personalCipher;

    public boolean inGame = false;
    public boolean inLobby = false;
    public boolean leaderForCurrentLobby = false;

    public Model(){
        serverResponseMonitor = new ServerResponseMonitor();
    }

    public Thread updateAgent;

    public int indexInLobby = -1;

    public void addIpToRemoteSpaces(String ip) throws IOException {

        requestSpace = new RemoteSpace("tcp://" + ip + ":25565/requestSpace?keep");
        lobbyListSpace = new RemoteSpace("tcp://" + ip + ":25565/lobbyOverviewSpace?keep");
        serverIp = ip;
        responseSpace = new RemoteSpace("tcp://" + ip + ":25565/responseSpace?keep");

    }

    public void setPublicKey(PublicKey pk) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        serverPublicKey = pk;

        serverCipher = Cipher.getInstance("RSA");
        serverCipher.init(Cipher.ENCRYPT_MODE, pk);
    }

    public Cipher getServerCipher(){
        return serverCipher;
    }

    public RemoteSpace getLobbyListSpace(){
        return lobbyListSpace;
    }

    public RemoteSpace getRequestSpace(){
        return requestSpace;
    }

    public RemoteSpace getLobbySpace() {return lobbySpace;}

    public void joinLobby(UUID lobbyid) throws IOException {
        lobbySpace = new RemoteSpace("tcp://" + serverIp + ":25565/" + lobbyid.toString() + "?keep");
    }

    public RemoteSpace getResponseSpace(){
        return responseSpace;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public ServerResponseMonitor getServerResponseMonitor() {
        return serverResponseMonitor;
    }

    public void changeResponseFromLobby(int responseFromLobby){
        this.responseFromLobby = responseFromLobby;
    }

    public int getResponseFromLobby() {
        return responseFromLobby;
    }

    public void resetLobbyInfo(){
        lobbySpace = null;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public boolean getInGame() {
        return inGame;
    }

    public boolean getInLobby() { return inLobby; }

    public void setInLobby(boolean inLobby) { this.inLobby = inLobby; }
}
