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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerData{

    //TODO: fryser hvis vi laver mere end 8 lobbies.
    private final static int MAXIMUM_LOBBIES = 8;

    SequentialSpace lobbyOverviewSpace = new SequentialSpace();
    SequentialSpace requestSpace = new SequentialSpace();
    SequentialSpace responseSpace = new SequentialSpace();
    SpaceRepository serverRepos = new SpaceRepository();
    KeyPairGenerator kpg;
    public Cipher cipher;

    ExecutorService executor = Executors.newFixedThreadPool(MAXIMUM_LOBBIES);//creating a pool of 5 threads

    ServerData() throws InvalidKeyException, NoSuchAlgorithmException, InterruptedException, NoSuchPaddingException {

        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of this machine\n" +
                        "it will run the server on port 25565:", "192.168.43.24"); //

        serverRepos.addGate("tcp://" + serverAddress + ":25565/?keep");
        serverRepos.add("lobbyOverviewSpace", lobbyOverviewSpace);
        serverRepos.add("requestSpace" , requestSpace);
        serverRepos.add("responseSpace", responseSpace);

        // Setting up Public Key Crypto

        kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair myPair = kpg.generateKeyPair();

        // Putting the Public Key for communication to the server in the "request space"
        requestSpace.put(myPair.getPublic());

        // Create an instance of the Cipher for RSA encryption/decryption
        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, myPair.getPrivate());

    }
}


