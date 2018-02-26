package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

public class LobbyConnectionManager implements Runnable{

    private SequentialSpace lobbySpace;

    public LobbyConnectionManager(SequentialSpace lobbySpace){
        this.lobbySpace = lobbySpace;
    }

    @Override
    public void run() {

        while (true){

            try {
                lobbySpace.get(new ActualField("Connection"),new FormalField(String.class),new FormalField(String.class));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}
