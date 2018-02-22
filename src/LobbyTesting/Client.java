package LobbyTesting;

import org.jspace.RemoteSpace;

import javax.swing.*;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client implements Runnable{

    public Client() {

    }

    public static void startClient(){

        RemoteSpace chat;

        Scanner reader = new Scanner(System.in);  // Reading from System.in
        try {

            String serverAddress = JOptionPane.showInputDialog(
                    "Enter IP Address of a machine that is\n" +
                            "running the server on port 25565:", "10.69.51.98");


            chat = new RemoteSpace("tcp://" + serverAddress + ":25565/ServerSpace?keep");

            String username  = JOptionPane.showInputDialog("Type in you username");

            chat.put("Connection",username);

            Inputloop:
            while(true) {

                String lobbymessage = reader.nextLine(); // Scans the next token of the input as an String.

                if (lobbymessage.contains("Begin")) {
                    chat.put("Begin");
                    //break Inputloop;
                } else if (lobbymessage.contains("Leave")){
                    chat.put("Leave", username);
                } else if (lobbymessage.contains("Connection")){
                    username  = JOptionPane.showInputDialog("Type in you username");
                    chat.put("Connection", username);
                } else if (lobbymessage.contains("Go")){
                    break Inputloop;
                }
            }

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.exit(0);

    }

    @Override
    public void run() {
        RemoteSpace chat;

        Scanner reader = new Scanner(System.in);  // Reading from System.in
        try {

            String serverAddress = JOptionPane.showInputDialog(
                    "Enter IP Address of a machine that is\n" +
                            "running the server on port 25565:", "10.69.51.98");


            chat = new RemoteSpace("tcp://" + serverAddress + ":25565/ServerSpace?keep");

            String username  = JOptionPane.showInputDialog("Type in you username");

            chat.put("Connection",username);

            Inputloop:
            while(true) {

                String lobbymessage = reader.nextLine(); // Scans the next token of the input as an String.

                if (lobbymessage.contains("Begin")) {
                    chat.put("Begin");
                    //break Inputloop;
                } else if (lobbymessage.contains("Leave")){
                    chat.put("Leave", username);
                } else if (lobbymessage.contains("Connection")){
                    username  = JOptionPane.showInputDialog("Type in you username");
                    chat.put("Connection", username);
                } else if (lobbymessage.contains("Go")){
                    break Inputloop;
                }

            }


            System.exit(0);

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}