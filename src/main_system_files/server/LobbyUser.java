package main_system_files.server;

import javax.crypto.Cipher;

public class LobbyUser {

    public String userID;
    public int lobbyAgentNo;
    public Cipher personalCipher;
    public int userNr;

    LobbyUser (String userID, int lobbyAgentNo, Cipher personalCipher, int userNr){
        this.userID = userID;
        this.lobbyAgentNo = lobbyAgentNo;
        this.personalCipher = personalCipher;
        this.userNr = userNr;
    }

}
