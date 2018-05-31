package mainSystemFiles.JavaFXClient;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.util.*;

/* IMPORTANT NOTE:
* These test are essentially testing is from the perspective of a client and treating the server as a black box.
* We test that given different requests we get expected responds from the server based on the description of
* Functional Requirements (see report). The server should be thus be running on the current local IP before
* executing the tests.
* */

public class ControllerTest {

    private Model modelPlayerOne;
    private Model modelPlayerTwo;
    private Model modelPlayerThree;
    private Model modelPlayerFour;
    private Model modelPlayerFive;
    //private ArrayList<long> timeMeasurements = new ArrayList<long>();

    @Before
    public void initialize() throws NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException, InvalidKeyException, IOException {
        // Create model objects simulating each of the five players in the testing environment
        modelPlayerOne = new Model();
        modelPlayerTwo = new Model();
        modelPlayerThree = new Model();
        modelPlayerFour = new Model();
        modelPlayerFive = new Model();

        // Get the current local IP (n.b! server is assumed to be running on this address. Set it up manually)
        // Let the five clients join the server.
        String IP = HelperFunctions.currentLocalIP();
        modelPlayerOne.joinServerLogic(IP);
        modelPlayerTwo.joinServerLogic(IP);
        modelPlayerThree.joinServerLogic(IP);
        modelPlayerFour.joinServerLogic(IP);
        modelPlayerFive.joinServerLogic(IP);
    }

    @Test
    public void joinServerLogicTest() throws NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException {


        // Get IP, create model and join server
        String IP = HelperFunctions.currentLocalIP();
        Model m = new Model();
        m.joinServerLogic(IP);

        // Create tuple and send PING_REQ
        SealedObject so1 = new SealedObject("dummy", m.getServerCipher());
        SealedObject so2 = new SealedObject(m.key, m.getServerCipher());
        m.getRequestSpace().put(m.REQUEST_CODE, m.PING_REQ, so1, so2);

        // Wait for PONG_RESP
        Object[] tuple = modelPlayerOne.getResponseSpace().get(new ActualField(m.RESPONSE_CODE), new ActualField(m.PONG_RESP));
//        long endTime = System.currentTimeMillis();
//        System.out.println("Time: " + (endTime-startTime));
        int expected = m.PONG_RESP;
        int actual = (int) tuple[1];

        // Assertion
        Assert.assertEquals(expected, actual);


    }

    @Test
    public void createUserLegalNameTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String user = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        long startTime = System.currentTimeMillis();
        boolean success = modelPlayerOne.createUserLogic(user);
        long endTime = System.currentTimeMillis();
        System.out.println("Approximate milliseconds for respond: " + (endTime-startTime));
        //timeMeasurements.add((endTime-startTime));

        // Assert that it was a success to create the user name
        Assert.assertTrue(success);

        // Assert that the character seperating the name and the UUID is a #
        Assert.assertEquals(modelPlayerOne.getUserID().substring(user.length(), user.length()+1),"#");

