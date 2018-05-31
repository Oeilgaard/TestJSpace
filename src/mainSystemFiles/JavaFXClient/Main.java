package mainSystemFiles.JavaFXClient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Main extends Application {

    static Stage appWindow;

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        System.out.println("Starting main");
        launch(args);
        System.out.println("Elegant closing");
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        System.out.println("Start");
        appWindow = primaryStage;
        Parent root = FXMLLoader.load(getClass().getResource("JoinServerScene.fxml"));
        appWindow.setScene(new Scene(root));
        appWindow.show();
    }

    @Override
    public void stop() throws InterruptedException, IOException, IllegalBlockSizeException {
        Controller.sendDisconnectTuple();
    }

}