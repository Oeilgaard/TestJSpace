package MasterLobbyListServerTest.Server_Part;

import MasterLobbyListServerTest.Server_Part.Gameplay.Game;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;

public class Lobby implements Runnable {

    private final static int LOBBY_REQ = 30;
    private final static int CONNECT = 31;
    private final static int DISCONNECT = 32;
    private final static int BEGIN = 33;
    private final static int CLOSE = 34;

    private final static int LOBBY_RESP = 40;
    private final static int CONNECT_DENIED = 41;
    private final static int CONNECT_ACCEPTED = 42;

    public final static int LOBBY_UPDATE = 50;
    public final static int CHAT_MESSAGE = 51;

    private final static int GET_PLAYERLIST = 61;

    private final static int MAX_PLAYER_PR_LOBBY = 5;

    // TODO: hvorfor ikke bare en reference til serverData?
    // the servers TS
    private SequentialSpace lobbyOverviewSpace;
    private SpaceRepository serverRepos;
    // this lobby's TS
    private SequentialSpace lobbySpace;

    private UUID lobbyID;
    private Boolean beginFlag;
    private String lobbyLeader;
    private int noPlayers;
    private ArrayList<LobbyUser> users;
    private ArrayList<Integer> availableNrs;
    private ServerData serverData;

    public Lobby(UUID lobbyID, SequentialSpace lobbyOverviewSpace, SpaceRepository serverRepos, String lobbyLeader, ServerData serverData){
        this.lobbyID = lobbyID;
        this.lobbyOverviewSpace = lobbyOverviewSpace;
        this.serverRepos = serverRepos;
        this.lobbyLeader = lobbyLeader;
        this.beginFlag = false;
        this.serverData = serverData;
        this.users = new ArrayList<>();
        this.availableNrs = new ArrayList<>();
        for(int i = 0; i < 5; i++){
            availableNrs.add(i);
        }
    }

