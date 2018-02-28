package MasterLobbyListServerTest.JavaFXClient;

import org.jspace.RemoteSpace;

import java.io.IOException;

public class Model {

    public static RemoteSpace requestSpace, lobbyListSpace, lobbySpace;

    public void addIpToRemoteSpaces(String ip) throws IOException {
        requestSpace = new RemoteSpace("tcp://" + ip + ":25565/requestSpace?keep");
        lobbyListSpace = new RemoteSpace("tcp://" + ip + ":25565/lobbyOverviewSpace?keep");
    }

    public RemoteSpace getLobbyList(){
        return lobbyListSpace;
    }

    public RemoteSpace getRequest(){
        return requestSpace;
    }
}
