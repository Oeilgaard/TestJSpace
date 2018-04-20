package MasterLobbyListServerTest.StressTesting;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class FilledLobbyTest {

    public static int nrOfClients = 200;

    public static int nrOfLobbies;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {

        nrOfLobbies = nrOfClients / 4 + (((nrOfClients % 4) != 0) ? 1 : 0);

        System.out.println("Testing the server with : " + nrOfClients + " clients and " + nrOfLobbies + " lobbies");

        RemoteSpace requestSpace = new RemoteSpace("tcp://localhost:25565/requestSpace?keep");
        RemoteSpace lobbyListSpace = new RemoteSpace("tcp://localhost:25565/lobbyOverviewSpace?keep");
        RemoteSpace responseSpace = new RemoteSpace("tcp://localhost:25565/responseSpace?keep");

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        Key clientKey = kg.generateKey();

        Object[] tuple = requestSpace.query(new FormalField(PublicKey.class));

        Cipher serverCipher = Cipher.getInstance("RSA");
        serverCipher.init(Cipher.ENCRYPT_MODE, (PublicKey) tuple[0]);

        SealedObject[] encryptedUserNameStrings = new SealedObject[nrOfClients];

        SealedObject encryptedKey = new SealedObject(clientKey, serverCipher);

        for (int i = 0; i < nrOfClients; i++) {
            String name = "testClient" + i + "!";
            encryptedUserNameStrings[i] = new SealedObject(name, serverCipher);
        }

        for (int i = 0; i < nrOfClients; i++) {

            requestSpace.put(1, 12, encryptedUserNameStrings[i], encryptedKey);

        }

        for (int i = 0; i < nrOfClients; i++) {
            responseSpace.get(new ActualField(2), new ActualField(23), new FormalField(SealedObject.class));
        }

        SealedObject[] encryptedLobbyNames = new SealedObject[nrOfLobbies];

        for (int i = 0; i < nrOfLobbies; i++) {
            String name = "lobbyname" + i + "!testClient" + i * 4 + "#123";
            encryptedLobbyNames[i] = new SealedObject(name, serverCipher);
        }

        System.out.println("0) Requesting the creation of the lobbies");
        for (int i = 0; i < nrOfLobbies; i++) {
            requestSpace.put(1, 11, encryptedLobbyNames[i], encryptedKey);
            Thread.sleep(1000);
        }

        System.out.println("1) Cleaning the responses");
        for (int i = 0; i < nrOfLobbies; i++) {
            responseSpace.get(new ActualField(2), new FormalField(SealedObject.class));
        }

        System.out.println("2) Waiting for the lobbies to start on the server");
        Thread.sleep(120 * nrOfClients);

        System.out.println("3) Making connection to the lobbies now");
        List<Object[]> tuplelist = lobbyListSpace.queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace[] lobbySpaces = new RemoteSpace[tuplelist.size()];
        Cipher[] lobbyCiphers = new Cipher[tuplelist.size()];
        SealedObject[] encryptedKeyForLobbies = new SealedObject[tuplelist.size()];

        int y = 0;
        for (Object[] obj : tuplelist) {
            lobbySpaces[y] = new RemoteSpace("tcp://localhost:25565/" + obj[2] + "?keep");
            Object[] tupleLobbyKey = lobbySpaces[y].query(new FormalField(PublicKey.class));
            lobbyCiphers[y] = Cipher.getInstance("RSA");
            lobbyCiphers[y].init(Cipher.ENCRYPT_MODE, (PublicKey) tupleLobbyKey[0]);
            encryptedKeyForLobbies[y] = new SealedObject(clientKey, lobbyCiphers[y]);
            y++;
        }

        System.out.println("4) Sending connection tuples to the lobbies");
        // 30, (31, name, 0), (key)
        SealedObject[] connectEncryption = new SealedObject[nrOfClients];

        outerpart:
        for (int i = 0; i < tuplelist.size(); i++) {
            for (int k = 0; k < 4; k++) {
                if ((i * 4) + k == connectEncryption.length) {
                    break outerpart;
                }
                connectEncryption[((i * 4) + k)] = new SealedObject("31!testClient" + ((i * 4) + k) + "#123?" + 0, lobbyCiphers[i]);
            }
        }

        outerpart:
        for (int i = 0; i < tuplelist.size(); i++) {
            for (int k = 0; k < 4; k++) {
                if ((i * 4) + k == connectEncryption.length) {
                    break outerpart;
                }
                lobbySpaces[i].put(30, connectEncryption[((i * 4) + k)], encryptedKeyForLobbies[i]);
            }
        }

        System.out.println("5) Cleaning up the responses from the connections");
        outerpart:
        for (int i = 0; i < tuplelist.size(); i++) {
            for (int k = 0; k < 4; k++) {
                if ((i * 4) + k == connectEncryption.length) {
                    break outerpart;
                }
                lobbySpaces[i].get(new ActualField(40), new FormalField(Integer.class), new FormalField(SealedObject.class));
            }
        }

        int nrOfRunningLobbies = nrOfLobbies;
        System.out.println("6) The nr of running lobbies is now :" + nrOfRunningLobbies);
        System.out.println("7) Sending begin requests");

        SealedObject[] encryptedBeginMsg = new SealedObject[nrOfLobbies];

        SealedObject[] fillers = new SealedObject[nrOfLobbies];

        for (int i = 0; i < nrOfLobbies; i++){
            fillers[i] = new SealedObject("filler",lobbyCiphers[i]);
            encryptedBeginMsg[i] = new SealedObject("33!testClient" + i*4 + "#123?-1", lobbyCiphers[i]);
        }

        for (int i = 0; i < nrOfLobbies;i++){
            lobbySpaces[i].put(30, encryptedBeginMsg[i], fillers[i]);
        }

        boolean[] lobbyStoppedRunning = new boolean[nrOfLobbies];

        Cipher clientCipher = Cipher.getInstance("AES");
        clientCipher.init(Cipher.DECRYPT_MODE,clientKey);

        System.out.println("8) Running gameplay for clients");

        playingLoop:
        while (true) {
            //Recognise who has the turn tuple
            for (int i = 0; i < nrOfLobbies; i++) {
                if (!lobbyStoppedRunning[i]) {
                    clientLoop:
                    for (int k = 0; k < 4; k++) {
                        System.out.println("Client : " + (i*4 + k));
                        Object[] tupleForClient;
                        //TODO sæt et flag og klar trækket efter alles tupler er tjekket
                        while (true) {
                            tupleForClient = lobbySpaces[i].get(new ActualField(10), new FormalField(SealedObject.class), new ActualField(k));
                            String decryptedNewRound = (String) ((SealedObject) tupleForClient[1]).getObject(clientCipher);
                            String field1text = decryptedNewRound.substring(0, decryptedNewRound.indexOf('!'));
                            int field1 = Integer.parseInt(field1text);
                            String field2 = decryptedNewRound.substring(decryptedNewRound.indexOf('!') + 1, decryptedNewRound.indexOf('?'));
                            System.out.println(" Client : " + (i*4+k) + " found at field1 : " + field1);
                            if (field1 == 11) {
                                System.out.println("Its a new turn");
                                if (!field2.equals("")) {
                                    System.out.println("Found the turn!");
                                    lobbySpaces[i].getAll(new ActualField(10), new FormalField(SealedObject.class), new FormalField(Integer.class));
                                    //This players turn
                                    takeAction(lobbySpaces[i], "testClient" + (i * 4 + k), k, lobbyCiphers[i], clientCipher);
                                    break clientLoop;
                                }
                                break;
                            } else if (field1 == 18) {
                                //Game is ending
                                lobbyStoppedRunning[i] = true;
                                nrOfRunningLobbies--;
                                System.out.println("Lobby closed. Nr. of lobbies is now : " + nrOfRunningLobbies);
                                if(nrOfRunningLobbies == 0){
                                    break playingLoop;
                                }
                                break clientLoop;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("DONE");
    }

    public static void takeAction(RemoteSpace lobbyspace, String clientName, int clientNr, Cipher lobbyCipher, Cipher clientCipher) throws IOException, IllegalBlockSizeException, InterruptedException, BadPaddingException, ClassNotFoundException {
        Random rn = new Random();
        outerloop:
        while(true){
            int playedCard = rn.nextInt(2);
            int target = rn.nextInt(4);
            int guessNr = rn.nextInt(7) + 1;

            String messageToBeEncrypted = "12!" + clientName + "#123" + "?" + playedCard + "=" + target + "*" + guessNr;
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,lobbyCipher);

            lobbyspace.put(20, encryptedMessage); // Send the action to the server

            innerloop:
            while(true) {
                Object[] tuple = lobbyspace.get(new ActualField(10), new FormalField(SealedObject.class), new ActualField(clientNr));

                String decryptedNewRound = (String) ((SealedObject) tuple[1]).getObject(clientCipher);
                String field1text = decryptedNewRound.substring(0, decryptedNewRound.indexOf('!'));

                if (field1text.equals("13")) {
                    System.out.println(clientName + " had success playing");
                    //Thread.sleep(1000);
                    break outerloop;
                } else if (field1text.equals("11")) {
                    System.out.println("FOUND A NEW TURN TUPLE ?!?");
                } else if (field1text.equals("17")) {
                    break innerloop;
                }
            }
        }
    }
}