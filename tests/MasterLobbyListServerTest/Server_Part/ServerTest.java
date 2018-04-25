package MasterLobbyListServerTest.Server_Part;

import MasterLobbyListServerTest.JavaFXClient.HelperFunctions;
import MasterLobbyListServerTest.JavaFXClient.Model;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

public class ServerTest {

    ServerData serverData;
    RequestHandlerThread reqHandler;
    Key clientKey;
    Cipher clientCipher;
    Cipher cipherForEncryptingServer;
    int REQUEST_CODE = 1;

    @Before
    public void initialize() throws InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        serverData = new ServerData("localhost");

        //Key for the client
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        clientKey = kg.generateKey();
        clientCipher = Cipher.getInstance("AES");
        clientCipher.init(Cipher.DECRYPT_MODE, clientKey);

        Object[] tuple = serverData.requestSpace.query(new FormalField(PublicKey.class));
        PublicKey key = (PublicKey) tuple[0];
        cipherForEncryptingServer = Cipher.getInstance("RSA");
        cipherForEncryptingServer.init(Cipher.ENCRYPT_MODE,key);
    }

    @Test
    public void createLobbyTest() throws InterruptedException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {

        SealedObject encryptedLobbyNameString = new SealedObject("TestLobby" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        Thread.sleep(1000);

        serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobby",tuple[1]);

        encryptedLobbyNameString = new SealedObject("TestLobbySillyName&%¤#" + "!" + "TestClient#123", cipherForEncryptingServer);
        encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        Thread.sleep(1000);

        tuple = serverData.lobbyOverviewSpace.getp(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertNull(tuple);

        tuple = serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1, Server.BAD_REQUEST);
        Assert.assertEquals(field2, "TestClient#123");
    }

    public void runRequestHandler() throws InterruptedException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException, IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Object[] tuple = serverData.requestSpace.get(new ActualField(REQUEST_CODE), new FormalField(Integer.class), new FormalField(SealedObject.class), new FormalField(SealedObject.class));

        SealedObject encryptedKeyReceived = (SealedObject) tuple[3];
        Key key = (Key) encryptedKeyReceived.getObject(serverData.cipher);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        SealedObject encryptedUserName = (SealedObject) tuple[2];
        String decryptedInfo = (String) encryptedUserName.getObject(serverData.cipher);

        reqHandler = new RequestHandlerThread(serverData, tuple,cipher, decryptedInfo);
        Thread thread = new Thread(reqHandler);
        thread.start();
    }

    @Test
    public void createUserTest() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        SealedObject encryptedUserNameString = new SealedObject("TestClient" + "!", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_USERNAME_REQ, encryptedUserNameString, encryptedKeySending);

        runRequestHandler();
        Thread.sleep(1000);

        Object[] tuple = serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE),new ActualField(Server.ASSIGN_UNIQUE_USERNAME_RESP), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1,Server.OK);
        Assert.assertEquals(field2,"TestClient");

        encryptedUserNameString = new SealedObject("TestClient#¤%&" + "!", cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_USERNAME_REQ, encryptedUserNameString, encryptedKeySending);

        runRequestHandler();
        Thread.sleep(1000);

        tuple = serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE),new ActualField(Server.ASSIGN_UNIQUE_USERNAME_RESP), new FormalField(SealedObject.class));

        decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);
        field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1,Server.BAD_REQUEST);
        Assert.assertEquals(field2,"TestClient#¤%&");
    }

    @Test
    public void createMoreLobbiesThanAllowed(){}

    @Test
    public void connectionToLobby() {}

    @Test
    public void disconnectionFromLobby(){}

    @Test
    public void lobbyLeaderDisconnectionFromLobby(){}

    @Test
    public void chatInLobby(){}

    @Test
    public void beginWithoutEnoughPlayers(){}

    @Test
    public void beginGame(){}

    @Test
    public void countessRule(){}

    @Test
    public void invalidCardIndex(){}

    @Test
    public void invalidTargetIndex(){}

    @Test
    public void invalidGuess(){}

    @Test
    public void winARound(){}

    @Test
    public void winTheGame(){}
}
