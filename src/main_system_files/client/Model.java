package main_system_files.client;

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
    protected final static int NOT_ENOUGH_PLAYERS = 52;

    // Query tuples
    //protected final static int GAMEPLAY_INFO = 60;
    protected final static int GET_PLAYERLIST = 61;
    //protected final static int PLAYERS_IN_ROUND = 62;

    //protected final static int GAMEPLAY_ACTION = 70;
    //protected final static int PLAY_CARD = 71;

    protected final static int TARGETS_REQUEST = 85;
    protected final static int TARGETS_RESPONSE = 86;

    protected final static int LOBBY_INFO = 90;

    // 'HTTP style'
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

    private static RemoteSpace requestSpace, lobbyOverviewSpace, lobbySpace, responseSpace;
    private static String serverIp;
    private int responseFromLobby = NO_RESPONSE;
    private String userID;
    private LobbyResponseMonitor serverResponseMonitor;
    private static int currentThreadNumber = 0;

    public Parent currentRoot;

    public ArrayList<String> cardsOnHand = new ArrayList<>();
    public ArrayList<String> actionHistory = new ArrayList<>();

    private PublicKey serverPublicKey;
    private Cipher serverCipher;

    private Cipher lobbyCipher;

    public Key key;
    public Cipher personalCipher;

    private boolean inGame = false;
    private boolean inLobby = false;
    private boolean isLeader = false;

    private static String currentLobbyName;

    public Model() {
        serverResponseMonitor = new LobbyResponseMonitor();
    }

    public Thread updateAgent;

    private int indexInLobby = -1;

    public static boolean currentSceneIsGameScene = false;

    public void setCurrentThreadNumber(int currentThreadNumber) {
        Model.currentThreadNumber = currentThreadNumber;
    }

    public void incrementCurrentThreadNumber(){
        currentThreadNumber++;
    }

    public void addIpToRemoteSpaces(String ip) throws IOException {

        requestSpace = new RemoteSpace("tcp://" + ip + ":25565/requestSpace?keep");
        lobbyOverviewSpace = new RemoteSpace("tcp://" + ip + ":25565/lobbyOverviewSpace?keep");
        serverIp = ip;
        responseSpace = new RemoteSpace("tcp://" + ip + ":25565/responseSpace?keep");

    }

    public void setPublicKey(PublicKey pk) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        serverPublicKey = pk;

        serverCipher = Cipher.getInstance("RSA");
        serverCipher.init(Cipher.ENCRYPT_MODE, pk);
    }

    public boolean isLeader(){
        return isLeader;
    }

    public Cipher getServerCipher() {
        return serverCipher;
    }

    public Cipher getLobbyCipher() { return lobbyCipher; }

    public int getCurrentThreadNumber(){
        return currentThreadNumber;
    }

    public RemoteSpace getLobbyListSpace() {
        return lobbyOverviewSpace;
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

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public LobbyResponseMonitor getServerResponseMonitor() {
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

    public boolean getInLobby() {
        return inLobby;
    }

    public void setInLobby(boolean inLobby) {
        this.inLobby = inLobby;
    }

    public String getUserName(){ return userID.substring(0, userID.indexOf("#")); }

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

            Object[] tuple = null;
            int field1;
            String field3;

            while (true) {
                // Blocks until user receives unique username (due to 'query')
                // [0] response code [1] Response [2] Ok or error [3] Username of receiver [4] Username with ID
                try {
                    tuple = responseSpace.get(new ActualField(Model.RESPONSE_CODE), new ActualField(Model.ASSIGN_UNIQUE_USERNAME_RESP),
                            new FormalField(SealedObject.class));

                    if (tuple != null) {

                        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(personalCipher);

                        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
                        field1 = Integer.parseInt(field1text);
                        field3 = decryptedMessage.substring(decryptedMessage.indexOf('?') + 1, decryptedMessage.length());

                        //KLARET: kan man ikke komme til at fjerne en ANDEN tuple en den man lige har samlet op?
                        //responseSpace.get(new ActualField(Model.RESPONSE_CODE), new ActualField(Model.ASSIGN_UNIQUE_USERNAME_RESP),
                        //        new FormalField(SealedObject.class));

                        break;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    //e.printStackTrace();
                    responseSpace.put(tuple[0],tuple[1],tuple[2]);
                }
            }

            if ((int) field1 == Model.OK) {
                setUserID((String) field3); // Setting the user's name
                return true;
            } else if ((int) field1 == Model.BAD_REQUEST) {
                return false;
            }
        }
        return false;
    }

    public boolean createLobbyLogic(String lobbyNameString) throws IOException, IllegalBlockSizeException, InterruptedException {

        System.out.println("The user's ID: " + userID);

        SealedObject encryptedLobbyNameString = new SealedObject(lobbyNameString + "!" + userID, serverCipher);

        if (HelperFunctions.validName(lobbyNameString)) {
            //createLobbyButton.setDisable(true);
            //instructionsLobbyName.setText("");

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

    public int joinLobbyLogic(String lobbyName, UUID lobbyID, int currentThreadNumber) throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        // Query the desired lobby-tuple (non-blocking)

        //[0] lobby code [1] lobby name [2] lobby id
        Object[] tuple = lobbyOverviewSpace.queryp(new ActualField(Model.LOBBY_INFO),
                new ActualField(lobbyName),
                new ActualField(lobbyID));

        if (tuple != null) {

            currentLobbyName = (String) tuple[1];

            joinLobby((UUID) tuple[2]);

            //Tuple 1 - 3 sealed object

            String messageToBeEncrypted = "" + Model.CONNECT + "!" + getUserID() + "?" + currentThreadNumber + "=*¤";

            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            SealedObject encryptedKey = new SealedObject(key, lobbyCipher);

            lobbySpace.put(Model.SERVER_UPDATE, encryptedMessage, encryptedKey);

            Thread tryToJoinLobby = new Thread(new LobbyConnectionTimer(this));
            tryToJoinLobby.start();

            serverResponseMonitor.sync();

            System.out.println("RESULT: " + getResponseFromLobby());
        }
        int result = getResponseFromLobby();
        responseFromLobby = Model.NO_RESPONSE; // reset for future calls
        return result;
    }

    public void sendDisconnectTuple() throws InterruptedException, IOException, IllegalBlockSizeException {

        // Checks whether the player is leaving during a game or in a lobby
            //Tuple 1 - 3 sealed object
            String messageToBeEncrypted = "" + Model.LOBBY_DISCONNECT + "!" + userID + "?" + -1 + "=*¤";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
            SealedObject filler = new SealedObject("filler", lobbyCipher);

            lobbySpace.put(Model.SERVER_UPDATE, encryptedMessage, filler);
            inLobby = false;
    }

    public void pressBeginLogic() throws IOException, IllegalBlockSizeException, InterruptedException {

        if (isLeader) {
            //System.out.println("Yes");

            //Tuple 1 - 3 sealed object
            String messageToBeEncrypted = "" + Model.BEGIN + "!" + userID + "?=*¤";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
            SealedObject filler = new SealedObject("filler", lobbyCipher);

            lobbySpace.put(Model.SERVER_UPDATE, encryptedMessage, filler);
        }

    }

    public ArrayList<String> updatePlayerLobbyListLogic() throws IOException, IllegalBlockSizeException, InterruptedException {
        //Encrypting the tuple

        String messageToBeEncrypted = "" + Model.GET_PLAYERLIST + "!" + userID + "?" + -1 + "=*¤";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, getLobbyCipher());

        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.SERVER_UPDATE, encryptedMessage, filler);

        // [0] response code [1] list of playernames [2] username
        Object[] tuple = lobbySpace.get(new ActualField(Model.LOBBY_RESP), new FormalField(ArrayList.class), new FormalField(Integer.class)); //ændret fra new ActualField(model.indexInLobby), har vist også ændret det et andet sted

        return (ArrayList<String>) tuple[1];
    }

    public int getIndexInLobby() {
        return indexInLobby;
    }

    public void setIndexInLobby(int index) {
        this.indexInLobby = index;
    }

    public void textToChatLogic(String text) throws InterruptedException {
        String textToSend = HelperFunctions.removeUUIDFromUserName(userID) + " : " + text;
        lobbySpace.put("Chat", textToSend);
    }

    public void setIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
    }
}



