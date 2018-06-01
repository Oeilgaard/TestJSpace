package mainSystemFiles.JavaFXClient;

import org.jspace.ActualField;
import org.jspace.FormalField;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;

public class LookForConnectionAccept implements Runnable {

    private Model model;

    LookForConnectionAccept(Model model){
        this.model = model;
    }

    @Override
    public void run() {
        try {
            while(true) {
                Object[] tuple = model.getLobbySpace().query(new ActualField(Model.LOBBY_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

                if (tuple != null) {

                    try {
                        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(model.personalCipher);


                        String field1 = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
                        String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.indexOf('?'));
                        String field3 = decryptedMessage.substring(decryptedMessage.indexOf('?') + 1, decryptedMessage.length());

                        if (field1.equals(model.getUserID())) {

                            model.getLobbySpace().get(new ActualField(Model.LOBBY_RESP), new FormalField(Integer.class), new FormalField(SealedObject.class));

                            if (tuple[1].equals(Model.CONNECT_ACCEPTED)) {
                                TimerForLobbyJoining.lobbyConnectionSuccess(Model.OK);
                                model.setIndexInLobby(Integer.parseInt(field3));
                            } else {
                                TimerForLobbyJoining.lobbyConnectionSuccess(Model.BAD_REQUEST);
                            }
                            if (Boolean.parseBoolean(field2)) {
                                model.setIsLeader(true);
                            }

                            model = null;
                            break;

                        }
                    } catch (BadPaddingException e){
                        System.out.println("COULDNT BE DECRYPTED");
                        Thread.sleep(1000);
                    }
                }
            }

        } catch (InterruptedException | IllegalBlockSizeException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}