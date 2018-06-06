package main_system_files.server;


import main_system_files.server.gameplay.Card;
import main_system_files.server.gameplay.Model;
import main_system_files.server.gameplay.Player;
import main_system_files.server.gameplay.Role;
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
import java.util.ArrayList;
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

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobby",tuple[1]);

        encryptedLobbyNameString = new SealedObject("TestLobbySillyName&%#" + "!" + "TestClient#123", cipherForEncryptingServer);
        encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        tuple = serverData.lobbyOverviewSpace.getp(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertNull(tuple);

        tuple = serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1, Server.BAD_REQUEST);
        Assert.assertEquals(field2, "TestClient#123");
    }

    @Test
    public void createSeveralLobbiesTest() throws InterruptedException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException, IOException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {

        SealedObject encryptedLobbyNameString = new SealedObject("TestLobby" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobby",tuple[1]);

        Assert.assertEquals(serverData.lobbyMap.get(tuple[2]).lobbyLeader, "TestClient#123");

        encryptedLobbyNameString = new SealedObject("TestLobby2" + "!" + "TestClient#123", cipherForEncryptingServer);
        encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        tuple = serverData.lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobby2",tuple[1]);

        Assert.assertEquals(serverData.lobbyMap.get(tuple[2]).lobbyLeader, "TestClient#123");

        encryptedLobbyNameString = new SealedObject("TestLobby3" + "!" + "TestClient#123", cipherForEncryptingServer);
        encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        tuple = serverData.lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobby3",tuple[1]);

        Assert.assertEquals(serverData.lobbyMap.get(tuple[2]).lobbyLeader, "TestClient#123");


    }

    @Test
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

        Object[] tuple = serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP),new ActualField(Server.CREATE_USERID_RESP), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1,Server.OK);
        Assert.assertEquals(field2,"TestClient");

        encryptedUserNameString = new SealedObject("TestClient#¤%&" + "!", cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_USERNAME_REQ, encryptedUserNameString, encryptedKeySending);

        runRequestHandler();

        tuple = serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP),new ActualField(Server.CREATE_USERID_RESP), new FormalField(SealedObject.class));

        decryptedMessage = (String) ((SealedObject) tuple[2 ]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);
        field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1,Server.BAD_REQUEST);
        Assert.assertEquals(field2,"TestClient#¤%&");
    }

    @Test
    public void createMoreLobbiesThanAllowedTest() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        for (int i = 0; i < serverData.MAXIMUM_LOBBIES;i++) {
            SealedObject encryptedLobbyNameString = new SealedObject("TestLobby" + i + "!" + "TestClient#123", cipherForEncryptingServer);
            SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

            serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

            runRequestHandler();

            serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
            Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
            Assert.assertEquals("TestLobby" + i, tuple[1]);
        }

        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyOverMax" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        Object[] tuple = serverData.lobbyOverviewSpace.getp(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertNull(tuple);

        tuple = serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf("!")+1,decryptedMessage.indexOf("?"));

        Assert.assertEquals(field1, Server.BAD_REQUEST);
        Assert.assertEquals(field2, "TestClient#123");

    }

    @Test
    public void connectionToLobbyTest() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobbyCon", tuple[1]);

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple2[2]).getObject(clientCipher);

        Assert.assertEquals("TestClient#123",decryptedMessage.substring(0,decryptedMessage.indexOf('!')));

        Assert.assertEquals(Lobby.CONNECT_ACCEPTED,tuple2[1]);
    }

    @Test
    public void tooManyConnectionsToLobbyTest() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient1#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.get(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
        Assert.assertEquals("TestLobbyCon", tuple[1]);

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        for(int i = 1; i < 5;i++) {

            String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient" + i +"#123?0=";

            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

            lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

            Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

            String decryptedMessage = (String) ((SealedObject) tuple2[2]).getObject(clientCipher);

            Assert.assertEquals("TestClient" + i + "#123", decryptedMessage.substring(0, decryptedMessage.indexOf('!')));

            Assert.assertEquals(Lobby.CONNECT_ACCEPTED, tuple2[1]);
        }

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient5#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String decryptedMessage = (String) ((SealedObject) tuple2[2]).getObject(clientCipher);

        Assert.assertEquals("TestClient5#123", decryptedMessage.substring(0, decryptedMessage.indexOf('!')));

        Assert.assertEquals(Lobby.CONNECT_DENIED, tuple2[1]);


    }

    @Test
    public void disconnectionFromLobbyTest() throws IOException, IllegalBlockSizeException, InterruptedException, ClassNotFoundException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient1#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient1#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String messageToBeEncrypted2 = "" + Lobby.DISCONNECT + "!TestClient2#123?" + -1 + "=";
        SealedObject encryptedMessage2 = new SealedObject(messageToBeEncrypted2, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage2, filler);

        Thread.sleep(2000);

        Object[] lobby = serverData.lobbyOverviewSpace.queryp(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        Assert.assertEquals("TestLobbyCon", lobby[1]);

        messageToBeEncrypted = "" + Lobby.GET_PLAYERLIST + "!TestClient1#123?" + -1 + "=";

        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);
        filler = new SealedObject("filler",lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        messageToBeEncrypted2 = "" + Lobby.DISCONNECT + "!TestClient1#123?" + -1 + "=";
        encryptedMessage2 = new SealedObject(messageToBeEncrypted2, lobbyCipher);
        filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage2, filler);

        Thread.sleep(2000);

        lobby = serverData.lobbyOverviewSpace.queryp(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        Assert.assertNull(lobby);
    }

    @Test
    public void lobbyLeaderDisconnectionFromLobbyTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String messageToBeEncrypted2 = "" + Lobby.DISCONNECT + "!TestClient#123?" + -1 + "=";
        SealedObject encryptedMessage2 = new SealedObject(messageToBeEncrypted2, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage2, filler);

        Thread.sleep(2000);

        Object[] lobby = serverData.lobbyOverviewSpace.queryp(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        Assert.assertNull(lobby);
    }

    @Test
    public void chatInLobbyTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        String textToSend = "TestClient : test";

        lobbySpace.put("Chat", textToSend);

        Object[] message = lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.CHAT_MESSAGE), new FormalField(String.class), new FormalField(Integer.class), new FormalField(Integer.class));

        Assert.assertEquals(message[2],textToSend);
        //(Model.C2S_LOBBY_GAME, Lobby.CHAT_MESSAGE, field1, user.threadNr, user.userNr);
    }

    @Test
    public void beginWithoutEnoughPlayersTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));
        
        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=" + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        tuple = lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new FormalField(Integer.class), new FormalField(String.class),new ActualField(0), new ActualField(0));

        Assert.assertEquals(Lobby.NOT_ENOUGH_PLAYERS, tuple[1]);
    }

    @Test
    public void beginGameTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Assert.assertTrue(serverData.lobbyMap.get(tuple[2]).gameBegun());
    }

    @Test
    public void countessRuleTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        System.out.println("testen : " + serverData.lobbyMap.get(tuple[2]).game);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.COUNTESS);
        players.get(0).getHand().setCards(1,Role.KING);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 1 + "=" + 1 + "*¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.ACTION_DENIED);
        Assert.assertEquals(field2, "Countess rule is in play");

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);
        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void invalidCardIndexTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        System.out.println("testen : " + serverData.lobbyMap.get(tuple[2]).game);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.HANDMAID);
        players.get(0).getHand().setCards(1,Role.KING);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 2 + "=" + 1 + "*¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.ACTION_DENIED);
        Assert.assertEquals(field2, "Card index is unvalid.");
    }

    @Test
     public void invalidTargetIndexTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        System.out.println("testen : " + serverData.lobbyMap.get(tuple[2]).game);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.HANDMAID);
        players.get(0).getHand().setCards(1,Role.KING);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 1 + "=" + 2 + "*¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.ACTION_DENIED);
        Assert.assertEquals(field2, "Target is invalid.");
    }

    @Test
    public void invalidGuessTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        System.out.println("testen : " + serverData.lobbyMap.get(tuple[2]).game);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.GUARD);
        players.get(0).getHand().setCards(1,Role.KING);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.ACTION_DENIED);
        Assert.assertEquals(field2, "Invalid guard guess.");
    }

    @Test
    public void playHandmaidTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.HANDMAID);
        players.get(0).getHand().setCards(1,Role.KING);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void playGuardTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.GUARD);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.GUARD);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*1¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void playPriestTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.PRIEST);
        players.get(0).getHand().setCards(1,Role.KING);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void playBaronTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.BARON);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.KING);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));
        System.out.println("Nåede her til");

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void playPrinceTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.PRINCE);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.KING);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void playKingTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.KING);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.KING);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void winARoundTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.BARON);
        players.get(0).getHand().setCards(1,Role.PRINCESS);

        players.get(1).getHand().setCards(0,Role.GUARD);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.KNOCK_OUT);

        Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.OUTCOME);

        winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.WIN);
    }

    @Test
    public void winTheGameTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/" + tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE, lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        for(int i = 0;i < 7;i++){
            players.get(0).getHand().setCards(0, Role.BARON);
            players.get(0).getHand().setCards(1, Role.PRINCESS);

            players.get(1).getHand().setCards(0, Role.GUARD);

            messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
            encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

            Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

            String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
            int field1 = Integer.parseInt(field1text);

            Assert.assertEquals(field1, Model.KNOCK_OUT);

            Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

            field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
            field1 = Integer.parseInt(field1text);

            Assert.assertEquals(field1, Model.OUTCOME);

            winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

            field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
            field1 = Integer.parseInt(field1text);

            Assert.assertEquals(field1, Model.WIN);
            Thread.sleep(200);

        }
        Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.GAME_ENDING);

        Thread.sleep(30000);

        Assert.assertTrue(serverData.lobbyMap.isEmpty());
    }

    @Test
    public void noCardsLeftInDeckTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient0#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/" + tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE, lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient0#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient1#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient0#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        serverData.lobbyMap.get(tuple[2]).game.model.secretCard = new Card(Role.PRINCESS);

        for(int i = 0;i < 10;i++){
            players.get(i%2).getHand().setCards(0, Role.HANDMAID);

            messageToBeEncrypted = "" + Model.DISCARD + "!TestClient" + i%2 + "#123?" + 0 + "=" + 1 + "*0¤";
            encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

            Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            String decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

            String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
            int field1 = Integer.parseInt(field1text);

            Assert.assertEquals(field1, Model.OUTCOME);

            Thread.sleep(200);
        }
        Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.WIN);
    }

    @Test
    public void noCardsLeftTieTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient0#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/" + tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE, lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient0#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient1#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient0#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        serverData.lobbyMap.get(tuple[2]).game.model.secretCard = new Card(Role.PRINCESS);

        for(int i = 0;i < 9;i++){
            players.get(i%2).getHand().setCards(0, Role.HANDMAID);

            messageToBeEncrypted = "" + Model.DISCARD + "!TestClient" + i%2 + "#123?" + 0 + "=" + 1 + "*0¤";
            encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(i%2));

            lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

            Thread.sleep(200);
        }
        players.get(1).getHand().setCards(0, Role.COUNTESS);

        players.get(0).getHand().setCards(0,Role.HANDMAID);
        players.get(1).getHand().setCards(1,Role.HANDMAID);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient" + 1 + "#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(1));
        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.OUTCOME);

        Thread.sleep(200);

        winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.WIN);
    }

    @Test
    public void tieDiscardPileAndCardTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient0#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/" + tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE, lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient0#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient1#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient0#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class), new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        serverData.lobbyMap.get(tuple[2]).game.model.secretCard = new Card(Role.HANDMAID);

        for(int i = 0;i < 10;i++){
            players.get(i%2).getHand().setCards(0, Role.HANDMAID);

            if(i == 9){
                players.get(0).getHand().setCards(0, Role.HANDMAID);
                players.get(1).getHand().setCards(1, Role.HANDMAID);
                lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));
            }

            messageToBeEncrypted = "" + Model.DISCARD + "!TestClient" + i%2 + "#123?" + 0 + "=" + 1 + "*0¤";
            encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(i%2));

            lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

            Thread.sleep(200);
        }
        Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.OUTCOME);

        winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

        field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.WIN);
    }

    @Test
    public void disconnectIngameTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Assert.assertTrue(serverData.lobbyMap.get(tuple[2]).gameBegun());

        Thread.sleep(3000);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(1));

        messageToBeEncrypted = "" + Lobby.DISCONNECT + "!TestClient#123?0=*¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        System.out.println("Her");
        Object[] tuples = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(1));
        System.out.println("der");
        String decryptedMessage = (String) ((SealedObject) tuples[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);

        Assert.assertEquals(field1, Model.GAME_DISCONNECT);

    }

    @Test
    public void guardGuessRightTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Thread.sleep(1000);

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.GUARD);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.PRINCESS);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*7¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.KNOCK_OUT);
    }

    @Test
    public void baronlooseDuelTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.BARON);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.PRINCESS);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));
        System.out.println("Nåede her til");

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.KNOCK_OUT);
    }

    @Test
    public void princeOnSelfTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.PRINCE);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.KING);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 0 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void princeOnSelfWithPrincessTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.PRINCE);
        players.get(0).getHand().setCards(1,Role.PRINCESS);

        players.get(1).getHand().setCards(0,Role.KING);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 0 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.KNOCK_OUT);
    }

    @Test
    public void princeOnOtherWithPrincessTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.PRINCE);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.PRINCESS);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.KNOCK_OUT);
    }

    @Test
    public void princeIntoSecretCardTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient0#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient0#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient1#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient0#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        for(int i = 0;i < 9;i++){
            players.get(0).getHand().setCards(0, Role.PRIEST);
            players.get(1).getHand().setCards(0, Role.PRIEST);


            messageToBeEncrypted = "" + Model.DISCARD + "!TestClient" + i%2 + "#123?" + 0 + "=" + ((i%2)+1)%2 + "*0¤";
            encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

            Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            String decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

            String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
            int field1 = Integer.parseInt(field1text);

            Assert.assertEquals(field1, Model.OUTCOME);

            Thread.sleep(200);
        }

        players.get(1).getHand().setCards(0,Role.PRINCE);
        players.get(1).getHand().setCards(1,Role.KING);

        players.get(0).getHand().setCards(0,Role.KING);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient1#123?" + 0 + "=" + 0 + "*0¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void noActionCardTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient0#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient0#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient1#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient0#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        for(int i = 0;i < 9;i++){
            players.get(0).getHand().setCards(0, Role.HANDMAID);
            players.get(1).getHand().setCards(0, Role.HANDMAID);


            messageToBeEncrypted = "" + Model.DISCARD + "!TestClient" + i%2 + "#123?" + 0 + "=" + ((i%2)+1)%2 + "*0¤";
            encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

            lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

            Object[] winTuple = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

            String decryptedMessage = (String) ((SealedObject) winTuple[1]).getObject(clientCipher);

            String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
            int field1 = Integer.parseInt(field1text);

            Assert.assertEquals(field1, Model.OUTCOME);

            Thread.sleep(200);
        }

        players.get(1).getHand().setCards(0,Role.PRINCE);
        players.get(1).getHand().setCards(1,Role.KING);

        players.get(0).getHand().setCards(0,Role.KING);


        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient1#123?" + 1 + "=0*¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.OUTCOME);
    }

    @Test
    public void playPrincessTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        ArrayList<Player> players = serverData.lobbyMap.get(tuple[2]).game.model.getPlayers();

        players.get(0).getHand().setCards(0,Role.PRINCESS);
        players.get(0).getHand().setCards(1,Role.KING);

        players.get(1).getHand().setCards(0,Role.GUARD);

        messageToBeEncrypted = "" + Model.DISCARD + "!TestClient#123?" + 0 + "=" + 1 + "*1¤";
        encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

        lobbySpace.getAll(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler); // Send the action to the server

        Object[] countessrule = lobbySpace.get(new ActualField(Model.S2C_GAME), new FormalField(SealedObject.class), new ActualField(0));

        String decryptedMessage = (String) ((SealedObject) countessrule[1]).getObject(clientCipher);

        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
        int field1 = Integer.parseInt(field1text);
        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));

        Assert.assertEquals(field1, Model.KNOCK_OUT);
    }

    @Test
    public void targetcallsTest() throws BadPaddingException, InterruptedException, NoSuchAlgorithmException, IllegalBlockSizeException, IOException, NoSuchPaddingException, InvalidKeyException, ClassNotFoundException {
        SealedObject encryptedLobbyNameString = new SealedObject("TestLobbyCon" + "!" + "TestClient#123", cipherForEncryptingServer);
        SealedObject encryptedKeySending = new SealedObject(clientKey, cipherForEncryptingServer);

        serverData.requestSpace.put(REQUEST_CODE, Server.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKeySending);

        runRequestHandler();

        serverData.responseSpace.get(new ActualField(Server.S2C_CREATE_RESP), new ActualField(Server.CREATE_LOBBY_RESP), new FormalField(SealedObject.class));
        Object[] tuple = serverData.lobbyOverviewSpace.query(new ActualField(Server.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace lobbySpace = new RemoteSpace("tcp://localhost:25565/"+ tuple[2].toString() + "?keep");

        Object[] lobbyKey = lobbySpace.query(new FormalField(PublicKey.class));

        PublicKey lobbyPublicKey = (PublicKey) lobbyKey[0];

        Cipher lobbyCipher = Cipher.getInstance("RSA");
        lobbyCipher.init(Cipher.ENCRYPT_MODE,lobbyPublicKey);

        String messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient#123?0=";

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        SealedObject encryptedKey = new SealedObject(clientKey, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        Object[] tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.CONNECT + "!TestClient2#123?0=";

        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, encryptedKey);

        tuple2 = lobbySpace.get(new ActualField(Lobby.S2C_CONNECT_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

        messageToBeEncrypted = "" + Lobby.BEGIN + "!TestClient#123?" + -1 + "=";
        encryptedMessage = new SealedObject(messageToBeEncrypted, lobbyCipher);
        SealedObject filler = new SealedObject("filler", lobbyCipher);

        lobbySpace.put(Model.C2S_LOBBY_GAME, encryptedMessage, filler);

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(0));

        lobbySpace.get(new ActualField(Lobby.S2C_LOBBY), new ActualField(Lobby.BEGIN), new FormalField(String.class),new ActualField(0), new ActualField(1));

        Thread.sleep(3000);

        encryptedMessage = new SealedObject("TestClient#123!" + 2,lobbyCipher);

        lobbySpace.put("TargetablePlayersRequest",encryptedMessage);

        encryptedMessage = new SealedObject("TestClient#123!" + 1,lobbyCipher);

        lobbySpace.put("TargetablePlayersRequest",encryptedMessage);

        encryptedMessage = new SealedObject("TestClient#123!" + 0,lobbyCipher);

        lobbySpace.put("TargetablePlayersRequest",encryptedMessage);
    }
}
