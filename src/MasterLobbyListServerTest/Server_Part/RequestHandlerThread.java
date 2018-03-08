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

            System.out.println("Creating a lobby with the name : " + tuple[2] + "\n");

            String serverName = (String) tuple[2];
            String user = (String) tuple[3];
            if(validName(serverName)) {


                //Add Thread to lobbyThreads

                UUID idForLobby = UUID.randomUUID();

                Runnable lobby = new Lobby(idForLobby, serverData.lobbyOverviewSpace, serverData.serverRepos, (String) tuple[3]);
                serverData.executor.execute(lobby);  //calling execute method of ExecutorService

                try {
                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.OK, user, idForLobby);
                    //Add Server information to entrySpace
                    serverData.lobbyOverviewSpace.put("Lobby", serverName, idForLobby);

                } catch (InterruptedException e) {
                    System.out.println("Error ");
                }

                System.out.println("LobbyRequest has now been handled");
            } else {
                UUID idForLobby = UUID.randomUUID();
                try{
                    serverData.responseSpace.put(Server.RESPONSE_CODE,Server.BAD_REQUEST, user, idForLobby);
                } catch (InterruptedException e){
                    System.out.println("Error");
                }

            }

        } else if ((int) tuple[1] == Server.CREATE_USERNAME_REQ) {

            String userName = (String) tuple[2];

            if(validName(userName)) {
                String uniqueName = uniqueUserName(userName);
                try {
                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.ASSIGN_UNIQUE_USERNAME_RESP, Server.OK, userName, uniqueName);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.ASSIGN_UNIQUE_USERNAME_RESP, Server.BAD_REQUEST, userName, "");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("Too many lobbies at once \n Deny request");
        }
    }

    public boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]+");
    }

    public String uniqueUserName(String name){
        String id = "#" + UUID.randomUUID().toString();
        return name+id;
    }


}
