package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class LobbyChatAgent implements Runnable{

    private SequentialSpace lobbySpace;
    private ArrayList<String> players;

    LobbyChatAgent(SequentialSpace lobbySpace, ArrayList<String> players){
        this.lobbySpace = lobbySpace;
        this.players = players;
    }

    @Override
    public void run() {

        while (true) {
            try {
                // [0] update code, [1] name of the one writing the message, [2] the message
                Object[] tuple = lobbySpace.get(new ActualField("Chat"), new FormalField(String.class), new FormalField(String.class));

                System.out.println("Receive chat update from : " + tuple[1]);
                for (String user : players) {

                    if (!user.equals(tuple[1])) {
                        String s = (String) tuple[1];
                        s = s.substring(0, s.indexOf("#"));
                        String finalText = s + " : " + tuple[2];
                        // [0] lobby code, [1] chat code, [2] name of the receiving player, [3] text message combined with sending username
                        lobbySpace.put(Lobby.LOBBY_UPDATE, Lobby.CHAT_MESSAGE, user, finalText);
                    }
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Chat Agent interrupted");
                break;
            }
        }

    }
}
