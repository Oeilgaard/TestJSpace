package main_system_files.client;

public class LobbyConnectionTimer implements Runnable{

    private static int connectionSucces = Model.NO_RESPONSE;

    private Model model;

    LobbyConnectionTimer(Model model){
        this.model = model;
    }

    @Override
    public void run() {

        Thread attemptAtConnecting = new Thread(new LobbyConnectionResponseSeeker(model));
        attemptAtConnecting.start();

        try {
            int secondsBeforeFailure = 5;
            for (int i = 0; i < secondsBeforeFailure; i++) {
                Thread.sleep(1000);
                if (connectionSucces == Model.OK) {
                    System.out.println("Connection succes");
                    //Success!
                    model.changeResponseFromLobby(Model.OK);
                    model.getServerResponseMonitor().okay();
                    model.setInLobby(true);
                    break;
                } else if (connectionSucces == Model.BAD_REQUEST) {
                    //Server is full
                    model.changeResponseFromLobby(Model.BAD_REQUEST);
                    model.getServerResponseMonitor().okay();
                    break;
                }
            }

            if(connectionSucces == Model.NO_RESPONSE){

                attemptAtConnecting.interrupt();
                // The server does not respond
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
