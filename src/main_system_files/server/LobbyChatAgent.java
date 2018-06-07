package main_system_files.server;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class LobbyChatAgent implements Runnable {

    private SequentialSpace lobbySpace;
    private ArrayList<LobbyUser> users;

    LobbyChatAgent(SequentialSpace lobbySpace, ArrayList<LobbyUser> users) {
        this.lobbySpace = lobbySpace;
        this.users = users;
    }

    @Override
    public void run() {

        while (true) {
            try {
                // [0] update code, [1] name of the one writing the message, [2] the message
                Object[] tuple = lobbySpace.get(new ActualField("Chat"), new FormalField(String.class));

                String field1 = (String) tuple[1];

                System.out.println("Receive chat update from : " + field1);
                for (LobbyUser user : users) {
                    // [0] lobby code, [1] chat code, [2] name of the receiving player, [3] text message combined with sending username
                    System.out.println("Sending chat message to " + user.userID + " with thread id " + user.lobbyAgentNo);
                    lobbySpace.put(Lobby.S2C_LOBBY, Lobby.CHAT_MESSAGE, field1, user.lobbyAgentNo, user.userNr);
                }

            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Chat Agent interrupted");
                break;
            }
        }
    }
}
