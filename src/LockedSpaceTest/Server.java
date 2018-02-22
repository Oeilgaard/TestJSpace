package LockedSpaceTest;

import org.jspace.ActualField;
import org.jspace.SequentialSpace;
import org.jspace.SpaceRepository;

import javax.swing.*;
import java.io.IOException;

public class Server {

    private static SequentialSpace entrySpace = new SequentialSpace();

    public static void main(String[] argv) throws InterruptedException, IOException {
        //SequentialSpace entrySpace = new SequentialSpace();
        SpaceRepository serverRepos = new SpaceRepository();

        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of this machine\n" +
                        "it will run the server on port 25565:", "10.69.51.150");

        serverRepos.addGate("tcp://" + serverAddress + ":25565/?keep");
        serverRepos.add("entrySpace", entrySpace);

        entrySpace.put("Test1");

        System.out.println("Size of entrySpace is : " + entrySpace.size());

        entrySpace.get(new ActualField("Test2"));

        System.out.println("Der var en fejl");
    }

    public static int sizeOfRemoteSpace(){
        return entrySpace.size();
    }
}
