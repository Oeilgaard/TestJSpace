package MasterLobbyListServerTest.Server_Part;

import MasterLobbyListServerTest.Server_Part.Gameplay.Game;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import java.util.ArrayList;
import java.util.UUID;

public class Lobby implements Runnable {

    public final static int LOBBY_REQ = 30;
    public final static int CONNECT = 31;
    public final static int DISCONNECT = 32;
    public final static int BEGIN = 33;
    public final static int CLOSE = 34;

    public final static int LOBBY_RESP = 40;
    public final static int CONNECT_DENIED = 41;
    public final static int CONNECT_ACCEPTED = 42;

    public final static int LOBBY_UPDATE = 50;
    public final static int CHAT_MESSAGE = 51;

    protected final static int GET_PLAYERLIST = 61;

    private final static int MAX_PLAYER_PR_LOBBY = 5;

    // TODO: hvorfor ikke bare en reference til serverData?
    private SequentialSpace lobbyOverviewSpace;
    private SpaceRepository serverRepos;
    private SequentialSpace lobbySpace;

    private UUID lobbyID;
    private Boolean beginFlag;
    private Boolean inGame;

    private String lobbyLeader;
    private int noPlayers;
    private ArrayList<String> players;

    public Lobby(UUID lobbyID, SequentialSpace lobbyOverviewSpace, SpaceRepository serverRepos, String lobbyLeader){
        this.lobbyID = lobbyID;
        this.lobbyOverviewSpace = lobbyOverviewSpace;
        this.serverRepos = serverRepos;
        this.lobbyLeader = lobbyLeader;
        this.inGame = false;
        this.beginFlag = false;
        this.players = new ArrayList<>();
    }

    @Override
    public void run() {

        lobbySpace = new SequentialSpace();
        serverRepos.add(lobbyID.toString(),lobbySpace);

        System.out.println("Lobby is now running\n");

        Thread chatAgent = new Thread(new LobbyChatAgent(lobbySpace,players));
        chatAgent.start();

        BeginLoop:
        while(true) {
            try {
                // [0] LOBBY-tuple code, [1] LOBBY action code, [2] User name (for some tuples)
                Object[] tuple = lobbySpace.get(new ActualField(LOBBY_REQ), new FormalField(Integer.class), new FormalField(String.class));
                int req = (int) tuple[1];
                String name = (String) tuple[2];

                if (req == CONNECT) {
                    if (noPlayers < MAX_PLAYER_PR_LOBBY) {
                        players.add(name); // add player to players
                        noPlayers++;
                        Boolean isThisPlayerLobbyLeader = false;
                        if(name.equals(lobbyLeader)){
                            isThisPlayerLobbyLeader = true;
                        }
                        lobbySpace.put(LOBBY_RESP, CONNECT_ACCEPTED, name, isThisPlayerLobbyLeader);
                        updatePlayers(name, CONNECT);
                    } else { // lobby full
                        lobbySpace.put(LOBBY_RESP, CONNECT_DENIED, name, false);
                    }
                } else if (req == DISCONNECT) {
                    if (name.equals(lobbyLeader)) {
                        System.out.println("The lobby leader left! Lobby is closing");
                        beginFlag = false;
                        updatePlayers(name, CLOSE);
                        break BeginLoop;
                    }
                    players.remove(name); // remove player from players
                    noPlayers--;
                    updatePlayers(name, DISCONNECT);
                } else if (req == CLOSE) {
                    System.out.println("Lobby is closing");
                    beginFlag = false;
                    updatePlayers(name, CLOSE);
                    break BeginLoop;
                } else if (req == BEGIN && name.equals(lobbyLeader)) {
                    if (noPlayers >= 2) {
                        System.out.println("Ready to begin!");
                        beginFlag = true;
                        updatePlayers(name, BEGIN);
                        break BeginLoop;
                    } else {
                        System.out.println("Not enough players to begin");
                    }
                } else if (req == GET_PLAYERLIST) {
                    lobbySpace.put(LOBBY_RESP, players, name);
                } else {
                    System.out.println("Unknown request");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Remove Lobby from Lobby List overview space
        try {
            lobbyOverviewSpace.get(new ActualField("Lobby"),new FormalField(String.class),new ActualField(lobbyID));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start the game
        if(beginFlag){
            inGame = true;
            //GameplayDummy gp = new GameplayDummy(players);
            //gp.runGamePlay();
            Game game = new Game(players, lobbySpace);
            game.startGame();
        }

        chatAgent.interrupt();
        serverRepos.remove(lobbyID.toString());
        System.out.println("Lobby is closed");
    }

    public void updatePlayers(String actingPlayer, int action) throws InterruptedException {
        if(action==BEGIN) {
            for(String p : players){
                // burde 'responded' også stå her?
                lobbySpace.put(LOBBY_UPDATE,action,p, "");
            }
        } else {
            for(String p : players) {
                if(p != actingPlayer) {
                    lobbySpace.put(LOBBY_UPDATE,action,p, "");
                }
            }
        }
    }
}
