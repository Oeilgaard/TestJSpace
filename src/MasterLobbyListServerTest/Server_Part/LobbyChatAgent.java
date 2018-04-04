package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.ArrayList;

public class LobbyChatAgent implements Runnable{

    private SequentialSpace lobbySpace;
    private ArrayList<String> players;
    private ArrayList<Integer> threadIds;
    private Cipher cipher;

    LobbyChatAgent(SequentialSpace lobbySpace, ArrayList<String> players, ArrayList<Integer> threadIds, Cipher cipher){
        this.lobbySpace = lobbySpace;
        this.players = players;
        this.threadIds = threadIds;
        this.cipher = cipher;
    }

    @Override
    public void run() {

        while (true) {
            try {
                // [0] update code, [1] name of the one writing the message, [2] the message
                Object[] tuple = lobbySpace.get(new ActualField("Chat"), new FormalField(SealedObject.class));

                String decryptedMessage = (String) ((SealedObject)tuple[1]).getObject(cipher);

                String field1 = decryptedMessage.substring(0,decryptedMessage.indexOf('!'));
                String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!')+1,decryptedMessage.length());

                System.out.println("Receive chat update from : " + field1);
                for (String user : players) {

                    if (!user.equals(field1)) {
                        String s = field1;
                        s = s.substring(0, s.indexOf("#"));
                        String finalText = s + " : " + field2;
                        // [0] lobby code, [1] chat code, [2] name of the receiving player, [3] text message combined with sending username
                        System.out.println("Sending chat message to " + s + " with thread id " + threadIds.get(players.indexOf(user)));
                        lobbySpace.put(Lobby.LOBBY_UPDATE, Lobby.CHAT_MESSAGE, user, finalText, threadIds.get(players.indexOf(user)));
                    }
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Chat Agent interrupted");
                break;
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}
