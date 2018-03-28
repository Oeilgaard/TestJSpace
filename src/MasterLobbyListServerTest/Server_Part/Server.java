package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    /*
        1: Create Space of lobbylist and requests.
        2: Start a manager to look for requests for new lobby.
        3: If request for new lobby, create new space with unique id + name given by requesting client.
        4: Add lobby to the lobbylist
        5: Send id to client.
        6: Start manager for the lobby.
        7: If creator of lobby leaves, close space.
        8: Remove lobby from the lobby list if the lobby moves to gameplay.

        9: Lobby handles users and gameplay with its own managers.

        Optional: Password protected. Join friends.

        NOTER:
        ---------------------------------------
        tcp://"0.0.0.0":25565/entrySpace?keep

        tcp://"0.0.0.0":25565/7000?keep

        Template mainTemplate = new Template(new ActualField(REQUEST_CODE), new FormalField(int.class), new FormalField(String.class));
        Object[] tuple = entrySpace.get(mainTemplate.getFields());
        ---------------------------------------
     */

    protected static int CREATE_LOBBY_REQ = 11;
    protected static int CREATE_USERNAME_REQ = 12;
    protected static int JOIN_LOBBY_REQ = 13;

    protected static int RESPONSE_CODE = 2;
    protected final static int ASSIGN_UNIQUE_USERNAME_RESP = 23;

    // 'HTTP style'
    protected final static int OK = 200;
    protected final static int BAD_REQUEST = 400;

    public static void main(String[] argv) throws InterruptedException, IOException {

        ServerData serverData = new ServerData();
        System.out.println("ServerData is initialised");

        while (true) {

            //TODO: Re-evaluate req. and response codes - are they unnessacary as we have seperate spaces?

            // [0] request code,[1] request type, [2] username to request/name of lobby, [3] null/username for lobby owner
            int REQUEST_CODE = 1;
            Object[] tuple = serverData.requestSpace.get(new ActualField(REQUEST_CODE), new FormalField(Integer.class), new FormalField(String.class), new FormalField(String.class));

            Runnable reqHandler = new RequestHandlerThread(serverData, tuple);
            serverData.executor.execute(reqHandler);//calling execute method of ExecutorService

        }
    }
}