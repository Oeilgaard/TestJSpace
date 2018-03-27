package MasterLobbyListServerTest.JavaFXClient;

import org.jspace.ActualField;
import org.jspace.FormalField;

public class LookForConnectionAccept implements Runnable {

    private Model model;

    LookForConnectionAccept(Model model){
        this.model = model;
    }

    @Override
    public void run() {
        try {
            Object[] tuple = model.getLobbySpace().get(new ActualField(Model.LOBBY_RESP),new FormalField(Integer.class),new ActualField(model.getUniqueName()), new FormalField(Boolean.class));

            if(tuple[1].equals(Model.CONNECT_ACCEPTED)){
                TimerForLobbyJoining.lobbyConnectionSuccess(2);
            } else {
                TimerForLobbyJoining.lobbyConnectionSuccess(1);
            }
            if((Boolean)tuple[3]){
                model.leaderForCurrentLobby = true;
            }

        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }
}
