package MasterLobbyListServerTest.Server_Part;

public class PlayerInfo {

    int maxPlayerNr;
    int nrOfPlayers = 0;
    String[] playerNames;

    PlayerInfo(int maxPlayerNr){
        this.maxPlayerNr = maxPlayerNr;
        playerNames = new String[5];
    }

    public void removePlayer(String username){
        for (int i = 0; i < maxPlayerNr;i++){
            if(playerNames[i] == username){
                playerNames[i] = null;
                nrOfPlayers--;
                System.out.println("Removed Player : " + username);
                return;
            }
        }
    }

    public void addPlayer(String username){
        for (int i = 0; i < maxPlayerNr;i++){
            if(playerNames[i] == null){
                playerNames[i] = username;
                nrOfPlayers++;
                System.out.println("Added Player : " + username);
                return;
            }
        }
    }
}
