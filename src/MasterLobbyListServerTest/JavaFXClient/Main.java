package MasterLobbyListServerTest.JavaFXClient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    static Stage appWindow;

    public static void main(String[] args) {
        launch(args);
        System.out.println("Elegant closing");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        appWindow = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("JoinServerScene.fxml"));
        appWindow.setScene(new Scene(root));
        appWindow.show();
    }

}