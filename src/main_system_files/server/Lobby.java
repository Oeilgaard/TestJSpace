package main_system_files.server;

import main_system_files.server.gameplay.Game;
import main_system_files.server.gameplay.Model;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import javax.crypto.*;
import java.io.IOException;
import java.security.*;
import java.util.ArrayList;
import java.util.UUID;

public class Lobby implements Runnable {

    public final static int LOBBY_REQ = 30;
    public final static int CONNECT = 31;
    public final static int DISCONNECT = 32;
    public final static int BEGIN = 33;
    public final static int CLOSE = 34;

    public final static int S2C_CONNECT_RESP = 40;
    public final static int CONNECT_DENIED = 41;
    public final static int CONNECT_ACCEPTED = 42;
    public final static int S2C_GET_PLAYERLIST_RESP = 43;

    public final static int S2C_LOBBY = 50;
    public final static int CHAT_MESSAGE = 51;
    public final static int NOT_ENOUGH_PLAYERS = 52;

    public final static int GET_PLAYERLIST = 61;

    public final static int MAX_PLAYER_PR_LOBBY = 4;

    // the servers TS
    private SequentialSpace lobbyOverviewSpace;
    private SpaceRepository serverRepos;
    // this lobby's TS
    private SequentialSpace lobbySpace;

    private UUID lobbyID;
    private Boolean beginFlag;
    public String lobbyLeader;
    private int noPlayers;
    private ArrayList<LobbyUser> users;
    private ArrayList<Integer> availableNrs;
    private ServerData serverData;
    private PrivateKey pKey;
    private Cipher lobbyCipher;
    private KeyPair myPair;
    public Game game;

    private static int connectedInt = 0;

