package MasterLobbyListServerTest.Server_Part;

import javax.crypto.Cipher;

public class LobbyUser {

    public String name;
    public int threadNr;
    public Cipher personalCipher;
    public int userNr;

    LobbyUser (String name, int threadNr,Cipher personalCipher, int userNr){
        this.name = name;
        this.threadNr = threadNr;
        this.personalCipher = personalCipher;
        this.userNr = userNr;
    }

}
