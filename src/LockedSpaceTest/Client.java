package LockedSpaceTest;

import org.jspace.ActualField;
import org.jspace.RemoteSpace;

import javax.swing.*;
import java.io.IOException;
import java.util.Scanner;

public class Client {

    public static void main(String[] argv) throws InterruptedException, IOException {
        RemoteSpace remoteSpace;

        Scanner reader = new Scanner(System.in);  // Reading from System.in

        String serverAddress = JOptionPane.showInputDialog(
                    "Enter IP Address of a machine that is\n" +
                            "running the server on port 25565:", "10.69.51.150");


        remoteSpace = new RemoteSpace("tcp://" + serverAddress + ":25565/entrySpace?keep");

        System.out.println("Før get");

        Object[] tuple = remoteSpace.get(new ActualField("Test1"));

        System.out.println("Efter get");

        System.out.println("Success! : Tuplen indeholdte : " + tuple[0]);
    }
}
