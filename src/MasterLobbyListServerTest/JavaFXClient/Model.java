package MasterLobbyListServerTest.JavaFXClient;

import org.jspace.RemoteSpace;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;

public class Model {

    public static RemoteSpace requestSpace, lobbyListSpace, lobbySpace, responseSpace;
    public static String serverIp;

    protected final static int REQUEST_CODE = 1;
    protected final static int CREATE_LOBBY_REQ = 11;
    protected final static int CREATE_USERNAME_REQ = 12;

    protected final static int RESPONSE_CODE = 2;
    protected final static int CREATE_UNIQUE_USERNAME = 21;

    private int responseFromLobby = 0;

    private String uniqueName;

    private ServerResponseMonitor serverResponseMonitor;

    public Model(){
        serverResponseMonitor = new ServerResponseMonitor();
    }

    public void addIpToRemoteSpaces(String ip) throws IOException {
        requestSpace = new RemoteSpace("tcp://" + ip + ":25565/requestSpace?keep");
        lobbyListSpace = new RemoteSpace("tcp://" + ip + ":25565/lobbyOverviewSpace?keep");
        serverIp = ip;
        responseSpace = new RemoteSpace("tcp://" + ip + ":25565/responseSpace?keep");
    }

    //TODO: rename to "Space"...
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

    public RemoteSpace getResponseSpace(){
        return responseSpace;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public ServerResponseMonitor getServerResponseMonitor() {
        return serverResponseMonitor;
    }

    public void changeResponseFromLobby(int responseFromLobby){
        this.responseFromLobby = responseFromLobby;
    }

    public int getResponseFromLobby() {
        return responseFromLobby;
    }
}
