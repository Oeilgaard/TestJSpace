package MasterLobbyListServerTest.Server_Part;

import java.util.ArrayList;

public class GameplayDummy {

    private ArrayList<String> players;

    public GameplayDummy(ArrayList<String> players){
        this.players = players;
    }

    public void runGamePlay(){
        System.out.println("Playing Love Letter...");
    }

}
