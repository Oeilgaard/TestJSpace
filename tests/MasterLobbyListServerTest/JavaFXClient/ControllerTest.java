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
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ControllerTest {

    private Model model;

    @Before
    public void initialize() throws NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException, InvalidKeyException, IOException {
        model = new Model();
        String IP = HelperFunctions.currentLocalIP();
        model.joinServerLogic(IP);
    }

    @Test
    public void joinServerLogicTest() throws NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException, InvalidKeyException, IOException, IllegalBlockSizeException {
        String IP = HelperFunctions.currentLocalIP();
        model.joinServerLogic(IP);
        SealedObject so1 = new SealedObject("null", model.getServerCipher());
        SealedObject so2 = new SealedObject(model.key, model.getServerCipher());
        model.getRequestSpace().put(model.REQUEST_CODE, model.PING_REQ, so1, so2);
        Object[] tuple = model.getResponseSpace().get(new ActualField(model.RESPONSE_CODE), new ActualField(model.PONG_RESP));
        int expected = model.PONG_RESP;
        int actual = (int) tuple[1];
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void createUserLegalNameTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String user = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        boolean success = model.createUserLogic(user);

        // Assert that it was a success to create the user name
        Assert.assertTrue(success);

        // Assert that the character seperating the name and the UUID is a #
        Assert.assertEquals(model.getUniqueName().substring(user.length(), user.length()+1),"#");

        // Assert that the last 36 characters of the user-id matches an UUID pattern
        String stringUUID = model.getUniqueName().substring(model.getUniqueName().length() - 36);
        Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(stringUUID));

        // Assert that length of the user-id is correct
        int actual = model.getUniqueName().length();
        int expected = user.length() + 37; // the unique id is user name + # (1 char.) + UUID (36 char.)
        Assert.assertEquals(expected, actual);

        // Assert that the user name corresponds to the inputted name upon creation
        String actual2 = HelperFunctions.removeUUIDFromUserName(model.getUniqueName()); //controller.removedIdFromUsername();
        String expected2 = user;
        Assert.assertEquals(expected2, actual2);
    }

    @Test
    public void createUserIllegalNameTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String illegalString= "-@";
        String user = HelperFunctions.randomLegalName(5) + illegalString;
        boolean outcome = model.createUserLogic(user);
        Assert.assertFalse(outcome);
    }

    @Test
    public void createUserIllegalNameTooShortTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        boolean outcome  = model.createUserLogic("a");
        Assert.assertFalse(outcome);
    }
    @Test
    public void createUserIllegalNameTooLongTest() throws InterruptedException, IOException, IllegalBlockSizeException {
        String userName = HelperFunctions.randomLegalName(16);
        boolean outcome  = model.createUserLogic(userName);
        Assert.assertFalse(outcome);
    }

    @Test
    public void createLegalLobby() throws InterruptedException, IOException, IllegalBlockSizeException {
        String lobbyName = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        boolean outcome = model.createLobbyLogic(lobbyName);
        Assert.assertTrue(outcome);
        Object[] tuple = model.getLobbyListSpace().get(new ActualField("Lobby"), new ActualField(lobbyName), new FormalField(UUID.class));
        UUID lobbyID = (UUID) tuple[2];
        Assert.assertTrue(HelperFunctions.stringMatchesUUIDPattern(lobbyID.toString()));

    }

    @Test
    public void queryLobbies() throws InterruptedException, IOException, IllegalBlockSizeException {
        // Clearing potential existing lobbies
        model.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        // Create three lobbies
        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        String lobbyTwo = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        String lobbyThree = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        model.createLobbyLogic(lobbyOne);
        model.createLobbyLogic(lobbyTwo);
        model.createLobbyLogic(lobbyThree);

        // Add the names to a temp. set
        HashSet<String> lobbyNames = new HashSet<>();
        lobbyNames.add(lobbyOne);
        lobbyNames.add(lobbyTwo);
        lobbyNames.add(lobbyThree);

        // Query all lobby-tuples (could be seperated to a function in model moved to a function)
        List<Object[]> tuples = model.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

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
    public void joinLobby() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Assert.assertFalse(model.getInLobby());

        // Clearing potential existing lobbies
        model.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        model.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));

        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        model.createLobbyLogic(lobbyOne);

        Object[] tuple = model.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        System.out.println("Is it null? " + tuple.equals(null));
        System.out.println("Same name? " + tuple[1].equals(lobbyOne));
        System.out.println("name: " + tuple[1] + " uuid: " + tuple[2]);

        model.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], model.getCurrentThreadNumber());

        Assert.assertTrue(model.getInLobby());
    }

    @Test
    public void disconnectFromOwnLobby() throws InterruptedException, IOException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        Assert.assertFalse(model.getInLobby());

        // Clearing potential existing lobbies
        model.getLobbyListSpace().getAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
        model.createUserLogic(HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength()));

        String lobbyOne = HelperFunctions.randomLegalName(HelperFunctions.randomLegalNameLength());
        model.createLobbyLogic(lobbyOne);

        Object[] tuple = model.getLobbyListSpace().query(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        System.out.println("Is it null? " + tuple.equals(null));
        System.out.println("Same name? " + tuple[1].equals(lobbyOne));
        System.out.println("name: " + tuple[1] + " uuid: " + tuple[2]);

        model.joinLobbyLogic((String) tuple[1], (UUID) tuple[2], model.getCurrentThreadNumber());

        Assert.assertTrue(model.getInLobby());
        model.sendDisconnectTuple();
        Assert.assertFalse(model.getInLobby());
    }
}