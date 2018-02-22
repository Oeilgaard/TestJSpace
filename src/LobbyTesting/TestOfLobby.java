package LobbyTesting;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Scanner;

public class TestOfLobby {

    public static void main(String[] argv) throws InterruptedException, IOException {

        Scanner reader = new Scanner(System.in);  // Reading from System.in

        String pickedOption = reader.nextLine(); // Scans the next token of the input as an String.

        if(pickedOption.contains("Server")) {

            System.out.println("Running a Server");

            Server serverSpace = new Server("ServerSpace");

            System.out.println("The server is running on : " + InetAddress.getLocalHost());

            Lobby lobby = new Lobby(serverSpace);

            lobby.startLobby();

            //pass spillernavne og antal til spillet.
            GamePlay gPlay = new GamePlay(serverSpace, lobby.getNrOfPlayers());

            lobby.getPlayerNames();

        } else if (pickedOption.contains("Client")){

            System.out.println("Running a Client");

            Client cl = new Client();

            cl.startClient();

        } else if (pickedOption.contains("Combo")){

            System.out.println("Running a Server/Client Combo");

            Server serverSpace = new Server("ServerSpace");

            System.out.println("The server is running on : " + InetAddress.getLocalHost());

            Thread client = new Thread(new Client());

            client.start();

            Lobby lobby = new Lobby(serverSpace);

            lobby.startLobby();

            //pass spillernavne og antal til spillet.
            GamePlay gPlay = new GamePlay(serverSpace, lobby.getNrOfPlayers());

            lobby.getPlayerNames();
        }


    }
}
