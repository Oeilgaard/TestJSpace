package common.src.main;

import org.jspace.*;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Scanner;

public class ChatTestClient {

    public static void main(String[] argv) throws InterruptedException, IOException {

        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.println("Write messages to other users: ");


        // SKRIV EN NY IP HER TIL SERVEREN
        RemoteSpace RemoteSp = new RemoteSpace("tcp://localhost:25565/SpaceToTarget?conn");


        System.out.println("Size of the Table before put : " + RemoteSp.size());

        String n = reader.nextLine(); // Scans the next token of the input as an String.

        while(true){

            System.out.println("Test Print");

            RemoteSp.put("From Client : ", n);

            System.out.println("Size of the Table after put : " + RemoteSp.size());

            Object[] tuple = RemoteSp.get(new FormalField(String.class),new FormalField(String.class));

            System.out.println("Message: " + tuple[0] + " " + tuple[1]);

            n = reader.nextLine(); // Scans the next token of the input as an string.

        }
    }

}
