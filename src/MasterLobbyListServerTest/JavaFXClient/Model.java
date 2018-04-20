package MasterLobbyListServerTest.JavaFXClient;

import javafx.scene.Parent;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javax.crypto.*;
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
    protected final static int NO_RESPONSE = 444;

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
    private int responseFromLobby = NO_RESPONSE;
    private String uniqueName;
    private ServerResponseMonitor serverResponseMonitor;
    private static int currentThreadNumber = 0;

    public Parent currentRoot;

    public ArrayList<String> cardsOnHand = new ArrayList<>();
    public ArrayList<String> actionHistory = new ArrayList<>();

    private PublicKey serverPublicKey;
    private Cipher serverCipher;

    private Cipher lobbyCipher;

    public Key key;
    public Cipher personalCipher;

    public boolean inGame = false;
    public boolean inLobby = false;
    public boolean leaderForCurrentLobby = false;

    private static String currentLobbyName;

    public Model() {
        serverResponseMonitor = new ServerResponseMonitor();
    }

    public Thread updateAgent;

    public int indexInLobby = -1;

    public static boolean currentSceneIsGameScene = false;

    public void setCurrentThreadNumber(int currentThreadNumber) {
        Model.currentThreadNumber = currentThreadNumber;
    }

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

    public Cipher getServerCipher() {
        return serverCipher;
    }

    public Cipher getLobbyCipher() { return lobbyCipher; }


    public int getCurrentThreadNumber(){
        return currentThreadNumber;
    }

    public void incrementCurrentThreadNumber(){
        currentThreadNumber++;
    }

    public void decrementCurrentThreadNumber(){
        currentThreadNumber--;
    }

    public RemoteSpace getLobbyListSpace() {
        return lobbyListSpace;
    }

    public RemoteSpace getRequestSpace() {
        return requestSpace;
    }

    public RemoteSpace getLobbySpace() {
        return lobbySpace;
    }

    public void joinLobby(UUID lobbyid) throws IOException, InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        lobbySpace = new RemoteSpace("tcp://" + serverIp + ":25565/" + lobbyid.toString() + "?keep");
        Object[] tuple = lobbySpace.query(new FormalField(PublicKey.class));
        lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE, (PublicKey) tuple[0]);
    }

    public RemoteSpace getResponseSpace() {
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

    public void changeResponseFromLobby(int responseFromLobby) {
        this.responseFromLobby = responseFromLobby;
    }

    public int getResponseFromLobby() {
        return responseFromLobby;
    }

    public void resetLobbyInfo() {
        lobbySpace = null;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public boolean getInGame() {
        return inGame;
    }

    public boolean getInLobby() {
        return inLobby;
    }

    public void setInLobby(boolean inLobby) {
        this.inLobby = inLobby;
    }

    public String getUserName(){ return uniqueName.substring(0, uniqueName.indexOf("#")); }

    public String getCurrentLobbyName(){
        return currentLobbyName;
    }

    public void joinServerLogic(String urlForRemoteSpace) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, IOException, InterruptedException {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        Key key = kg.generateKey();
        this.key = key;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        this.personalCipher = cipher;
        this.addIpToRemoteSpaces(urlForRemoteSpace);

        // (Blocking) query of the server's public key and set it in client's model
        Object[] tuple = requestSpace.query(new FormalField(PublicKey.class));
        this.setPublicKey((PublicKey) tuple[0]);
    }

    // Given an arbitrary string, returns true if the user-id is correctly created and picked up by the client
    // Returns false if the request was denied by the server (illegal name)
    // Returns false if the user name is recognized as invalid client-side
    public boolean createUserLogic(String userName) throws IOException, IllegalBlockSizeException, InterruptedException {

        SealedObject encryptedUserNameString = new SealedObject(userName + "!", serverCipher);

        if (HelperFunctions.validName(userName)) {

            //createUserNameButton.setDisable(true);
            //instructionsUserName.setText("");

            SealedObject encryptedKey = new SealedObject(key, serverCipher);

            requestSpace.put(Model.REQUEST_CODE, Model.CREATE_USERNAME_REQ, encryptedUserNameString, encryptedKey);

            Object[] tuple;
            int field1;
            String field3;

            while (true) {
                // Blocks until user receives unique username (due to 'get')
                // [0] response code [1] Response [2] Ok or error [3] Username of receiver [4] Username with ID
                try {
                    tuple = responseSpace.query(new ActualField(Model.RESPONSE_CODE), new ActualField(Model.ASSIGN_UNIQUE_USERNAME_RESP),
                            new FormalField(SealedObject.class));

                    if (tuple != null) {

                        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(personalCipher);

                        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
                        field1 = Integer.parseInt(field1text);
                        field3 = decryptedMessage.substring(decryptedMessage.indexOf('?') + 1, decryptedMessage.length());

                        responseSpace.get(new ActualField(Model.RESPONSE_CODE), new ActualField(Model.ASSIGN_UNIQUE_USERNAME_RESP),
                                new FormalField(SealedObject.class));

                        break;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    //e.printStackTrace();
                }
            }

            if ((int) field1 == Model.OK) {
                setUniqueName((String) field3); // Setting the user's name
                return true;
            } else if ((int) field1 == Model.BAD_REQUEST) {
                return false;
            }
        }
        return false;
    }

    public boolean createLobbyLogic(String lobbyNameString) throws IOException, IllegalBlockSizeException, InterruptedException {

        System.out.println("The user's id: " + uniqueName);

        SealedObject encryptedLobbyNameString = new SealedObject(lobbyNameString + "!" + uniqueName, serverCipher);

        if (HelperFunctions.validName(lobbyNameString)) {
            //createLobbyButton.setDisable(true);
            //instructionsLobbyName.setText("");

            System.out.println("Yes the lobby name is valid!");

            SealedObject encryptedKey = new SealedObject(key, serverCipher);

            requestSpace.put(Model.REQUEST_CODE, Model.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKey);

            // Wait for server to be created
            int field1;
            while (true) {
                try {
                    // [0] response code [1] Ok or deny [2] username of receiver [3] ID for lobby
                    Object[] tuple = responseSpace.query(new ActualField(Model.RESPONSE_CODE), new FormalField(SealedObject.class));
                    if (tuple != null) {

                        String decryptedMessage = (String) ((SealedObject) tuple[1]).getObject(personalCipher);

                        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
                        field1 = Integer.parseInt(field1text);
                        System.out.println("field1: " + field1);

                        responseSpace.get(new ActualField(Model.RESPONSE_CODE), new FormalField(SealedObject.class));

                        break;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    //e.printStackTrace();
                }
            }

            if ((int) field1 == Model.OK) {
                return true;
            } else if ((int) field1 == Model.BAD_REQUEST) {
                return false;
            }
        }
        return false;
    }

    public void joinLobbyLogic(String lobbyName, UUID lobbyID, int threadGlobalId) throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        // Query the desired lobby-tuple (non-blocking)

        System.out.println("307");

        //[0] lobby code [1] lobby name [2] lobby id
        Object[] tuple = lobbyListSpace.queryp(new ActualField("Lobby"),
                new ActualField(lobbyName),
                new ActualField(lobbyID));

        System.out.println("314");

        if (tuple != null) {

            System.out.println("319");

            currentLobbyName = (String) tuple[1];

            joinLobby((UUID) tuple[2]);

            //Tuple 1 - 3 sealed object

            String messageToBeEncrypted = "" + Model.CONNECT + "!" + getUniqueName() + "?" + threadGlobalId;

            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            SealedObject encryptedKey = new SealedObject(key, lobbyCipher);

            lobbySpace.put(Model.LOBBY_REQ, encryptedMessage, encryptedKey);

            Thread tryToJoinLobby = new Thread(new TimerForLobbyJoining(this));
            tryToJoinLobby.start();

            System.out.println("337");
            serverResponseMonitor.sync();
            System.out.println("340");
            System.out.println("RESULT: " + getResponseFromLobby());
        }
    }

    public void sendDisconnectTuple() throws InterruptedException, IOException, IllegalBlockSizeException {

        // Checks whether the player is leaving during a game or in a lobby
        if(inGame){
            String messageToBeEncrypted = "" + Model.GAME_DISCONNECT + "!" + uniqueName + "?0=*";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
            lobbySpace.put(Model.SERVER_UPDATE, encryptedMessage); // Send the action to the server
        } else if(inLobby){
            //Tuple 1 - 3 sealed object
            String messageToBeEncrypted = "" + Model.LOBBY_DISCONNECT + "!" + uniqueName + "?" + -1;
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
            SealedObject filler = new SealedObject("filler", lobbyCipher);

            lobbySpace.put(Model.LOBBY_REQ, encryptedMessage, filler);
            inLobby = false;
        }
    }
}



