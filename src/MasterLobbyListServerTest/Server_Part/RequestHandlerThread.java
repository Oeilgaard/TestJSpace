package MasterLobbyListServerTest.Server_Part;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
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

        try {
            SealedObject encryptedKey = (SealedObject) tuple[3];
            Key key = (Key) encryptedKey.getObject(serverData.cipher);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            if ((int) tuple[1] == Server.CREATE_LOBBY_REQ) {

                SealedObject encryptedLobbyName = (SealedObject) tuple[2];
                //String serverName = (String) tuple[2];
                String deCryptedMessage = (String) encryptedLobbyName.getObject(serverData.cipher);

                String serverName = deCryptedMessage.substring(0, deCryptedMessage.indexOf('!'));

                System.out.println("Creating a lobby with the name : " + serverName + "\n");

                String user = deCryptedMessage.substring(deCryptedMessage.indexOf('!') + 1, deCryptedMessage.length());

                if(!(serverData.getCurrentNoThreads() < ServerData.MAXIMUM_LOBBIES)){
                    UUID idForLobby = UUID.randomUUID();
                    try{
                        SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + user + "?" + idForLobby, cipher);
                        serverData.responseSpace.put(Server.RESPONSE_CODE, encryptedMessage);
                        System.out.println("Putted the BAD_REQ");
                    } catch (InterruptedException e){
                        System.out.println("Error");
                    }
                    return;
                }

                if(validName(serverName)) {

                    //Add Thread to lobbyThreads

                    UUID idForLobby = UUID.randomUUID();

                    System.out.println("Creating a lobby with the name : " + serverName + "\n");


                    //Add Thread to lobbyThreads
                    serverData.createNewLobbyThread(idForLobby, user, serverData);

                    SealedObject encryptedMessage = new SealedObject(Server.OK + "!" + user + "?" + idForLobby, cipher);

                    serverData.responseSpace.put(Server.RESPONSE_CODE, encryptedMessage);
                    //Add Server information to entrySpace
                    serverData.lobbyOverviewSpace.put("Lobby", serverName, idForLobby);

                    System.out.println("LobbyRequest has now been handled");
                } else {
                    UUID idForLobby = UUID.randomUUID();
                    SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + user + "?" + idForLobby, cipher);

                    serverData.responseSpace.put(Server.RESPONSE_CODE, encryptedMessage);
                }
            } else if ((int) tuple[1] == Server.CREATE_USERNAME_REQ) {

                SealedObject encryptedUserName = (SealedObject) tuple[2];

                //String userName = (String) tuple[2];
                String decryptedUsername = (String) encryptedUserName.getObject(serverData.cipher);

                String userName = decryptedUsername.substring(0, decryptedUsername.indexOf('!'));

                if (validName(userName)) {
                    String uniqueName = uniqueUserName(userName);
                    SealedObject encryptedMessage = new SealedObject(Server.OK + "!" + userName + "?" + uniqueName, cipher);

                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.ASSIGN_UNIQUE_USERNAME_RESP, encryptedMessage);

                } else {

                    SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + userName + "?",cipher);
                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.ASSIGN_UNIQUE_USERNAME_RESP, encryptedMessage);
                }
            } else {
                System.out.println("Too many lobbies at once \n Deny request");
            }


        } catch ( InterruptedException | IOException | IllegalBlockSizeException | ClassNotFoundException | BadPaddingException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        System.out.println("Req. Thread is done ");
    }

    private boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]+");
    }

    private String uniqueUserName(String name){
        String id = "#" + UUID.randomUUID().toString();
        System.out.println(name+id);
        return name+id;
    }


}
