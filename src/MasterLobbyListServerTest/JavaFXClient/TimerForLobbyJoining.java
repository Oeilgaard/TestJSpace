package MasterLobbyListServerTest.JavaFXClient;

public class TimerForLobbyJoining implements Runnable{

    private static int connectionSucces = 0;

    private Model model;

    TimerForLobbyJoining(Model model){
        this.model = model;
    }

    @Override
    public void run() {

        Thread attemptAtConnecting = new Thread(new LookForConnectionAccept(model));
        attemptAtConnecting.start();

        try {
            int secondsBeforeFailure = 5;
            for (int i = 0; i < secondsBeforeFailure; i++) {
                Thread.sleep(1000);
                if (connectionSucces == 2) {

                    //Success!
                    model.changeResponseFromLobby(2);
                    model.getServerResponseMonitor().okay();
                    break;
                } else if (connectionSucces == 1) {

                    //Server is full
                    model.changeResponseFromLobby(1);
                    model.getServerResponseMonitor().okay();
                    break;
                }
            }

            if(connectionSucces == 0){

                attemptAtConnecting.interrupt();
                //reager på at serveren ikke er tilgængelig
                model.getServerResponseMonitor().okay();

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void lobbyConnectionSuccess(int i){
        connectionSucces = i;
    }
}
