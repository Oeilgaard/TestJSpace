package mainSystemFiles.Server_Part;

import javax.crypto.Cipher;

public class LobbyUser {

    public String userID;
    public int threadNr;
    public Cipher personalCipher;
    public int userNr;

    LobbyUser (String userID, int threadNr,Cipher personalCipher, int userNr){
        this.userID = userID;
        this.threadNr = threadNr;
        this.personalCipher = personalCipher;
        this.userNr = userNr;
    }

}
