package LobbyTesting;

import org.jspace.ActualField;
import org.jspace.FormalField;

import java.net.ConnectException;

public class LobbyDisconnectManager implements Runnable {

    Server server;

    public LobbyDisconnectManager(Server server) {
        this.server = server;
    }

    @Override
    public void run() {

        serverLoop:
        while(true) {

            try {
                Object[] tuple = server.GetServerSpace().get(new ActualField("Leave"), new FormalField(String.class));

                if (tuple == null) {
                    System.out.println("Tuplen er null");
                } else if (tuple[0].equals("Leave")){
                    Lobby.removeNrOfPlayer();
                    Lobby.removePlayerName((String) tuple[1]);
                }

            } catch (InterruptedException e) {
                //System.out.println("Thread Interrupted");
            }

        }
    }
}
