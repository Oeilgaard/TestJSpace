package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

public class LobbyConnectionManager implements Runnable{

    private SequentialSpace lobbySpace;
    private PlayerInfo playerInfo;

    public LobbyConnectionManager(SequentialSpace lobbySpace, PlayerInfo playerInfo){
        this.lobbySpace = lobbySpace;
        this.playerInfo = playerInfo;
    }

    @Override
    public void run() {

        while (true){

            Object[] tuple = null;

            try {
                tuple = lobbySpace.get(new ActualField("Connection"),new FormalField(Boolean.class),new FormalField(String.class));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(tuple != null && tuple[0].equals("Connection")){
                if(tuple[1].equals(true)){

                } else if (tuple[1].equals(false)){

                }
            }


        }

    }
}