        // Assert that the last 36 characters of the user-id matches an UUID pattern
        String stringUUID = modelPlayerOne.getUserID().substring(modelPlayerOne.getUserID().length() - 36);
        Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(stringUUID));

        // Assert that length of the user-id is correct
        int actual = modelPlayerOne.getUserID().length();
        int expected = user.length() + 37; // the unique id is user name + # (1 char.) + UUID (36 char.)
        Assert.assertEquals(expected, actual);

        // Assert that the user name corresponds to the inputted name upon creation
        String actual2 = HelperFunctions.removeUUIDFromUserName(modelPlayerOne.getUserID()); //controller.removedIdFromUsername();
        String expected2 = user;
        Assert.assertEquals(expected2, actual2);
    }

    @Test
    public void createUserIllegalNameDueToIllegalCharactersTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        // A string with two illegal characters
        String illegalString= "-@";

        // Nickname consisting of 5 legal characters + 2 illegal characters
        String user = HelperFunctions.randomLegalName(5) + illegalString;

        // Assert that creation of user was unsuccessful
        boolean outcome = modelPlayerOne.createUserLogic(user);
        Assert.assertFalse(outcome);
    }

    @Test
    public void createUserIllegalNameTooShortTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        boolean outcome  = modelPlayerOne.createUserLogic(HelperFunctions.randomLegalName(1));
        Assert.assertFalse(outcome);
    }

    @Test
    public void createUserIllegalNameTooLongTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String userName = HelperFunctions.randomLegalName(16); // max legal length is 15
        boolean outcome  = modelPlayerOne.createUserLogic(userName);
        Assert.assertFalse(outcome);
    }

    @Test
    public void createLegalLobbyTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        // Create a random legal name
        String lobbyName = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        // outcome is true if the creation is successful
        boolean outcome = modelPlayerOne.createLobbyLogic(lobbyName);
        Assert.assertTrue(outcome);


        // Query the lobby information from Lobby List Space and assert that the lobby has an UUID
        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new ActualField(lobbyName), new FormalField(UUID.class));
        UUID lobbyID = (UUID) tuple[2];
        Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(lobbyID.toString()));
    }

    @Test
    public void createLobbyIllegalNameDueToIllegalCharactersTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        // A string with two illegal characters
        String illegalString= "-@";

        // Nickname consisting of 5 legal characters + 2 illegal characters
        String lobby = HelperFunctions.randomLegalName(5) + illegalString;

        // Assert that creation of user was unsuccessful
        boolean outcome = modelPlayerOne.createLobbyLogic(lobby);
        Assert.assertFalse(outcome);
    }

    @Test
    public void createLobbyIllegalNameTooShortTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        boolean outcome  = modelPlayerOne.createUserLogic(HelperFunctions.randomLegalName(1));
        Assert.assertFalse(outcome);
    }

    @Test
    public void createLobbyIllegalNameTooLongTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String lobby = HelperFunctions.randomLegalName(16); // max legal length is 15
        boolean outcome  = modelPlayerOne.createLobbyLogic(lobby);
        Assert.assertFalse(outcome);
    }

    @Test
    public void queryLobbiesTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        // Assign an user-id for the acting client, Alice
        modelPlayerOne.createUserLogic("Alice");

        // Clearing potential existing lobbies
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        // Alice creates three arbitrary lobbies
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
        int expected = 3;
        int actual = tuples.size();
        Assert.assertEquals(expected, actual);

        // Assert that for each of the lobby-tuples:
        // the first field with the name is contained in the set of lobby names
        // the second field with the lobby ID matches a UUID-pattern
        // the player can join and leave the lobbies
        for(Object[] obj : tuples){
            Assert.assertTrue(lobbyNames.contains((String) obj[1]));
            Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(obj[2].toString()));

            modelPlayerOne.joinLobbyLogic((String) obj[1], (UUID) obj[2], modelPlayerOne.getCurrentThreadNumber());
            Assert.assertTrue(modelPlayerOne.getInLobby());
            modelPlayerOne.sendDisconnectTuple();
            Assert.assertFalse(modelPlayerOne.getInLobby());
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

        modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());

        Assert.assertTrue(modelPlayerOne.getInLobby());
    }

    @Test
    public void disconnectFromOwnLobbyTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Assert.assertFalse(modelPlayerOne.getInLobby());
        Assert.assertFalse(modelPlayerTwo.getInLobby());

        // Clearing potential existing lobbies
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        modelPlayerOne.createUserLogic("Alice");
        modelPlayerTwo.createUserLogic("Bob");

        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        modelPlayerOne.createLobbyLogic(lobbyOne);
        System.out.println("248");
        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        System.out.println("250");