    @Override
    public void run() {

        lobbySpace = new SequentialSpace();
        serverRepos.add(lobbyID.toString(),lobbySpace);

        System.out.println("Lobby is now running\n");

        // A seperate thread for listening to chat messages
        Thread chatAgent = new Thread(new LobbyChatAgent(lobbySpace,users,serverData.cipher));
        chatAgent.start();

        // Stays in lobby loop until game starts or terminated
        lobbyLoop();

        /* CLOSING THE LOBBY (AND STARTING THE GAME IF APPLICABLE) */

        // Remove Lobby from Lobby List overview space
        try {
            // [0] lobby code [1] lobbyname [2] lobby-id
            lobbyOverviewSpace.get(new ActualField("Lobby"),new FormalField(String.class),new ActualField(lobbyID));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stopping the chat thread
        //TODO: is it actaully stopping with interrupt?
        chatAgent.interrupt();

        // Start the game
        if(beginFlag){
            Game game = new Game(users, lobbySpace, serverData);
            try {
                game.startGame();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }

        serverRepos.remove(lobbyID.toString());
        System.out.println("Lobby is closed");
    }

    public void lobbyLoop(){
        while (true) {
            try {
                // Wait for a new LOBBY_REQ tuple
                // [0] LOBBY-tuple code, [1] (int)LOBBY action code, [2] (String)User name (for some tuples) [3] (int)thread nr for the specific user
                Object[] tuple = lobbySpace.get(new ActualField(LOBBY_REQ), new FormalField(SealedObject.class), new FormalField(SealedObject.class));

                // Decrypting the LOBBY_REQ tuple
                String decryptedString = (String) ((SealedObject)tuple[1]).getObject(serverData.cipher);
                String field1 = decryptedString.substring(0,decryptedString.indexOf('!'));
                String field2 = decryptedString.substring(decryptedString.indexOf('!')+1,decryptedString.indexOf('?'));
                String field3 = decryptedString.substring(decryptedString.indexOf('?')+1,decryptedString.length());
                int req = Integer.parseInt(field1);
                String name = field2;

                // If request type is CONNECT, A new client tries to connect to the lobby
                if (req == CONNECT) {
                    // TODO: hvad sker der her?
                    Key key = (Key) ((SealedObject)tuple[2]).getObject(serverData.cipher);
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE,key);

                    if (noPlayers < MAX_PLAYER_PR_LOBBY) {
                        users.add(new LobbyUser(name,Integer.parseInt(field3),cipher,availableNrs.get(0)));
                        availableNrs.remove(0);
                        noPlayers++;
                        Boolean isThisPlayerLobbyLeader = false;
                        if (name.equals(lobbyLeader)) {
                            isThisPlayerLobbyLeader = true;
                        }
                        SealedObject encryptedMessage = new SealedObject(name + "!" + isThisPlayerLobbyLeader + "?" + getUserfromName(name).userNr, cipher);
                        lobbySpace.put(LOBBY_RESP, CONNECT_ACCEPTED,encryptedMessage);
                        updatePlayers(name, CONNECT);
                    } else { // lobby full
                        SealedObject encryptedMessage = new SealedObject(name + "!false?" + -1, cipher);
                        lobbySpace.put(LOBBY_RESP, CONNECT_DENIED, encryptedMessage);
                    }
                } else if (req == DISCONNECT) { // If request type is DISCONNECT, the sending client disconnects
                    if (name.equals(lobbyLeader)) {
                        System.out.println("The lobby leader left! Lobby is closing");
                        beginFlag = false;
                        updatePlayers(name, CLOSE);
                        break;
                    }
                    int indexForPlayer = getUserfromName(name).userNr;
                    users.remove(getUserfromName(name)); // remove player from players
                    availableNrs.add(indexForPlayer);
                    noPlayers--; //TODO: should it be synchronized? (Probably not, as only one thread)
                    updatePlayers(name, DISCONNECT);
                } else if (req == CLOSE) { // If request type is CLOSE,
                    System.out.println("Lobby is closing");
                    beginFlag = false;
                    updatePlayers(name, CLOSE);
                    break;
                } else if (req == BEGIN && name.equals(lobbyLeader)) { // the lobby is going in game
                    if (noPlayers >= 2) {
                        System.out.println("Ready to begin!");
                        beginFlag = true;
                        updatePlayers(name, BEGIN);
                        break;
                    } else {
                        System.out.println("Not enough players to begin");
                        //TODO: perhaps send a tuple back
                    }
                } else if (req == GET_PLAYERLIST) { // If request is GET_PLAYERLIST, a client requests the list of player's in the lobby

                    // TODO: hvorfor ikke returnere private ArrayList<LobbyUser> users?
                    ArrayList<String> usernames = new ArrayList<>();
                    for (LobbyUser user : users) {
                        String s = user.name;
                        s = s.substring(0, s.indexOf("#"));
                        usernames.add(s);
                    }
                    lobbySpace.put(LOBBY_RESP, usernames, getUserfromName(name).userNr);
                } else {
                    System.out.println("Unknown request");
                    System.out.println(field1.toString());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }
    }

    private LobbyUser getUserfromName(String name) {
        for (LobbyUser user : users){
            if(user.name.equals(name)){
                return user;
            }
        }
        return null;
    }

    private void updatePlayers(String actingPlayer, int action) throws InterruptedException {
        if(action==BEGIN) {
            for(LobbyUser u : users){
                String p = u.name;
                // burde 'responded' også stå her?
                System.out.println("Informing : " + p + " that the game has started");

                lobbySpace.put(LOBBY_UPDATE,action,"",u.threadNr, u.userNr);
            }
        } else {
            for(LobbyUser u : users) {
                String p = u.name;
                if(!p.equals(actingPlayer)) {
                    lobbySpace.put(LOBBY_UPDATE,action,"",u.threadNr, u.userNr);
                }
            }
        }
    }
}
