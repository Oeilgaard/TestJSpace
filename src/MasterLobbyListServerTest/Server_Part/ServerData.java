package MasterLobbyListServerTest.Server_Part;

import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerData{

    //TODO: fryser hvis vi laver mere end 8 lobbies.
    public final static int MAXIMUM_LOBBIES = 2;
    public final static int MAXIMUM_REQUESTS = 1000;

    SequentialSpace lobbyOverviewSpace = new SequentialSpace();
    SequentialSpace requestSpace = new SequentialSpace();
    SequentialSpace responseSpace = new SequentialSpace();
    SpaceRepository serverRepos = new SpaceRepository();
    KeyPairGenerator kpg;
    public Cipher cipher;
    private int currentNoThreads; // the current amount of either lobby or game threads (each lobby/game actually have two threads)
    ExecutorService executor = Executors.newFixedThreadPool(MAXIMUM_LOBBIES); // creating a pool lobby threads
    ExecutorService requestExecutor = Executors.newFixedThreadPool(MAXIMUM_REQUESTS); // creating a pool lobby threads

    // ServerData constructor
    ServerData() throws InvalidKeyException, NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException {

        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of this machine\n" +
                        "it will run the server on port 25565:", "localhost");

        serverRepos.addGate("tcp://" + serverAddress + ":25565/?keep");
        serverRepos.add("lobbyOverviewSpace", lobbyOverviewSpace);
        serverRepos.add("requestSpace" , requestSpace);
        serverRepos.add("responseSpace", responseSpace);
        currentNoThreads = 0;

        // Setting up Public Key Crypto

        kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair myPair = kpg.generateKeyPair();

        // Putting the Public Key for communication to the server in the "request space"
        requestSpace.put(myPair.getPublic());

        // Create an instance of the Cipher for RSA encryption/decryption
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, myPair.getPrivate());

    }

    // Methods for incrementing, decrementing and getting the current no. of threads in a safe, synchronized manner
    public synchronized void incrementCurrentNoThreads(){
        this.currentNoThreads++;
        System.out.println("Current no of threads: " + currentNoThreads);
    }

    public synchronized void decrementCurrentNoThreads(){
        this.currentNoThreads--;
        System.out.println("Current no of threads: " + currentNoThreads);
    }

    public synchronized int getCurrentNoThreads(){
        return currentNoThreads;
    }

    public synchronized void createNewLobbyThread(UUID uuid, String username, ServerData serverData){
        //calling execute method of ExecutorService
        executor.execute(new Lobby(uuid, lobbyOverviewSpace, serverRepos, username, serverData));

        // printing the current amount of active threads
        if (executor instanceof ThreadPoolExecutor) {
            System.out.println(
                    "Pool size is now " +
                            ((ThreadPoolExecutor) executor).getActiveCount()
            );
        }
        incrementCurrentNoThreads(); //TODO: decrement accordingly
    }
}