    public Lobby(UUID lobbyID, SequentialSpace lobbyOverviewSpace, SpaceRepository serverRepos, String lobbyLeader, ServerData serverData) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        this.lobbyID = lobbyID;
        this.lobbyOverviewSpace = lobbyOverviewSpace;
        this.serverRepos = serverRepos;
        this.lobbyLeader = lobbyLeader;
        this.beginFlag = false;
        this.serverData = serverData;
        this.users = new ArrayList<>();
        this.availableNrs = new ArrayList<>();
        for(int i = 0; i < 4; i++){
            availableNrs.add(i);
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        myPair = kpg.generateKeyPair();
        pKey = myPair.getPrivate();

        // Putting the Public Key for communication to the server in the "request space"

        // Create an instance of the Cipher for RSA encryption/decryption
        lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.DECRYPT_MODE, myPair.getPrivate());

    }

    @Override
    public void run() {

        lobbySpace = new SequentialSpace();
        serverRepos.add(lobbyID.toString(),lobbySpace);

        try {
            lobbySpace.put(myPair.getPublic());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        System.out.println("Lobby is now running\n");

        // A seperate thread for listening to chat messages
        Thread chatAgent = new Thread(new LobbyChatAgent(lobbySpace,users));
        chatAgent.start();

        // Stays in lobby loop until game starts or terminated
        lobbyLoop();

        /* CLOSING THE LOBBY (AND STARTING THE GAME IF APPLICABLE) */

        // Remove Lobby from Lobby List overview space
        try {
            // [0] lobby code [1] lobbyname [2] lobby-id
            lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO),new FormalField(String.class),new ActualField(lobbyID));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stopping the chat thread
        chatAgent.interrupt();

        // Start the game
        if(beginFlag){
            game = new Game(users, lobbySpace, serverData, lobbyCipher);

            try {
                game.startGame();
            } catch (InterruptedException | IllegalBlockSizeException | IOException e) {
                e.printStackTrace();
            }
        }

        serverData.removeLobbyFromMap(lobbyID);
        //serverData.decrementCurrentNoThreads();
        serverRepos.remove(lobbyID.toString());
        System.out.println("Lobby is closed");
    }

    private void lobbyLoop(){
        while (true) {
            try {
                // Wait for a new LOBBY_REQ tuple
                // [0] LOBBY-tuple code, [1] (int)LOBBY action code, [2] (String)User name (for some tuples) [3] (int)thread nr for the specific user
                Object[] tuple = lobbySpace.get(new ActualField(Model.C2S_LOBBY_GAME), new FormalField(SealedObject.class), new FormalField(SealedObject.class));
                System.out.println("Received a lobby request");

                // Decrypting the LOBBY_REQ tuple
                String decryptedString = (String) ((SealedObject) tuple[1]).getObject(lobbyCipher);
                String field1 = decryptedString.substring(0, decryptedString.indexOf('!'));
                String field2 = decryptedString.substring(decryptedString.indexOf('!') + 1, decryptedString.indexOf('?'));
                String field3 = decryptedString.substring(decryptedString.indexOf('?') + 1, decryptedString.indexOf('='));
                int req = Integer.parseInt(field1);
                String name = field2;

                // If request type is CONNECT, A new client tries to connect to the lobby
                if (req == CONNECT) {
                    Key key = (Key) ((SealedObject) tuple[2]).getObject(lobbyCipher);
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, key);

                    if ((noPlayers < MAX_PLAYER_PR_LOBBY) && userHasLegalUserID(name)) {
                        users.add(new LobbyUser(name, Integer.parseInt(field3), cipher, availableNrs.get(0)));
                        availableNrs.remove(0);
                        noPlayers++;
                        Boolean isThisPlayerLobbyLeader = false;
                        if (name.equals(lobbyLeader)) {
                            isThisPlayerLobbyLeader = true;
                        }
                        SealedObject encryptedMessage = new SealedObject(name + "!" + isThisPlayerLobbyLeader + "?" + getUserfromuserID(name).userNr, cipher);
                        lobbySpace.put(S2C_CONNECT_RESP, CONNECT_ACCEPTED, encryptedMessage);
                        updatePlayers(name, CONNECT);
                        connectedInt++;

                    } else { // lobby full or illegal userID
                        SealedObject encryptedMessage = new SealedObject(name + "!false?" + -1, cipher);
                        lobbySpace.put(S2C_CONNECT_RESP, CONNECT_DENIED, encryptedMessage);
                    }
                    System.out.println("Connect response handled");

                } else if (req == DISCONNECT) { // A client in the lobby disconnects
                    if (name.equals(lobbyLeader)) {
                        System.out.println("The lobby leader left! Lobby is closing");
                        beginFlag = false;
                        updatePlayers(name, CLOSE);
                        break;
                    }
                    int indexForPlayer = getUserfromuserID(name).userNr;
                    users.remove(getUserfromuserID(name)); // remove player from players
                    availableNrs.add(indexForPlayer);
                    noPlayers--;
                    updatePlayers(name, DISCONNECT);
                /*} else if (req == CLOSE) { // If request type is CLOSE,
                    System.out.println("Lobby is closing");
                    beginFlag = false;
                    updatePlayers(name, CLOSE);
                    break;*/
                } else if (req == BEGIN && name.equals(lobbyLeader)) { // the lobby is going in game
                    if (noPlayers >= 2) {
                        System.out.println("Ready to begin!");
                        Thread.sleep(2000);
                        beginFlag = true;
                        updatePlayers(name, BEGIN);
                        break;
                    } else {
                        System.out.println("Not enough players to begin");
                        updatePlayers(name, NOT_ENOUGH_PLAYERS); // there will just be the one player
                    }
                } else if (req == GET_PLAYERLIST) { // If request is GET_PLAYERLIST, a client requests the list of player's in the lobby

                    ArrayList<String> userNames = new ArrayList<>();
                    for (LobbyUser user : users) {
                        String s = user.userID;
                        s = s.substring(0, s.indexOf("#"));
                        userNames.add(s);
                    }
                    lobbySpace.put(S2C_GET_PLAYERLIST_RESP, userNames, getUserfromuserID(name).userNr);
                } else {
                    System.out.println("Unknown request");
                    System.out.println(field1);
                }
            } catch (InterruptedException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | ClassNotFoundException | IOException | IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }
        }
    }

    private LobbyUser getUserfromuserID(String userID) {
        for (LobbyUser user : users){
            if(user.userID.equals(userID)){
                return user;
            }
        }
        return null;
    }

    private void updatePlayers(String actingPlayer, int action) throws InterruptedException {
        if(action==BEGIN || action==NOT_ENOUGH_PLAYERS) {
            for(LobbyUser u : users){
                //String p = u.name;
                // burde 'responded' også stå her?
                //System.out.println("Informing : " + p + " that the game has started");
                lobbySpace.put(S2C_LOBBY,action, "", u.lobbyAgentNo, u.userNr);
            }
        } else {
            for(LobbyUser u : users) {
                String p = u.userID;
                if(!p.equals(actingPlayer)) {
                    lobbySpace.put(S2C_LOBBY, action, "", u.lobbyAgentNo, u.userNr);
                }
            }
        }
    }

    public boolean userHasLegalUserID(String s){
        for (LobbyUser u : users){
            if (u.userID == s){
                return false;
            }
        }
        if(s.indexOf('#') != -1){
            String subS1 = s.substring(0,s.indexOf('#'));
            String subS2 = s.substring(s.indexOf('#')+1,s.length());
            return (HelperFunctionsServer.stringMatchesUUIDPattern(subS2) && HelperFunctionsServer.validName(subS1));
        } else {
            return false;
        }
    }

    public String getLobbyLeader(){ return lobbyLeader; }
    public boolean gameBegun(){
        return beginFlag;
    }

}
