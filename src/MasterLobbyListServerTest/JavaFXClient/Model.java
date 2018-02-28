package MasterLobbyListServerTest.JavaFXClient;

import org.jspace.RemoteSpace;

import java.io.IOException;

public class Model {

    public static RemoteSpace requestSpace, lobbyListSpace, lobbySpace, responseSpace;

    protected final static int REQUEST_CODE = 1;
    protected final static int CREATE_LOBBY_REQ = 11;
    protected final static int CREATE_USERNAME_REQ = 12;
    protected final static int RESPONSE_CODE = 2;
    protected final static int CREATE_UNIQUE_USERNAME = 21;

    private String uniqueName;

    public void addIpToRemoteSpaces(String ip) throws IOException {
        requestSpace = new RemoteSpace("tcp://" + ip + ":25565/requestSpace?keep");
        lobbyListSpace = new RemoteSpace("tcp://" + ip + ":25565/lobbyOverviewSpace?keep");
        responseSpace = new RemoteSpace("tcp://" + ip + ":25565/responseSpace?keep");
    }

    //TODO: rename to "Space"...
    public RemoteSpace getLobbyList(){
        return lobbyListSpace;
    }

    public RemoteSpace getRequest(){
        return requestSpace;
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

}
