package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;
import java.util.UUID;

public class Lobby implements Runnable {

    private UUID lobbyID;
    private SequentialSpace lobbyOverviewSpace;
    private SpaceRepository serverRepos;
    private SequentialSpace lobbySpace;

    private PlayerInfo playerInfo;
    private String lobbyLeader;

    private Boolean beginFlag = true;

    private final static int MAX_PLAYER_PR_LOBBY = 5;

    public static int LOBBY_MESSAGE = 200;

    public Lobby(UUID lobbyID, SequentialSpace lobbyOverviewSpace, SpaceRepository serverRepos, String lobbyLeader){
        this.lobbyID = lobbyID;
        this.lobbyOverviewSpace = lobbyOverviewSpace;
        this.serverRepos = serverRepos;
        this.lobbyLeader = lobbyLeader;
        playerInfo = new PlayerInfo(MAX_PLAYER_PR_LOBBY);
    }

    @Override
    public void run() {

        lobbySpace = new SequentialSpace();
        serverRepos.add(lobbyID.toString(),lobbySpace);

        Thread lobbyConnectionManager = new Thread(new LobbyConnectionManager(lobbySpace, playerInfo, lobbyLeader));
        lobbyConnectionManager.start();

        System.out.println("Lobby is now running\n");

        BeginLoop:
        while(true) {
            System.out.println("Waiting to begin");
            Object[] tuple = null;
            try {
                tuple = lobbySpace.get(new ActualField(LOBBY_MESSAGE), new FormalField(String.class));

                if(tuple[1].equals("Closing")){
                    System.out.println("Lobby is closing");
                    beginFlag = false;
                    break BeginLoop;

                } else if(playerInfo.nrOfPlayers >= 2 && tuple[1].equals("Begin")){

                    System.out.println("Ready to begin!");
                    break BeginLoop;

                } else {
                    System.out.println("Not enough players to begin");
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        lobbyConnectionManager.interrupt();

        if(beginFlag) {

            Gameplay gp = new Gameplay(playerInfo);

            gp.RunGamePlay();
        }

        try {
            lobbyOverviewSpace.get(new ActualField("Lobby"),new FormalField(String.class),new ActualField(lobbyID));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        serverRepos.remove(lobbyID.toString());
    }
}
