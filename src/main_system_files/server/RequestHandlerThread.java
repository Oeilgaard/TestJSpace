package main_system_files.server;

import main_system_files.client.HelperFunctions;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class RequestHandlerThread implements Runnable {

    private Object[] tuple;
    private ServerData serverData;
    private Cipher clientCipher;
    private String decryptedInfo;

    RequestHandlerThread(ServerData serverData, Object[] tuple, Cipher clientCipher, String decryptedInfo){
        this.serverData = serverData;
        this.tuple = tuple;
        this.clientCipher = clientCipher;
        this.decryptedInfo = decryptedInfo;
    }

    @Override
    public void run() {

        //Cipher serverCipher = serverData.cipher;
        try {
            if ((int) tuple[1] == Server.CREATE_LOBBY_REQ) {

                String serverName = decryptedInfo.substring(0, decryptedInfo.indexOf('!'));

                String user = decryptedInfo.substring(decryptedInfo.indexOf('!') + 1, decryptedInfo.length());

                if(!(serverData.currentNumberOfActiveThreads() < ServerData.MAXIMUM_LOBBIES)) {
                    try{
                        SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + user + "?", clientCipher);
                        serverData.responseSpace.put(Server.S2C_CREATE_RESP, Server.CREATE_LOBBY_RESP, encryptedMessage);
                        System.out.println("Putted the BAD_REQ tuple");
                    } catch (InterruptedException e){
                        System.out.println("Error");
                    }
                    return;
                }

                System.out.println("valid server name: " + HelperFunctions.validName(serverName));

                if(HelperFunctions.validName(serverName)) {

                    //Add Thread to lobbyThreads

                    UUID idForLobby = UUID.randomUUID();

                    System.out.println("Creating a lobby with the name : " + serverName + "\n");

                    //Add Thread to lobbyThreads
                    serverData.createNewLobbyThread(idForLobby, user, serverData);

                    SealedObject encryptedMessage = new SealedObject(Server.OK + "!" + user + "?" + idForLobby, clientCipher);

                    serverData.responseSpace.put(Server.S2C_CREATE_RESP, Server.CREATE_LOBBY_RESP, encryptedMessage);

                    //Add Server information to lobbyOverviewSpace
                    serverData.lobbyOverviewSpace.put(Server.LOBBY_INFO, serverName, idForLobby);

                } else {
                    SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + user + "?", clientCipher);
                    //SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + user + "?" + idForLobby, cipher);

                    serverData.responseSpace.put(Server.S2C_CREATE_RESP, Server.CREATE_LOBBY_RESP, encryptedMessage);
                }
            } else if ((int) tuple[1] == Server.CREATE_USERNAME_REQ) {

                String userName = decryptedInfo.substring(0, decryptedInfo.indexOf('!'));

                if (HelperFunctions.validName(userName)) {
                    String uniqueName = assignUserID(userName);
                    SealedObject encryptedMessage = new SealedObject(Server.OK + "!" + userName + "?" + uniqueName, clientCipher);

                    serverData.responseSpace.put(Server.S2C_CREATE_RESP, Server.CREATE_USERID_RESP, encryptedMessage);

                } else {

                    SealedObject encryptedMessage = new SealedObject(Server.BAD_REQUEST + "!" + userName + "?", clientCipher);
                    serverData.responseSpace.put(Server.S2C_CREATE_RESP, Server.CREATE_USERID_RESP, encryptedMessage);
                }
            } else if ((int) tuple[1] == Server.PING_REQ) {
                serverData.responseSpace.put(Server.S2C_CREATE_RESP, Server.PONG_RESP);
            } else {
                System.out.println("Too many lobbies at once \n Deny request");
            }


        } catch ( InterruptedException | IOException | IllegalBlockSizeException | NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        System.out.println("Req. Thread is done ");
    }

    private String assignUserID(String name){
        String id = "#" + UUID.randomUUID().toString();
        System.out.println(name+id);
        return name+id;
    }
}
