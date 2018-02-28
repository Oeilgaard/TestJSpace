package MasterLobbyListServerTest.JavaFXClient;

import org.jspace.RemoteSpace;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;

public class Model {

    public static RemoteSpace requestSpace, lobbyListSpace, lobbySpace;
    public static String serverIp;

    public void addIpToRemoteSpaces(String ip) throws IOException {
        requestSpace = new RemoteSpace("tcp://" + ip + ":25565/requestSpace?keep");
        lobbyListSpace = new RemoteSpace("tcp://" + ip + ":25565/lobbyOverviewSpace?keep");
        serverIp = ip;
    }

    public RemoteSpace getLobbyList(){
        return lobbyListSpace;
    }

    public RemoteSpace getRequest(){
        return requestSpace;
    }

    public RemoteSpace getLobbySpace() {return lobbySpace;}

    public void joinLobby(UUID lobbyid) throws IOException {
        lobbySpace = new RemoteSpace("tcp://" + serverIp + ":25565/" + lobbyid.toString() + "?keep");
    }
}
