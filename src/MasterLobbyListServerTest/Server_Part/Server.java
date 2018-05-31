package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class Server {
    //TODO remove comments?
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
    protected static int PING_REQ = 14;

    protected static int RESPONSE_CODE = 2;
    protected final static int ASSIGN_UNIQUE_USERNAME_RESP = 23;
    protected final static int PONG_RESP = 24;

    // 'HTTP style'
    protected final static int OK = 200;
    protected final static int BAD_REQUEST = 400;

    public static void main(String[] argv) throws InterruptedException, IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {

        ServerData serverData = new ServerData();
        System.out.println("ServerData is initialised");

        while (true) {

            //TODO: Re-evaluate req. and response codes - are they unnessacary as we have seperate spaces?

            // [0] request code,[1] request type, [2] if USER_NAME_REQ -> username to request/null, if CREATE_LOBBY_REQ -> name of lobby/username for lobby owner [3] the client's key
            int REQUEST_CODE = 1;
            Object[] tuple = serverData.requestSpace.get(new ActualField(REQUEST_CODE), new FormalField(Integer.class), new FormalField(SealedObject.class), new FormalField(SealedObject.class));

            SealedObject encryptedKey = (SealedObject) tuple[3];
            Key key = (Key) encryptedKey.getObject(serverData.cipher);

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            SealedObject encryptedUserName = (SealedObject) tuple[2];

            String decryptedInfo = (String) encryptedUserName.getObject(serverData.cipher);

            Runnable reqHandler = new RequestHandlerThread(serverData, tuple,cipher, decryptedInfo);
            serverData.requestExecutor.execute(reqHandler);//calling execute method of ExecutorService
        }
    }
}