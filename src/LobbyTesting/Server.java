package LobbyTesting;

import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import javax.swing.*;


public class Server {

    private SequentialSpace ServerSpace = new SequentialSpace();
    private SpaceRepository ServerConnection = new SpaceRepository();

    Server (String serverName) {

        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of this machine\n" +
                        "it will run the server on port 25565:", "10.69.51.98");

        //HUSK AT ÆNDRE IP VÆRDIEN HER
        //ServerConnection.addGate("tcp://10.69.51.98:25565/?keep");
        ServerConnection.addGate("tcp://" + serverAddress + ":25565/?keep");
        ServerConnection.add(serverName, ServerSpace);

    }

    public SequentialSpace GetServerSpace(){
        return ServerSpace;
    }
}