package MasterLobbyListServerTest.Client_Part;

import org.jspace.RemoteSpace;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;
import java.util.Scanner;

public class Client {

    /*
        1: Connect to the master server.
        2: Query list of servers.
        3A: Choose server and connect to that space.
        3B: Ask for a new lobby.
        4B: If successful, name the new lobby and join it.
        5: Enter username
     */

    public static void main(String[] argv) throws InterruptedException, IOException {

        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of a machine that is\n" +
                        "running the server on port 25565:", "10.68.108.51");

        RemoteSpace serverSpace = new RemoteSpace("tcp://" + serverAddress + ":25565/requestSpace?keep");

        Scanner reader = new Scanner(System.in);  // Reading from System.in
        int number = reader.nextInt(); // Scans the next token of the input as an String.

        System.out.println("Putting now");

        for(int i = 0; i < number;i++){
            byte[] array = new byte[7]; // length is bounded by 7
            new Random().nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));
            byte[] array2 = new byte[7]; // length is bounded by 7
            new Random().nextBytes(array2);
            String generatedString2 = new String(array2, Charset.forName("UTF-8"));
            serverSpace.put(1,11,generatedString,generatedString2);
        }

        System.out.println("Put is now done : " + number);

    }

}
