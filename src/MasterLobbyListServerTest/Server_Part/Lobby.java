package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.UUID;

public class Lobby implements Runnable {

    private UUID lobbyID;
    private SequentialSpace requestSpace;
    private SpaceRepository serverRepos;
    private SequentialSpace lobbySpace;

    private int nrOfPlayer = 0;

    public Lobby(UUID lobbyID, SequentialSpace requestSpace, SpaceRepository serverRepos){
        this.lobbyID = lobbyID;
        this.requestSpace = requestSpace;
        this.serverRepos = serverRepos;
    }

    @Override
    public void run() {

        lobbySpace = new SequentialSpace();
        serverRepos.add(lobbyID.toString(),lobbySpace);

        //Run lobbyConnectionManager here!

        System.out.println("Lobby is now running\n");

        BeginLoop:
        while(true) {
            System.out.println("Waiting for begin");
            try {
                lobbySpace.get(new ActualField("Begin"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(nrOfPlayer >= 2){
                System.out.println("Ready to begin!");
                break BeginLoop;
            } else {
                System.out.println("Not enough players to begin");
            }
        }

        Gameplay gp = new Gameplay();

        gp.RunGamePlay();
    }
}
