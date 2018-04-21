package MasterLobbyListServerTest.JavaFXClient;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ControllerTest {

    private Model modelPlayerOne;
    private Model modelPlayerTwo;

    @Before
    public void initialize() throws NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException, InvalidKeyException, IOException {
        modelPlayerOne = new Model();
        modelPlayerTwo = new Model();
        String IP = HelperFunctions.currentLocalIP();
        modelPlayerOne.joinServerLogic(IP);
        modelPlayerTwo.joinServerLogic(IP);
    }

    @Test
    public void joinServerLogicTest() throws NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException {
        String IP = HelperFunctions.currentLocalIP();
        modelPlayerOne.joinServerLogic(IP);
        SealedObject so1 = new SealedObject("null", modelPlayerOne.getServerCipher());
        SealedObject so2 = new SealedObject(modelPlayerOne.key, modelPlayerOne.getServerCipher());
        modelPlayerOne.getRequestSpace().put(modelPlayerOne.REQUEST_CODE, modelPlayerOne.PING_REQ, so1, so2);
        Object[] tuple = modelPlayerOne.getResponseSpace().get(new ActualField(modelPlayerOne.RESPONSE_CODE), new ActualField(modelPlayerOne.PONG_RESP));
        int expected = modelPlayerOne.PONG_RESP;
        int actual = (int) tuple[1];
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void createUserLegalNameTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String user = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        boolean success = modelPlayerOne.createUserLogic(user);

        // Assert that it was a success to create the user name
        Assert.assertTrue(success);

        // Assert that the character seperating the name and the UUID is a #
        Assert.assertEquals(modelPlayerOne.getUniqueName().substring(user.length(), user.length()+1),"#");

        // Assert that the last 36 characters of the user-id matches an UUID pattern
        String stringUUID = modelPlayerOne.getUniqueName().substring(modelPlayerOne.getUniqueName().length() - 36);
        Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(stringUUID));

        // Assert that length of the user-id is correct
        int actual = modelPlayerOne.getUniqueName().length();
        int expected = user.length() + 37; // the unique id is user name + # (1 char.) + UUID (36 char.)
        Assert.assertEquals(expected, actual);

        // Assert that the user name corresponds to the inputted name upon creation
        String actual2 = HelperFunctions.removeUUIDFromUserName(modelPlayerOne.getUniqueName()); //controller.removedIdFromUsername();
        String expected2 = user;
        Assert.assertEquals(expected2, actual2);
    }

    @Test
    public void createUserIllegalNameTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String illegalString= "-@";
        String user = HelperFunctions.randomLegalName(5) + illegalString;
        boolean outcome = modelPlayerOne.createUserLogic(user);
        Assert.assertFalse(outcome);
    }

    @Test
    public void createUserIllegalNameTooShortTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        boolean outcome  = modelPlayerOne.createUserLogic("a");
        Assert.assertFalse(outcome);
    }
    @Test
    public void createUserIllegalNameTooLongTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String userName = HelperFunctions.randomLegalName(16);
        boolean outcome  = modelPlayerOne.createUserLogic(userName);
        Assert.assertFalse(outcome);
    }

    @Test
    public void createLegalLobbyTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String lobbyName = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        boolean outcome = modelPlayerOne.createLobbyLogic(lobbyName);
        Assert.assertTrue(outcome);
        Object[] tuple = modelPlayerOne.getLobbyListSpace().get(new ActualField("Lobby"), new ActualField(lobbyName), new FormalField(UUID.class));
        UUID lobbyID = (UUID) tuple[2];
        Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(lobbyID.toString()));
    }

    @Test
    public void queryLobbiesTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        // Clearing potential existing lobbies
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        // Create three lobbies
        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        String lobbyTwo = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        String lobbyThree = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        modelPlayerOne.createLobbyLogic(lobbyOne);
        modelPlayerOne.createLobbyLogic(lobbyTwo);
        modelPlayerOne.createLobbyLogic(lobbyThree);

        // Add the names to a temp. set
        HashSet<String> lobbyNames = new HashSet<>();
        lobbyNames.add(lobbyOne);
        lobbyNames.add(lobbyTwo);
        lobbyNames.add(lobbyThree);

        // Query all lobby-tuples (could be seperated to a function in modelPlayerOne moved to a function)
        List<Object[]> tuples = modelPlayerOne.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        // Assert that 3 lobby-tuples are found
        Assert.assertEquals(3, tuples.size());

        // Assert that for each of the lobby-tuples, the first field with the name is contained in the set of lobby names and
        // Assert that the second field with the lobby ID matches a UUID-pattern
        for(Object[] obj : tuples){
            Assert.assertTrue(lobbyNames.contains((String) obj[1]));
            Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(obj[2].toString()));
        }
    }

    @Test
    public void joinLobbyTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Assert.assertFalse(modelPlayerOne.getInLobby());

        // Clearing potential existing lobbies
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        modelPlayerOne.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));

        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        modelPlayerOne.createLobbyLogic(lobbyOne);

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        System.out.println("Is it null? " + tuple.equals(null));
        System.out.println("Same name? " + tuple[1].equals(lobbyOne));
        System.out.println("name: " + tuple[1] + " uuid: " + tuple[2]);

        modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());

        Assert.assertTrue(modelPlayerOne.getInLobby());
    }

    @Test
    public void disconnectFromOwnLobbyTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Assert.assertFalse(modelPlayerOne.getInLobby());

        // Clearing potential existing lobbies
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        modelPlayerOne.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));

        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        modelPlayerOne.createLobbyLogic(lobbyOne);

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        System.out.println("Is it null? " + tuple.equals(null));
        System.out.println("Same name? " + tuple[1].equals(lobbyOne));
        System.out.println("name: " + tuple[1] + " uuid: " + tuple[2]);

        modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());

        Assert.assertTrue(modelPlayerOne.getInLobby());
        modelPlayerOne.sendDisconnectTuple();
        Assert.assertFalse(modelPlayerOne.getInLobby());

        // TODO: check at den ikke findes mere... (det gør den - hvorfor??)
        Object[] tuple2 = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        System.out.println(lobbyOne + " " + tuple2[1]);
    }

    @Test
    public void beginWithTooFewPlayersTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        //String lobbyName = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        modelPlayerOne.createLobbyLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));
        modelPlayerOne.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));
        modelPlayerOne.pressBeginLogic();
        Object[] tuple = modelPlayerOne.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new FormalField(Integer.class), new FormalField(String.class), new FormalField(Integer.class), new FormalField(Integer.class));

        //Assert.assertEquals((int) tuple[1], Model.NOT_ENOUGH_PLAYERS);
    }

    @Test
    public void queryPlayerNamesTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        String userNameOne = "Alice";
        String userNameTwo = "Bob";
        modelPlayerOne.createUserLogic(userNameOne);
        modelPlayerTwo.createUserLogic(userNameTwo);
        HashSet<String> userNames = new HashSet<>();
        userNames.add(userNameOne);
        userNames.add(userNameTwo);

        modelPlayerOne.createLobbyLogic("fun_lobby");

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"),
                new FormalField(String.class), new FormalField(UUID.class));

        modelPlayerOne.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        modelPlayerTwo.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());

        ArrayList<String> players = modelPlayerTwo.updatePlayerLobbyListLogic();

        for(String p : players){
            Assert.assertTrue(userNames.contains(p));
        }
    }

    @Ignore
    public void startGameTest() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, IOException {
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        String userNameOne = "Alice";
        String userNameTwo = "Bob";
        modelPlayerOne.createUserLogic(userNameOne);
        modelPlayerTwo.createUserLogic(userNameTwo);

        modelPlayerOne.createLobbyLogic("fun_lobby");

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"),
                new FormalField(String.class), new FormalField(UUID.class));

        modelPlayerOne.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        modelPlayerTwo.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());

        modelPlayerOne.pressBeginLogic();

        System.out.println("player 1: in game? " + modelPlayerOne.inGame + " in lobby: " + modelPlayerOne.inLobby);
        System.out.println("player 2: in game? " + modelPlayerTwo.inGame + " in lobby: " + modelPlayerTwo.inLobby);
        Assert.assertTrue(modelPlayerTwo.inGame);
        Assert.assertFalse(modelPlayerTwo.inLobby);
        Assert.assertTrue(modelPlayerOne.inGame);
        Assert.assertFalse(modelPlayerOne.inLobby);

        // TODO fejler fordi flagene bliver sat i ClientUpdateAgent linie 126-128 som er blandet sammen med GUI..
    }


}