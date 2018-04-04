package MasterLobbyListServerTest.Server_Part;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.UUID;

public class RequestHandlerThread implements Runnable {

    private Object[] tuple;
    private ServerData serverData;

    RequestHandlerThread(ServerData serverData, Object[] tuple){
        this.serverData = serverData;
        this.tuple = tuple;
    }

    @Override
    public void run() {
        if ((int) tuple[1] == Server.CREATE_LOBBY_REQ) {

            SealedObject encryptedLobbyName = (SealedObject) tuple[2];
            //String serverName = (String) tuple[2];
            String deCryptedMessage = null;
            try {
                deCryptedMessage = (String) encryptedLobbyName.getObject(serverData.cipher);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }

            String serverName = deCryptedMessage.substring(0,deCryptedMessage.indexOf('!'));

            System.out.println("Creating a lobby with the name : " + serverName + "\n");

            String user = deCryptedMessage.substring(deCryptedMessage.indexOf('!')+1,deCryptedMessage.length());
            if(validName(serverName)) {


                //Add Thread to lobbyThreads

                UUID idForLobby = UUID.randomUUID();

                Runnable lobby = new Lobby(idForLobby, serverData.lobbyOverviewSpace, serverData.serverRepos, user, serverData);
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

            SealedObject encryptedUserName = (SealedObject) tuple[2];

            //String userName = (String) tuple[2];
            String decryptedUsername = null;
            try {
                decryptedUsername = (String) encryptedUserName.getObject(serverData.cipher);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }

            String userName = decryptedUsername.substring(0,decryptedUsername.indexOf('!'));

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

    private boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]+");
    }

    private String uniqueUserName(String name){
        String id = "#" + UUID.randomUUID().toString();
        return name+id;
    }


}
