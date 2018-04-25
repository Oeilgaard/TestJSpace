package MasterLobbyListServerTest.Server_Part;

import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.security.*;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerData{

    //TODO: fryser hvis vi laver mere end 8 lobbies.
    public final static int MAXIMUM_LOBBIES = 1000;
    public final static int MAXIMUM_REQUESTS = 1000;

    SequentialSpace lobbyOverviewSpace = new SequentialSpace();
    SequentialSpace requestSpace = new SequentialSpace();
    SequentialSpace responseSpace = new SequentialSpace();
    SpaceRepository serverRepos = new SpaceRepository();
    KeyPairGenerator kpg;
    public Cipher cipher;
//    private int currentNoThreads; // the current amount of either lobby or game threads (each lobby/game actually have two threads)
    ExecutorService executor = Executors.newFixedThreadPool(MAXIMUM_LOBBIES); // creating a pool lobby threads
    ExecutorService requestExecutor = Executors.newFixedThreadPool(MAXIMUM_REQUESTS); // creating a pool lobby threads
    public PrivateKey privateKey;
    HashMap<UUID, Lobby> lobbyMap = new HashMap<>();


    // ServerData constructor
    ServerData() throws InvalidKeyException, NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException {

        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of this machine\n" +
                        "it will run the server on port 25565:", "localhost");

        serverRepos.addGate("tcp://" + serverAddress + ":25565/?keep");
        serverRepos.add("lobbyOverviewSpace", lobbyOverviewSpace);
        serverRepos.add("requestSpace" , requestSpace);
        serverRepos.add("responseSpace", responseSpace);
//        currentNoThreads = 0;

        // Setting up Public Key Crypto

        kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair myPair = kpg.generateKeyPair();
        privateKey = myPair.getPrivate();

        // Putting the Public Key for communication to the server in the "request space"
        requestSpace.put(myPair.getPublic());

        // Create an instance of the Cipher for RSA encryption/decryption
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, myPair.getPrivate());

    }

    ServerData(String serverAddress) throws InvalidKeyException, NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException {

        serverRepos.addGate("tcp://" + serverAddress + ":25565/?keep");
        serverRepos.add("lobbyOverviewSpace", lobbyOverviewSpace);
        serverRepos.add("requestSpace" , requestSpace);
        serverRepos.add("responseSpace", responseSpace);
//        currentNoThreads = 0;

        // Setting up Public Key Crypto

        kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair myPair = kpg.generateKeyPair();
        privateKey = myPair.getPrivate();

        // Putting the Public Key for communication to the server in the "request space"
        requestSpace.put(myPair.getPublic());

        // Create an instance of the Cipher for RSA encryption/decryption
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, myPair.getPrivate());

    }

    public synchronized void createNewLobbyThread(UUID uuid, String username, ServerData serverData) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InterruptedException {
        //calling execute method of ExecutorService
        Lobby l = new Lobby(uuid, lobbyOverviewSpace, serverRepos, username, serverData);
        executor.execute(l);
        lobbyMap.put(uuid, l); // TODO remove when lobby is over

        // printing the current amount of active threads
        if (executor instanceof ThreadPoolExecutor) {
            System.out.println(
                    "Pool size is now " +
                            ((ThreadPoolExecutor) executor).getActiveCount()
            );
        }
    }

    public synchronized int currentNumberOfActiveThreads(){
        return ((ThreadPoolExecutor) executor).getActiveCount();
    }

    public synchronized void removeLobbyFromMap(UUID uuid){
        lobbyMap.remove(uuid);
    }
}


