package LobbyTesting;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

public class LobbyAcceptManager implements Runnable {

    Server server;
    int maxnrOfPlayer;

    public LobbyAcceptManager(Server server, int maxnrOfPlayer) {
        this.server = server;
        this.maxnrOfPlayer = maxnrOfPlayer;
    }

    @Override
    public void run() {

        serverLoop:
        while(true) {

            try {
                Object[] tuple = server.GetServerSpace().get(new ActualField("Connection"), new FormalField(String.class));

                if (tuple == null) {
                    System.out.println("Tuplen er null");
                } else if (tuple[0].equals("Connection")){
                    Lobby.addNrOfPlayer();
                    Lobby.addPlayerName(Lobby.getNrOfPlayers() - 1,(String) tuple[1]);
                }

            } catch (InterruptedException e) {
                //System.out.println("Thread Interrupted");
            }

            if (Lobby.getNrOfPlayers() == maxnrOfPlayer){
                server.GetServerSpace().put("Begin");
                break serverLoop;
            }

        }
    }
}
