package MasterLobbyListServerTest.JavaFXClient;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientGameUpdate implements Runnable{

    private Model model;
    private Parent root;

    public ClientGameUpdate(Model model, Parent root){
        this.model = model;
        this.root = root;
    }

    @Override
    public void run() {

        updateLoop:
        while(true) {
            try {

                Object[] tuple = model.getLobbySpace().get(new ActualField("game update"));

                if (tuple[1].equals("action1")) {
                    Platform.runLater(new Runnable() {
                        public void run() {

                            //Update GUI
                        }
                    });
                } else if (tuple[1].equals("action2")){

                    //Reveal information about player

                    Platform.runLater(new Runnable() {
                        public void run() {

                        }
                    });
                } else if (tuple[1].equals("action3")){

                    //Your turn

                } else if (tuple[1].equals("action4")){

                    Platform.runLater(new Runnable() {
                        public void run() {

                        }
                    });

                }

            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Den er blevet catched!");
                break updateLoop;
            }
        }

    }
}
