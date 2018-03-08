package MasterLobbyListServerTest.Server_Part;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class PlayerInfo {

    static int maxPlayerNr;
    static int nrOfPlayers = 0;
    static ArrayList<String> playerNames;

    PlayerInfo(int maxPlayerNr){
        this.maxPlayerNr = maxPlayerNr;
        playerNames = new ArrayList<String>();
    }

    public void removePlayer(String username, SequentialSpace lobbySpace) throws InterruptedException {
        for (int i = 0; i < maxPlayerNr;i++){
            if(playerNames.contains(username)){
                playerNames.remove(username);
                nrOfPlayers--;
                System.out.println("Removed Player : " + username);
                lobbySpace.get(new ActualField("playerField"),new FormalField(Integer.class), new ActualField(username));
                return;
            }
        }
    }

    public void addPlayer(String username, SequentialSpace lobbySpace) throws InterruptedException {
        if(!playerNames.contains(username)){
            playerNames.add(username);
            nrOfPlayers++;
            System.out.println("Added Player : " + username);
            lobbySpace.put("playerField", playerNames.indexOf(username), username);
            return;
        }
    }

    public ArrayList<String> getOtherPlayers(String username){
        ArrayList<String> listOfUser = new ArrayList<String>();
        for (String Currentusername : playerNames){
            if(!Currentusername.equals(username)){
                listOfUser.add(Currentusername);
            }
        }
        return listOfUser;
    }


}
