package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

public class LobbyConnectionManager implements Runnable{

    private SequentialSpace lobbySpace;
    private PlayerInfo playerInfo;
    private String lobbyLeader;

    public LobbyConnectionManager(SequentialSpace lobbySpace, PlayerInfo playerInfo, String lobbyLeader){
        this.lobbySpace = lobbySpace;
        this.playerInfo = playerInfo;
        this.lobbyLeader = lobbyLeader;
    }

    @Override
    public void run() {

        while (true){

            Object[] tuple = null;

            try {
                // Indices:
                // 0: tuple type 1: true for joining, false for leaving 2: unique username
                tuple = lobbySpace.get(new ActualField("Connection"),new FormalField(Boolean.class),new FormalField(String.class));

                if(tuple != null && tuple[0].equals("Connection")){

                    if(tuple[1].equals(true)){

                        playerInfo.addPlayer((String)tuple[2]);

                    } else if (tuple[1].equals(false)){

                        playerInfo.removePlayer((String)tuple[2]);

                        if (tuple[2].equals(lobbyLeader)){

                            lobbySpace.put(Lobby.LOBBY_MESSAGE,"Closing");
                        }
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }

    }
}
