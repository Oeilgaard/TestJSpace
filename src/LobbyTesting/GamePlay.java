package LobbyTesting;

public class GamePlay {

    Server serverSpace;
    int nrOfPlayers;

    GamePlay (Server serverSpace, int nrOfPlayers){
        this.serverSpace = serverSpace;
        this.nrOfPlayers = nrOfPlayers;

        System.out.println("Spillet er nu igang");
    }


}
