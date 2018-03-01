package MasterLobbyListServerTest.Server_Part;

import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import java.util.ArrayList;

public class ServerData{

    final static int MAXIMUM_LOBBIES = 8;

    SequentialSpace lobbyOverviewSpace = new SequentialSpace();
    SequentialSpace requestSpace = new SequentialSpace();
    SequentialSpace responseSpace = new SequentialSpace();
    SpaceRepository serverRepos = new SpaceRepository();

    ExecutorService executor = Executors.newFixedThreadPool(MAXIMUM_LOBBIES);//creating a pool of 5 threads

    public ServerData() {

            String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of this machine\n" +
                        "it will run the server on port 25565:", "10.68.96.109"); //

            serverRepos.addGate("tcp://" + serverAddress + ":25565/?keep");
            serverRepos.add("lobbyOverviewSpace", lobbyOverviewSpace);
            serverRepos.add("requestSpace" , requestSpace);
            serverRepos.add("responseSpace", responseSpace);
    }
}