//        System.out.println("Is it null? " + tuple.equals(null));
//        System.out.println("Same name? " + tuple[1].equals(lobbyOne));
//        System.out.println("name: " + tuple[1] + " uuid: " + tuple[2]);

        modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        modelPlayerTwo.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());

        Assert.assertTrue(modelPlayerOne.getInLobby());
        Assert.assertTrue(modelPlayerTwo.getInLobby());
        modelPlayerOne.sendDisconnectTuple();
        Assert.assertFalse(modelPlayerOne.getInLobby());

        Object[] tuple2 = modelPlayerTwo.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new FormalField(Integer.class),
               new FormalField(String.class),new ActualField(modelPlayerTwo.getCurrentThreadNumber()), new ActualField(modelPlayerTwo.getIndexInLobby()));

        Assert.assertEquals(Model.CLOSE, tuple2[1]);
        modelPlayerTwo.setInLobby(false);
        modelPlayerTwo.resetLobbyInfo();
        Assert.assertFalse(modelPlayerTwo.getInLobby());

        //TODO: a bit sketchy? maybe there is a better solution.
        Thread.sleep(200);

        Assert.assertFalse(modelPlayerTwo.getInLobby());

        int expected = Model.NO_RESPONSE;
        int actual = modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void disconnectFromOthersLobbyTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        // Clearing potential existing lobbies
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        modelPlayerOne.createUserLogic("Alice");
        modelPlayerTwo.createUserLogic("Bob");

        Assert.assertFalse(modelPlayerOne.getInLobby());
        Assert.assertFalse(modelPlayerTwo.getInLobby());

        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        modelPlayerOne.createLobbyLogic(lobbyOne);

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        modelPlayerTwo.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());

        Assert.assertTrue(modelPlayerOne.getInLobby());
        Assert.assertTrue(modelPlayerTwo.getInLobby());
        modelPlayerTwo.sendDisconnectTuple();
        Assert.assertFalse(modelPlayerTwo.getInLobby());

        Object[] tuple2 = modelPlayerOne.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new ActualField(Model.LOBBY_DISCONNECT),
                new FormalField(String.class),new ActualField(modelPlayerOne.getCurrentThreadNumber()), new ActualField(modelPlayerOne.getIndexInLobby()));

        Assert.assertEquals(Model.LOBBY_DISCONNECT, tuple2[1]);

        Assert.assertFalse(modelPlayerTwo.getInLobby());

        int expected = Model.OK;
        int actual = modelPlayerTwo.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());
        Assert.assertEquals(expected,actual);
    }

    @Test
    public void beginWithTooFewPlayersTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        //String lobbyName = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        modelPlayerOne.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));
        modelPlayerOne.createLobbyLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());

        modelPlayerOne.getLobbyListSpace().getAll(new ActualField(Model.LOBBY_UPDATE), new FormalField(Integer.class), new FormalField(String.class), new FormalField(Integer.class), new FormalField(Integer.class));
        modelPlayerOne.pressBeginLogic();
        Object[] tuple2 = modelPlayerOne.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new FormalField(Integer.class), new FormalField(String.class), new FormalField(Integer.class), new FormalField(Integer.class));
        Assert.assertEquals((int) tuple2[1], Model.NOT_ENOUGH_PLAYERS);
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

    @Test
    public void startGameTest() throws InterruptedException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, IOException {
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        String userNameOne = "Alice";
        String userNameTwo = "Bob";
        modelPlayerOne.createUserLogic(userNameOne);
        modelPlayerTwo.createUserLogic(userNameTwo);

        String lobbyName = "fun_lobby";
        modelPlayerOne.createLobbyLogic(lobbyName);

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"),
                new FormalField(String.class), new FormalField(UUID.class));

        modelPlayerOne.joinLobbyLogic(lobbyName, (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        modelPlayerTwo.joinLobbyLogic(lobbyName, (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());

        modelPlayerOne.pressBeginLogic();

        Thread.sleep(3000); // TODO: a bit sloopy (due to race condition)

        Object[] tuple2 = modelPlayerOne.getLobbyListSpace().queryp(new ActualField("Lobby"),
                new FormalField(String.class), new FormalField(UUID.class));
        boolean c = (tuple2==null);

        Assert.assertTrue(c);
//
//        System.out.println("player 1: in game? " + modelPlayerOne.inGame + " in lobby: " + modelPlayerOne.inLobby);
//        System.out.println("player 2: in game? " + modelPlayerTwo.inGame + " in lobby: " + modelPlayerTwo.inLobby);
//        Assert.assertTrue(modelPlayerTwo.inGame);
//        Assert.assertFalse(modelPlayerTwo.inLobby);
//        Assert.assertTrue(modelPlayerOne.inGame);
//        Assert.assertFalse(modelPlayerOne.inLobby);

        // TODO fejler fordi flagene bliver sat i ClientUpdateAgent linie 126-128 som er blandet sammen med GUI..
    }

    //TODO burde vi kun liste lobbies der er joinable når der queries?
    @Test
    public void joinDeniedDueToFullLobby() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        String userNameOne = "Alice";
        String userNameTwo = "Bob";
        String userNameThree = "Charles";
        String userNameFour = "Dave";
        String userNameFive = "Eric";
        modelPlayerOne.createUserLogic(userNameOne);
        modelPlayerTwo.createUserLogic(userNameTwo);
        modelPlayerThree.createUserLogic(userNameThree);
        modelPlayerFour.createUserLogic(userNameFour);
        modelPlayerFive.createUserLogic(userNameFive);

        modelPlayerOne.createLobbyLogic("fun_lobby");

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"),
                new FormalField(String.class), new FormalField(UUID.class));
        int actualOne = modelPlayerOne.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        int actualTwo = modelPlayerTwo.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());
        int actualThree = modelPlayerThree.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerThree.getCurrentThreadNumber());
        int actualFour = modelPlayerFour.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerFour.getCurrentThreadNumber());
        int actualFive = modelPlayerFive.joinLobbyLogic("fun_lobby", (UUID) tuple[2], modelPlayerFive.getCurrentThreadNumber());
        int expectedOneToFour = Model.OK;
        int expectedFive = Model.BAD_REQUEST;

        Assert.assertEquals(expectedOneToFour, actualOne);
        Assert.assertEquals(expectedOneToFour, actualTwo);
        Assert.assertEquals(expectedOneToFour, actualThree);
        Assert.assertEquals(expectedOneToFour, actualFour);
        Assert.assertEquals(expectedFive, actualFive);
    }

    @Test
    public void chatTest() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        // Clearing potential existing lobbies
        modelPlayerOne.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        String nicknameOne = "Alice";
        String nicknameTwo = "Bob";
        String nicknameThree = "Charles";
        modelPlayerOne.createUserLogic(nicknameOne); // Lobby leader
        modelPlayerTwo.createUserLogic(nicknameTwo);
        modelPlayerThree.createUserLogic(nicknameThree);

        modelPlayerOne.createLobbyLogic("fun_lobby");

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class),
                new FormalField(UUID.class));
        modelPlayerOne.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        Object[] tuple2 = modelPlayerTwo.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class),
                new FormalField(UUID.class));
        modelPlayerTwo.joinLobbyLogic((String) tuple2[1], (UUID) tuple2[2], modelPlayerTwo.getCurrentThreadNumber());
        Object[] tuple3 = modelPlayerThree.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class),
                new FormalField(UUID.class));
        modelPlayerThree.joinLobbyLogic((String) tuple3[1], (UUID) tuple3[2], modelPlayerThree.getCurrentThreadNumber());

        String msg = "Hi Bob and Charles";
        modelPlayerOne.textToChatLogic(msg);

        Object[] tuple4 = modelPlayerTwo.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new ActualField(Model.CHAT_MESSAGE),
                new FormalField(String.class),new ActualField(modelPlayerTwo.getCurrentThreadNumber()),
                new ActualField(modelPlayerTwo.getIndexInLobby()));

        String expected = nicknameOne + " : " + msg;

        String actual = (String) tuple4[2];

        System.out.println("Actual: " + actual);
        Assert.assertEquals(expected,actual);

        Object[] tuple5 = modelPlayerThree.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new ActualField(Model.CHAT_MESSAGE),
                new FormalField(String.class),new ActualField(modelPlayerThree.getCurrentThreadNumber()),
                new ActualField(modelPlayerThree.getIndexInLobby()));

        String actual2 = (String) tuple5[2];
        Assert.assertEquals(expected,actual2);
    }

    @Ignore
    public void takeActionsIngame() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, ClassNotFoundException {
        modelPlayerOne.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));
        modelPlayerTwo.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));

        modelPlayerOne.createLobbyLogic("LobbyTest");

        Object[] tuple = modelPlayerOne.getLobbyListSpace().query(new ActualField("Lobby"),
                new FormalField(String.class), new FormalField(UUID.class));

        modelPlayerOne.joinLobbyLogic("LobbyTest", (UUID) tuple[2], modelPlayerOne.getCurrentThreadNumber());
        modelPlayerTwo.joinLobbyLogic("LobbyTest", (UUID) tuple[2], modelPlayerTwo.getCurrentThreadNumber());

        modelPlayerOne.getLobbySpace().getAll(new ActualField(Model.LOBBY_UPDATE), new FormalField(Integer.class), new FormalField(String.class), new FormalField(Integer.class), new FormalField(Integer.class));

        modelPlayerOne.pressBeginLogic();
        Object[] tuple2 = modelPlayerOne.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new ActualField(Model.BEGIN), new FormalField(String.class), new FormalField(Integer.class), new FormalField(Integer.class));

        //Tag skiftevis træk for hver spiller.
        int nrOfMovesToTest = 5;
        Model modelForCurrentPlayer;
        for (int i = 0; i < nrOfMovesToTest;i++){
            if (i % 2 == 0){
                modelForCurrentPlayer = modelPlayerOne;
            } else {
                modelForCurrentPlayer = modelPlayerTwo;
            }
            Random rn = new Random();
            outerloop:
            while(true){
                int playedCard = rn.nextInt(2);
                int target = rn.nextInt(4);
                int guessNr = rn.nextInt(7) + 1;

                String messageToBeEncrypted = "12!" + modelForCurrentPlayer.getUserID() + "?" + playedCard + "=" + target + "*" + guessNr;
                SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,modelForCurrentPlayer.getLobbyCipher());

                modelForCurrentPlayer.getLobbySpace().put(20, encryptedMessage); // Send the action to the server

                innerloop:
                while(true) {
                    Object[] tuple3 = modelForCurrentPlayer.getLobbySpace().get(new ActualField(10), new FormalField(SealedObject.class), new ActualField(modelForCurrentPlayer.getIndexInLobby()));

                    String decryptedNewRound = (String) ((SealedObject) tuple3[1]).getObject(modelForCurrentPlayer.personalCipher);
                    String field1text = decryptedNewRound.substring(0, decryptedNewRound.indexOf('!'));

                    if (field1text.equals("13")) {
                        break outerloop;
                    } else if (field1text.equals("17")) {
                        break innerloop;
                    }
                }
            }
        }

    }
}