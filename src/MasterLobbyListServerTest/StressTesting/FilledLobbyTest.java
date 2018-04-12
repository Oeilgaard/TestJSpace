package MasterLobbyListServerTest.StressTesting;

import MasterLobbyListServerTest.JavaFXClient.Model;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class FilledLobbyTest {

    public static int nrOfClients = 100;

    public static int nrOfLobbies;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InterruptedException, IllegalBlockSizeException {

        nrOfLobbies = nrOfClients / 5 + (((nrOfClients % 5) != 0) ? 1 : 0);
        System.out.println(nrOfLobbies);

        RemoteSpace requestSpace = new RemoteSpace("tcp://localhost:25565/requestSpace?keep");
        RemoteSpace lobbyListSpace = new RemoteSpace("tcp://localhost:25565/lobbyOverviewSpace?keep");
        RemoteSpace responseSpace = new RemoteSpace("tcp://localhost:25565/responseSpace?keep");

        KeyGenerator kg = KeyGenerator.getInstance("AES");
        Key clientKey = kg.generateKey();

        //KeyGenerator kg = KeyGenerator.getInstance("AES");
        //Key clientKey = kg.generateKey();
        //Cipher clientCipher = Cipher.getInstance("AES");
        //clientCipher.init(Cipher.DECRYPT_MODE,clientKey);

        Object[] tuple = requestSpace.query(new FormalField(PublicKey.class));

        Cipher serverCipher = Cipher.getInstance("RSA");
        serverCipher.init(Cipher.ENCRYPT_MODE, (PublicKey) tuple[0]);

        SealedObject[] encryptedUserNameStrings = new SealedObject[nrOfClients];

        SealedObject encryptedKey = new SealedObject(clientKey,serverCipher);

        for(int i = 0; i < nrOfClients; i++) {
            String name = "testClient" + i + "!";
            encryptedUserNameStrings[i] = new SealedObject( name, serverCipher);
        }

        for(int i = 0; i < nrOfClients; i++) {

            requestSpace.put(1, 12, encryptedUserNameStrings[i], encryptedKey);

        }
        /*
        for(int i = 0; i < nrOfClients; i++) {
            responseSpace.get(new ActualField(2), new ActualField(23), new FormalField(SealedObject.class));
        }
        */
        System.out.println("DONE");

    }
}