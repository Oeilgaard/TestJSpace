package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;
import java.util.UUID;

public class Lobby implements Runnable {

    private UUID lobbyID;
    private SequentialSpace requestSpace;
    private SpaceRepository serverRepos;
    private SequentialSpace lobbySpace;

    private PlayerInfo playerInfo;

    private final static int MAX_PLAYER_PR_LOBBY = 5;

    public Lobby(UUID lobbyID, SequentialSpace requestSpace, SpaceRepository serverRepos){
        this.lobbyID = lobbyID;
        this.requestSpace = requestSpace;
        this.serverRepos = serverRepos;
        playerInfo = new PlayerInfo(MAX_PLAYER_PR_LOBBY);
    }

    @Override
    public void run() {

        lobbySpace = new SequentialSpace();
        serverRepos.add(lobbyID.toString(),lobbySpace);

        //Run lobbyConnectionManager here!
        Thread lobbyConnectionManager = new Thread(new LobbyConnectionManager(lobbySpace, playerInfo));
        lobbyConnectionManager.start();

        System.out.println("Lobby is now running\n");

        BeginLoop:
        while(true) {
            System.out.println("Waiting for begin");
            try {
                lobbySpace.get(new ActualField("Begin"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(playerInfo.nrOfPlayers >= 2){
                System.out.println("Ready to begin!");
                break BeginLoop;
            } else {
                System.out.println("Not enough players to begin");
            }
        }

        Gameplay gp = new Gameplay(playerInfo);

        gp.RunGamePlay();
    }
}
