package MasterLobbyListServerTest.Server_Part;

import MasterLobbyListServerTest.JavaFXClient.HelperFunctions;
import MasterLobbyListServerTest.JavaFXClient.Model;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.junit.*;

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

    @After
    public void shutDown(){
        serverData.closeDownServer();
    }

    @Test
    public void createLobbyTest() throws InterruptedException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {

        SealedObject encryptedLobbyNameString = new SealedObject("TestLobby" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobby",tuple[1]);

        encryptedLobbyNameString = new SealedObject("TestLobbySillyName&%¤#" + "!" + "TestClient#123", cipherForEncryptingServer);
        encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

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

        tuple = serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE),new ActualField(Server.ASSIGN_UNIQUE_USERNAME_RESP), new FormalField(SealedObject.class));

        decryptedMessage = (String) ((SealedObject) tuple[2 ]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);
        field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1,Server.BAD_REQUEST);
        Assert.assertEquals(field2,"TestClient#¤%&");
    }

    @Test
    public void createMoreLobbiesThanAllowed() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        for (int i = 0; i < serverData.MAXIMUM_LOBBIES;i++) {
            SealedObject encryptedLobbyNameString = new SealedObject("TestLobby" + i + "!" + "TestClient#123", cipherForEncryptingServer);
            SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

            serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

            runRequestHandler();

            serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
            Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
            Assert.assertEquals("TestLobby" + i, tuple[1]);
        }

        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyOverMax" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        Object[] tuple = serverData.lobbyOverviewSpace.getp(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertNull(tuple);

        tuple = serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1, Server.BAD_REQUEST);
        Assert.assertEquals(field2, "TestClient#123");

    }

    @Test
    public void connectionToLobby() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobbyCon", tuple[1]);

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.LOBBY_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple2[2]).getObject(clientCipher);

        Assert.assertEquals("TestClient#123",decryptedMessage.substring(0,decryptedMessage.indexOf('!')));

        Assert.assertEquals(Lobby.CONNECT_ACCEPTED,tuple2[1]);
    }

    @Test
    public void disconnectionFromLobby() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.LOBBY_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String messageToBeEncrypted2 = "" + Lobby.DISCONNECT + "!TestClient2#123?" + -1;
        SealedObject encryptedMessage2 = new SealedObject(messageToBeEncrypted2, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage2, filler);

        Thread.sleep(2000);

        Object[] lobby = serverData.lobbyOverviewSpace.queryp(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        Assert.assertEquals("TestLobbyCon", lobby[1]);
    }

    @Test
    public void lobbyLeaderDisconnectionFromLobby() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.LOBBY_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String messageToBeEncrypted2 = "" + Lobby.DISCONNECT + "!TestClient#123?" + -1;
        SealedObject encryptedMessage2 = new SealedObject(messageToBeEncrypted2, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage2, filler);

        Thread.sleep(2000);

        Object[] lobby = serverData.lobbyOverviewSpace.queryp(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        Assert.assertNull(lobby);
    }

    @Test
    public void chatInLobby() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.LOBBY_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String textToSend = "TestClient : test";

        lobbySpace.put("Chat", textToSend);

        Object[] message = lobbySpace.get(new ActualField(Lobby.LOBBY_UPDATE), new ActualField(Lobby.CHAT_MESSAGE), new FormalField(String.class), new FormalField(Integer.class), new FormalField(Integer.class));

        Assert.assertEquals(message[2],textToSend);
        //(Lobby.LOBBY_UPDATE, Lobby.CHAT_MESSAGE, field1, user.threadNr, user.userNr);
    }

    @Test
    public void beginWithoutEnoughPlayers() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.RESPONSE_CODE), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.LOBBY_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1;
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Lobby.LOBBY_REQ, encryptedMessage, filler);

        tuple = lobbySpace.get(new ActualField(Lobby.LOBBY_UPDATE), new FormalField(Integer.class), new FormalField(String.class),new ActualField(0), new ActualField(0));

        Assert.assertEquals(Lobby.NOT_ENOUGH_PLAYERS, tuple[1]);
    }

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
