package MasterLobbyListServerTest.JavaFXClient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.SocketException;

public class Main extends Application {

    private static Controller controller;

    public static void main(String[] args) {
        launch(args);

        System.out.println("Elegant closing");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));

        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() throws InterruptedException {

        if(Controller.connectedToLobby) {
            Controller.sendDisconnectTuple();
        }
    }

}