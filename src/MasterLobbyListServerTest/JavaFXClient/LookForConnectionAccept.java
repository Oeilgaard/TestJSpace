package MasterLobbyListServerTest.JavaFXClient;

import org.jspace.ActualField;
import org.jspace.FormalField;

public class LookForConnectionAccept implements Runnable {

    private Model model;

    public LookForConnectionAccept(Model model){
        this.model = model;
    }

    @Override
    public void run() {
        try {
            Object[] tuple = model.getLobbySpace().get(new ActualField(model.RESPONSE_CODE),new ActualField(model.getUniqueName()),new FormalField(Boolean.class));

            if((Boolean) tuple[2]){
                TimerForLobbyJoining.lobbyConnectionSuccess(2);
            } else {
                TimerForLobbyJoining.lobbyConnectionSuccess(1);
            }

        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }
}
