package main_system_files.client_simulation_testing;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class FilledLobbyThreadTest {
    public static String ip = "localhost"; //212.237.129.195

    public static int nrOfClients = 200;

    public static int nrOfLobbies;

    public static int counter;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        FilledLobbyThreadTest fltt = new FilledLobbyThreadTest();
        fltt.stressTest();
    }

    synchronized void stressTest() throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException {
        long startTime;
        long endTime;
        long startTimeMaster;
        long endTimeMaster;
        long startTimeInitiation;
        long endTimeInitiation;
        long startTimeGameplay;
        long endTimeGameplay;
        long[] usernametimes = new long[nrOfClients];
        long[] connecttimes = new long[nrOfClients];

        nrOfLobbies = nrOfClients / 4 + (((nrOfClients % 4) != 0) ? 1 : 0);

        System.out.println("Testing the server with : " + nrOfClients + " clients and " + nrOfLobbies + " lobbies");
        startTimeMaster = System.currentTimeMillis();
        startTimeInitiation = startTimeMaster;

        RemoteSpace requestSpace = new RemoteSpace("tcp://" + ip + ":25565/requestSpace?keep");
        RemoteSpace lobbyListSpace = new RemoteSpace("tcp://" + ip + ":25565/lobbyOverviewSpace?keep");
        RemoteSpace responseSpace = new RemoteSpace("tcp://" + ip + ":25565/responseSpace?keep");

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

            startTime = System.currentTimeMillis();
            requestSpace.put(1, 12, encryptedUserNameStrings[i], encryptedKey);

            responseSpace.get(new ActualField(2), new ActualField(23), new FormalField(SealedObject.class));
            endTime = System.currentTimeMillis();
            usernametimes[i] = endTime - startTime;
        }

        SealedObject[] encryptedLobbyNames = new SealedObject[nrOfLobbies];

        for (int i = 0; i < nrOfLobbies; i++) {
            String name = "lobbyname" + i + "!testClient" + i * 4 + "#12345678-1234-1234-1234-123456789012";
            encryptedLobbyNames[i] = new SealedObject(name, serverCipher);
        }

        System.out.println("0) Requesting the creation of the lobbies");
        for (int i = 0; i < nrOfLobbies; i++) {
            requestSpace.put(1, 11, encryptedLobbyNames[i], encryptedKey);
            Thread.sleep(1000);
        }

        System.out.println("1) Cleaning the responses");
        for (int i = 0; i < nrOfLobbies; i++) {
            responseSpace.get(new ActualField(2), new ActualField(25), new FormalField(SealedObject.class));
        }

        System.out.println("2) Waiting for the lobbies to start on the server");
        Thread.sleep(120 * nrOfClients);

        System.out.println("3) Making connection to the lobbies now");
        List<Object[]> tuplelist = lobbyListSpace.queryAll(new ActualField(90), new FormalField(String.class), new FormalField(UUID.class));

        RemoteSpace[] lobbySpaces = new RemoteSpace[tuplelist.size()];
        Cipher[] lobbyCiphers = new Cipher[tuplelist.size()];
        SealedObject[] encryptedKeyForLobbies = new SealedObject[tuplelist.size()];

        int y = 0;
        for (Object[] obj : tuplelist) {

            lobbySpaces[y] = new RemoteSpace("tcp://" + ip + ":25565/" + obj[2] + "?keep");

            Object[] tupleLobbyKey = lobbySpaces[y].query(new FormalField(PublicKey.class));
            lobbyCiphers[y] = Cipher.getInstance("RSA");
            lobbyCiphers[y].init(Cipher.ENCRYPT_MODE, (PublicKey) tupleLobbyKey[0]);
            encryptedKeyForLobbies[y] = new SealedObject(clientKey, lobbyCiphers[y]);
            y++;
        }

        System.out.println("4) Sending connection tuples to the lobbies");
        // 20, (31, name, 0), (key)
        SealedObject[] connectEncryption = new SealedObject[nrOfClients];

        outerpart:
        for (int i = 0; i < tuplelist.size(); i++) {
            for (int k = 0; k < 4; k++) {
                if ((i * 4) + k == connectEncryption.length) {
                    break outerpart;
                }
                connectEncryption[((i * 4) + k)] = new SealedObject("31!testClient" + ((i * 4) + k) + "#12345678-1234-1234-1234-123456789012?" + 0 + "=*¤", lobbyCiphers[i]);
            }
        }

        outerpart:
        for (int i = 0; i < tuplelist.size(); i++) {
            for (int k = 0; k < 4; k++) {
                if ((i * 4) + k == connectEncryption.length) {
                    break outerpart;
                }
                startTime = System.currentTimeMillis();
                lobbySpaces[i].put(20, connectEncryption[((i * 4) + k)], encryptedKeyForLobbies[i]);
                lobbySpaces[i].get(new ActualField(40), new FormalField(Integer.class), new FormalField(SealedObject.class));
                endTime = System.currentTimeMillis();
                connecttimes[i*4+k] = endTime - startTime;
            }
        }

        int nrOfRunningLobbies = nrOfLobbies;
        System.out.println("6) The nr of running lobbies is now :" + nrOfRunningLobbies);
        System.out.println("7) Sending begin requests");

        SealedObject[] encryptedBeginMsg = new SealedObject[nrOfLobbies];

        SealedObject[] fillers = new SealedObject[nrOfLobbies];

        for (int i = 0; i < nrOfLobbies; i++){
            fillers[i] = new SealedObject("filler",lobbyCiphers[i]);
            encryptedBeginMsg[i] = new SealedObject("33!testClient" + i*4 + "#12345678-1234-1234-1234-123456789012?-1=*¤", lobbyCiphers[i]);
        }

        for (int i = 0; i < nrOfLobbies;i++){
            lobbySpaces[i].put(20, encryptedBeginMsg[i], fillers[i]);
        }

        Cipher clientCipher = Cipher.getInstance("AES");
        clientCipher.init(Cipher.DECRYPT_MODE,clientKey);

        System.out.println("8) Running gameplay for clients");

        ArrayList<Thread> listOfThreads = new ArrayList<>();
        ArrayList<gameStressThread> stressObjects = new ArrayList<>();
        //Recognise who has the turn tuple
        System.out.println("Playing the game...");
        endTimeInitiation = System.currentTimeMillis();
        startTimeGameplay = endTimeInitiation;

        for (int i = 0; i < nrOfLobbies; i++) {
            stressObjects.add(new gameStressThread(this,clientCipher,lobbySpaces[i],lobbyCiphers[i],i));
            listOfThreads.add(new Thread(stressObjects.get(i)));
        }

        for (Thread thread : listOfThreads){
            thread.start();
        }

        counter = nrOfLobbies;
        //
        // LAV NOGET VENTE TING TING HER
        while(counter > 0) {
            //System.out.println("Lobby counter is now : " + counter);
            wait();
            //Thread.sleep(4000);
        }

        System.out.println("UDE AF WAIT LOOPET");
        //
        endTimeGameplay = System.currentTimeMillis();

        long usernametimesfinal = 0;
        long connectiontimesfinal = 0;
        for (int i = 0; i < nrOfClients;i++){
            usernametimesfinal += usernametimes[i];
            connectiontimesfinal += connecttimes[i];
        }
        usernametimesfinal = usernametimesfinal / nrOfClients;
        connectiontimesfinal = connectiontimesfinal / nrOfClients;

        ArrayList<Long> totaltimesForPlays = new ArrayList<>();

        for (int i = 0;i < nrOfLobbies;i++){
            totaltimesForPlays.addAll(stressObjects.get(i).getTimeList());
        }

        long finalPlaytimes = 0;
        for (int i = 0; i < totaltimesForPlays.size();i++){
            finalPlaytimes += totaltimesForPlays.get(i);
        }
        finalPlaytimes = finalPlaytimes / totaltimesForPlays.size();

        System.out.println("The results from the timers with " + nrOfClients + " number of clients");
        System.out.println("Username times : " + usernametimesfinal + "ms and Connect times : " + connectiontimesfinal + "ms and play time : " + finalPlaytimes + "ms with a size of " + totaltimesForPlays.size());
        System.out.println("");
        endTimeMaster = System.currentTimeMillis();
        System.out.println("Initiation time: " + (endTimeInitiation-startTimeInitiation) + " ms");
        System.out.println("Gameplay time: " + (endTimeGameplay-startTimeGameplay) + " ms");
        System.out.println("Total time: " + (endTimeMaster-startTimeMaster) + " ms");
        System.out.println("DONE");
        System.exit(1);
    }

    synchronized void sync(){
        counter--;
        System.out.println("Counter is now : " + counter);
        notify();
    }
}
