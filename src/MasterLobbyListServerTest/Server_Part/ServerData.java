package MasterLobbyListServerTest.Server_Part;

import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import javax.swing.*;
import java.util.ArrayList;

public class ServerData{

    final static int MAXIMUM_LOBBIES = 8;

    SequentialSpace lobbyOverviewSpace = new SequentialSpace();
    SequentialSpace requestSpace = new SequentialSpace();
    SpaceRepository serverRepos = new SpaceRepository();

    static ArrayList<Thread> lobbyThreads = new ArrayList<>();

    public ServerData(){
        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of this machine\n" +
                        "it will run the server on port 25565:", "10.69.51.98");

        serverRepos.addGate("tcp://" + serverAddress + ":25565/?keep");
        serverRepos.add("lobbyOverviewSpace", lobbyOverviewSpace);
        serverRepos.add("requestSpace" , requestSpace);
    }

}
