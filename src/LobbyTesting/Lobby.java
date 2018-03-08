package LobbyTesting;

import org.jspace.ActualField;

public class Lobby {

    public static int nrOfPlayer = 0;

    public static final int maxPlayerNr = 5;

    public static String[] playerNames = new String[5];

    Server serverSpace;

    Thread conManager;
    Thread leaveManager;

    Lobby (Server serverSpace){
        this.serverSpace = serverSpace;
    }

    public void startLobby() throws InterruptedException {
        System.out.println("Hey");

        conManager = new Thread( new LobbyAcceptManager(serverSpace,maxPlayerNr));
        leaveManager = new Thread( new LobbyDisconnectManager(serverSpace));

        conManager.start();
        leaveManager.start();

        BeginLoop:
        while(true) {
            System.out.println("Waiting for begin");
            serverSpace.GetServerSpace().get(new ActualField("Begin"));

            if(nrOfPlayer >= 2){
                System.out.println("Ready to Begin!");
                break BeginLoop;
            } else {
                System.out.println("Not enough players to begin");
            }
        }

        conManager.interrupt();
        leaveManager.interrupt();

    }

    public static int getNrOfPlayers(){
        return nrOfPlayer;
    }

    public static void addNrOfPlayer(){
        System.out.println("addNrOfPlayer was called");
        nrOfPlayer++;
    }

    public static void addPlayerName(int nrForPlayer, String playerName){
        playerNames[nrForPlayer] = playerName;
    }

    public static void getPlayerNames(){
        int i = 0;
        System.out.println("\nPlayerList: ");
        for (String s : playerNames){
            i++;
            System.out.println("Player " + i + ": " + s);
        }
    }

    public static void removeNrOfPlayer(){
        System.out.println("removeNrOfPlayer was called");
        nrOfPlayer--;
    }

    public static void removePlayerName(String playername){
        for (int i = 0; i < playerNames.length; i++){
            if(playerNames[i].contains(playername)){
                playerNames[i] = null;
                break;
            }
        }
    }
}
