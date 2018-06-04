package main_system_files.server.gameplay;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;

public class PossibleTargetsThread implements Runnable{

    SequentialSpace lobbySpace;
    Model model;
    Cipher cipher;

    public PossibleTargetsThread(SequentialSpace lobbySpace, Model model, Cipher cipher){
        this.lobbySpace = lobbySpace;
        this.model = model;
        this.cipher = cipher;
    }

    @Override
    public void run() {
        while(true) try {

            Object[] tuple = lobbySpace.get(new ActualField(Model.TARGETS_REQUEST), new FormalField(SealedObject.class));

            String[] targetsAndReceiver = {"", "", "", "", "", ""};

            String decryptedMessage = (String) ((SealedObject) tuple[1]).getObject(cipher);

            String field1 = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));

            targetsAndReceiver[5] = field1;

            String field2String = decryptedMessage.substring(decryptedMessage.indexOf('!') + 1, decryptedMessage.length());
            int field2 = Integer.parseInt(field2String);

            int index = 0;

            if ((Integer) field2 == 0) {
                for (Player p : model.players) {
                    if (p.isInRound() && !p.isHandMaidProtected() && !p.isMe((String) field1)) {
                        String s = p.getuserID();
                        s = s.substring(0, s.indexOf("#"));
                        targetsAndReceiver[index] = s;
                    }
                    index++;
                }
            } else if ((Integer) field2 == 1) {
                for (Player p : model.players) {
                    if (p.isInRound() && !p.isHandMaidProtected()) {
                        String s = p.getuserID();
                        s = s.substring(0, s.indexOf("#"));
                        targetsAndReceiver[index] = s;
                    }
                    index++;
                }
            } else if ((Integer) field2 == 2) {
                for (Player p : model.players) {
                    if (p.isInRound()) {
                        String s = p.getuserID();
                        s = s.substring(0, s.indexOf("#"));
                        targetsAndReceiver[index] = s + " : Alive";
                    } else {
                        String s = p.getuserID();
                        s = s.substring(0, s.indexOf("#"));
                        targetsAndReceiver[index] = s + " : K.O.";
                    }
                    index++;
                }
            }
            SealedObject encryptedMessage = new SealedObject(targetsAndReceiver, model.getUserfromUserID(field1).getPlayerCipher());

            lobbySpace.put(Model.TARGETS_RESPONSE, encryptedMessage, model.getUserfromUserID(field1).getPlayerIndex());

        } catch (InterruptedException e) {
            //e.printStackTrace();
            break;
        } catch (BadPaddingException | IllegalBlockSizeException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
