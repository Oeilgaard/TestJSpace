package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class LobbyConnectionManager implements Runnable{

    private SequentialSpace lobbySpace;
    private PlayerInfo playerInfo;
    private String lobbyLeader;

    private static final String CONNECTION = "Connection";

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
                // 0: request type type 1: true for joining, false for leaving 2: unique username

                tuple = lobbySpace.get(new ActualField(CONNECTION),new FormalField(Boolean.class),new FormalField(String.class));
                //

                if(tuple != null && tuple[0].equals(CONNECTION)){

                    if(tuple[1].equals(true)){

                        if(playerInfo.maxPlayerNr == playerInfo.nrOfPlayers){
                            lobbySpace.put(2,tuple[2],false);
                        } else {
                            playerInfo.addPlayer((String) tuple[2], lobbySpace);
                            lobbySpace.put(2, tuple[2], true);
                            ArrayList<String> userToUpdate = playerInfo.getOtherPlayers((String) tuple[2]);
                            for (String user : userToUpdate) {
                                lobbySpace.put("LobbyUpdate","connection", user, "join", tuple[2]);
                            }
                        }

                        // put 'join tuple' to user

                    } else if (tuple[1].equals(false)){

                        playerInfo.removePlayer((String)tuple[2],lobbySpace);
                        ArrayList<String> userToUpdate = playerInfo.getOtherPlayers((String) tuple[2]);
                        for (String user : userToUpdate) {
                            lobbySpace.put("LobbyUpdate","connection", user, "left", tuple[2]);
                        }

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
