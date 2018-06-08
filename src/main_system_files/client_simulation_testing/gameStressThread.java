package main_system_files.client_simulation_testing;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class gameStressThread implements Runnable{

    private Cipher clientCipher;
    private RemoteSpace lobbySpace;
    private javax.crypto.Cipher lobbyCipher;
    private int i;
    private FilledLobbyThreadTest fltt;

    public ArrayList<Long> timesForPlays = new ArrayList<Long>();

    gameStressThread(FilledLobbyThreadTest fltt, Cipher clientCipher, RemoteSpace lobbySpace, javax.crypto.Cipher lobbyCipher, int i){
        this.clientCipher = clientCipher;
        this.lobbySpace = lobbySpace;
        this.lobbyCipher = lobbyCipher;
        this.i = i;
        this.fltt = fltt;
    }

    @Override
    public void run() {
        boolean lobbyStoppedRunning = false;

        outerLoop:
        while (!lobbyStoppedRunning) {
            /*forLOOP:
            for (int k = 0; k < 4; k++) {
                Object[] tupleForClient;

                clientLoop:
                while (true) {
                    try {
                        tupleForClient = lobbySpace.get(new ActualField(10), new FormalField(SealedObject.class), new ActualField(k));

                        String decryptedNewRound = (String) ((SealedObject) tupleForClient[1]).getObject(clientCipher);
                        String field1text = decryptedNewRound.substring(0, decryptedNewRound.indexOf('!'));
                        int field1 = Integer.parseInt(field1text);
                        String field2 = decryptedNewRound.substring(decryptedNewRound.indexOf('!') + 1, decryptedNewRound.indexOf('?'));
                        if (field1 == 11) {
                            if (!field2.equals("")) {
                                lobbySpace.getAll(new ActualField(10), new FormalField(SealedObject.class), new FormalField(Integer.class));
                                //This players turn
                                takeAction(lobbySpace, "testClient" + (i * 4 + k), k, lobbyCipher, clientCipher);
                                break forLOOP;
                            }
                            break clientLoop;
                        } else if (field1 == 18) {
                            //Game is ending
                            System.out.println("LOBBY number : " + i + " has closed");
                            break outerLoop;
                        }
                    } catch (InterruptedException | BadPaddingException | IOException | IllegalBlockSizeException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }*/
            try {
                List<Object[]> tuples = lobbySpace.getAll(new ActualField(10), new FormalField(SealedObject.class), new FormalField(Integer.class));
                for (Object[] tuple : tuples) {
                    String decryptedNewRound = (String) ((SealedObject) tuple[1]).getObject(clientCipher);
                    String field1text = decryptedNewRound.substring(0, decryptedNewRound.indexOf('!'));
                    int field1 = Integer.parseInt(field1text);
                    String field2 = decryptedNewRound.substring(decryptedNewRound.indexOf('!') + 1, decryptedNewRound.indexOf('?'));

                    if (field1 == 11 && !field2.equals("")) {
                        //This players turn
                        takeAction(lobbySpace, "testClient" + (i * 4 + (int) tuple[2]), (int)tuple[2], lobbyCipher, clientCipher);
                        break;
                    } else if (field1 == 18){
                        //Game is ending
                        System.out.println("LOBBY number : " + i + " has closed");
                        break outerLoop;
                    }
                }

            } catch (IOException | InterruptedException | BadPaddingException | IllegalBlockSizeException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        fltt.sync();
        System.out.println("Lobby DONE : " + i);
    }


    public void takeAction(RemoteSpace lobbyspace, String clientName, int clientNr, Cipher lobbyCipher, Cipher clientCipher) throws IOException, IllegalBlockSizeException, InterruptedException, BadPaddingException, ClassNotFoundException {
        Random rn = new Random();
        outerloop:
        while(true){
            int playedCard = rn.nextInt(2);
            int target = rn.nextInt(4);
            int guessNr = rn.nextInt(7) + 1;

            String messageToBeEncrypted = "12!" + clientName + "#12345678-1234-1234-1234-123456789012" + "?" + playedCard + "=" + target + "*" + guessNr + "¤";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);
            SealedObject filler = new SealedObject("filler",lobbyCipher);

            long startTime = System.currentTimeMillis();
            lobbyspace.put(20, encryptedMessage, filler); // Send the action to the server

            innerloop:
            while(true) {
                Object[] tuple = lobbyspace.get(new ActualField(10), new FormalField(SealedObject.class), new ActualField(clientNr));

                String decryptedNewRound = (String) ((SealedObject) tuple[1]).getObject(clientCipher);
                String field1text = decryptedNewRound.substring(0, decryptedNewRound.indexOf('!'));

                if (field1text.equals("13")) {
                    long endTime = System.currentTimeMillis();
                    timesForPlays.add(endTime - startTime);
                    break outerloop;
                } else if (field1text.equals("17")) {
                    long endTime = System.currentTimeMillis();
                    timesForPlays.add(endTime - startTime);
                    break innerloop;
                }
            }
        }
    }

    public ArrayList<Long> getTimeList(){
        return timesForPlays;
    }
}

