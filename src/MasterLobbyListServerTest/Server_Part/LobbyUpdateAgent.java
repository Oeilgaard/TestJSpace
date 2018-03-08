package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class LobbyUpdateAgent implements Runnable{

    private SequentialSpace lobbySpace;
    private PlayerInfo playerInfo;

    public LobbyUpdateAgent(SequentialSpace lobbySpace, PlayerInfo playerInfo){
        this.lobbySpace = lobbySpace;
        this.playerInfo = playerInfo;
    }

    @Override
    public void run() {

        while(true) {
            try {
                Object[] tuple = lobbySpace.get(new ActualField("Update"), new FormalField(String.class), new FormalField(String.class), new FormalField(String.class));

                switch ((String) tuple[1]) {
                    case "chat":
                        System.out.println("Recieve chat update from : " + tuple[2]);
                        ArrayList<String> userToUpdate = playerInfo.getOtherPlayers((String) tuple[2]);
                        for (String user : userToUpdate) {
                            lobbySpace.put("LobbyUpdate","chat", user, tuple[3], tuple[2]);
                        }
                        break;

                }


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
