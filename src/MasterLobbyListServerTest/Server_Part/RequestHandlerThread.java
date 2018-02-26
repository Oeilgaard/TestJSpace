package MasterLobbyListServerTest.Server_Part;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestHandlerThread implements Runnable {

    Object[] tuple;
    ServerData serverData;

    public RequestHandlerThread(ServerData serverData, Object[] tuple){
        this.serverData = serverData;
        this.tuple = tuple;
    }

    @Override
    public void run() {
        if ((int) tuple[1] == Server.CREATE_LOBBY_REQ){
            System.out.println("Trying to create a lobby with the name : " + tuple[2] + "\n");

            //System.out.println("(Add Thread to lobbyThreads)");

            UUID idForLobby = UUID.randomUUID();

            Runnable lobby = new Lobby(idForLobby,serverData.requestSpace,serverData.serverRepos);
            serverData.executor.execute(lobby);//calling execute method of ExecutorService

            //System.out.println("(Put Tuple with information about the lobbyID to the user)");

            serverData.requestSpace.put("Response",tuple[3],idForLobby);

            //System.out.println("(Add Server information to entrySpace)");

            serverData.lobbyOverviewSpace.put("Lobby", tuple[2], idForLobby);

            System.out.println("LobbyRequest has now been handled");
        } else {
            System.out.println("Too many lobbies at once \n Deny request");
        }
    }


}
