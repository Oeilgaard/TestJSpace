package common.src.main;

import org.jspace.*;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ChatTestServer {

    public static void main(String[] argv) throws InterruptedException, IOException {

        SpaceRepository repository = new SpaceRepository();
        Space PrimarySpace = new SequentialSpace();
        repository.add("SpaceToTarget", PrimarySpace);
        repository.addGate("tcp://localhost:25565/?conn");

        System.out.println("Server is now ready to receive");

        Object[] tuple;

        PrimarySpace.put("Hej Client", "Fra server");

        while (true) {

            tuple = PrimarySpace.get(new ActualField("From Client : "), new FormalField(String.class));
            System.out.println((String) tuple[0] + tuple[1]);

        }

    }
}


