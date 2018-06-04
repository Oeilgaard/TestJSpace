package main_system_files.server;

import org.jspace.ActualField;
import org.jspace.FormalField;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class Server {

    protected static int CREATE_REQUEST = 1;
    protected static int CREATE_LOBBY_REQ = 11;
    protected static int CREATE_USERNAME_REQ = 12;
    protected static int JOIN_LOBBY_REQ = 13;
    protected static int PING_REQ = 14;
    protected static int LOBBY_INFO = 90;

    protected static int S2C_CREATE_RESP = 2;
    protected final static int CREATE_USERID_RESP = 23;
    protected final static int PONG_RESP = 24;
    protected final static int CREATE_LOBBY_RESP = 25;

    // 'HTTP style'
    protected final static int OK = 200;
    protected final static int BAD_REQUEST = 400;

    public static void main(String[] argv) throws InterruptedException, IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, ClassNotFoundException {

        ServerData serverData = new ServerData();
        System.out.println("ServerData is initialised");

        while (true) {

            // [0] request code,[1] request type, [2] if USER_NAME_REQ -> username to request/null, if CREATE_LOBBY_REQ -> name of lobby/username for lobby owner [3] the client's key

            Object[] tuple = serverData.requestSpace.get(new ActualField(CREATE_REQUEST), new FormalField(Integer.class), new FormalField(SealedObject.class), new FormalField(SealedObject.class));

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