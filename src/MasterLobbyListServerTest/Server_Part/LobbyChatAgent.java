package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class LobbyChatAgent implements Runnable{

    private SequentialSpace lobbySpace;
    private PlayerInfo playerInfo;

    public LobbyChatAgent(SequentialSpace lobbySpace, PlayerInfo playerInfo){
        this.lobbySpace = lobbySpace;
        this.playerInfo = playerInfo;
    }

    @Override
    public void run() {

        while(true) {
            try {
                Object[] tuple = lobbySpace.get(new ActualField("Chat"), new FormalField(String.class), new FormalField(String.class));

                System.out.println("Receive chat update from : " + tuple[1]);
                ArrayList<String> userToUpdate = playerInfo.getOtherPlayers((String) tuple[1]);
                for (String user : userToUpdate) {
                    lobbySpace.put("ChatUpdate",tuple[1],tuple[2],user);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
