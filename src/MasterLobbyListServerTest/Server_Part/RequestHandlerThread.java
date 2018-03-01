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
        if ((int) tuple[1] == Server.CREATE_LOBBY_REQ) {

            System.out.println("Trying to create a lobby with the name : " + tuple[2] + "\n");

            //Add Thread to lobbyThreads

            UUID idForLobby = UUID.randomUUID();

            Runnable lobby = new Lobby(idForLobby,serverData.lobbyOverviewSpace,serverData.serverRepos,(String)tuple[3]);
            serverData.executor.execute(lobby);  //calling execute method of ExecutorService

            try {
                serverData.responseSpace.put(Server.RESPONSE_CODE, tuple[3], idForLobby);
                //Add Server information to entrySpace
                serverData.lobbyOverviewSpace.put("Lobby", tuple[2], idForLobby);

            } catch (InterruptedException e){
                System.out.println("Error ");
            }

            System.out.println("LobbyRequest has now been handled");

        } else if ((int) tuple[1] == Server.CREATE_USERNAME_REQ) {

            String userName = (String) tuple[2];

            if(validName(userName)) {
                String uniqueName = uniqueUserName(userName);
                try {
                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.CREATE_UNIQUE_USERNAME, userName, uniqueName);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else if ((int) tuple[1] == Server.JOIN_LOBBY_REQ) {

            // PSEUDO
            // if (lobby exists AND not in game) { join(); }
            // else { lobby unavailable respond }

        } else {
            System.out.println("Too many lobbies at once \n Deny request");
        }
    }

    public boolean validName(String name){
        return name.matches("[a-zA-Z]+");
    }

    public String uniqueUserName(String name){
        String id = "#" + UUID.randomUUID().toString();
        return name+id;
    }


}
