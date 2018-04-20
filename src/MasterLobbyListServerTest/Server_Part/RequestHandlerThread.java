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
    private Cipher Clientcipher;
    private String decryptedInfo;

    RequestHandlerThread(ServerData serverData, Object[] tuple, Cipher Clientcipher, String decryptedInfo){
        this.serverData = serverData;
        this.tuple = tuple;
        this.Clientcipher = Clientcipher;
        this.decryptedInfo = decryptedInfo;
    }

    @Override
    public void run() {
        
        Cipher serverCipher = serverData.cipher;

        try {

            if ((int) tuple[1] == Server.CREATE_LOBBY_REQ) {

                String serverName = decryptedInfo.substring(0, decryptedInfo.indexOf('!'));

                String user = decryptedInfo.substring(decryptedInfo.indexOf('!') + 1, decryptedInfo.length());

                if(!(serverData.getCurrentNoThreads() < ServerData.MAXIMUM_LOBBIES)){
                    UUID idForLobby = UUID.randomUUID();
                    try{
                        SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + user + "?" + idForLobby, Clientcipher);
                        serverData.responseSpace.put(Server.RESPONSE_CODE, encryptedMessage);
                        System.out.println("Putted the BAD_REQ tuple");
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

                    SealedObject encryptedMessage = new SealedObject(Server.OK + "!" + user + "?" + idForLobby, Clientcipher);

                    serverData.responseSpace.put(Server.RESPONSE_CODE, encryptedMessage);
                    //Add Server information to entrySpace
                    serverData.lobbyOverviewSpace.put("Lobby", serverName, idForLobby);

                } else {
                    UUID idForLobby = UUID.randomUUID();
                    SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + user + "?" + idForLobby, Clientcipher);

                    serverData.responseSpace.put(Server.RESPONSE_CODE, encryptedMessage);
                }
            } else if ((int) tuple[1] == Server.CREATE_USERNAME_REQ) {

                String userName = decryptedInfo.substring(0, decryptedInfo.indexOf('!'));

                if (validName(userName)) {
                    String uniqueName = uniqueUserName(userName);
                    SealedObject encryptedMessage = new SealedObject(Server.OK + "!" + userName + "?" + uniqueName, Clientcipher);

                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.ASSIGN_UNIQUE_USERNAME_RESP, encryptedMessage);

                } else {

                    SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + userName + "?",Clientcipher);
                    serverData.responseSpace.put(Server.RESPONSE_CODE, Server.ASSIGN_UNIQUE_USERNAME_RESP, encryptedMessage);
                }
            } else {
                System.out.println("Too many lobbies at once \n Deny request");
            }


        } catch ( InterruptedException | IOException | IllegalBlockSizeException e){
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
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
