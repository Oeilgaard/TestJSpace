package MasterLobbyListServerTest.Server_Part;

import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import java.util.UUID;

public class Lobby implements Runnable {

    private UUID lobbyID;
    private SequentialSpace entrySpace;
    private SpaceRepository serverRepos;

    public Lobby(UUID lobbyID, SequentialSpace entrySpace, SpaceRepository serverRepos){
        this.lobbyID = lobbyID;
        this.entrySpace = entrySpace;
        this.serverRepos = serverRepos;
    }

    @Override
    public void run() {

    }
}
