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

                Object[] tuple = model.getLobbySpace().get(new ActualField(Model.CLIENT_UPDATE),new FormalField(Integer.class),new ActualField(model.getUniqueName()),new FormalField(String.class),new FormalField(String.class),new FormalField(String.class));

                if (tuple[1].equals(Model.NEW_TURN)) {

                    if(!tuple[3].equals("")) {

                        Platform.runLater(new Runnable() {
                            public void run() {

                                //Update GUI
                                try {
                                    Parent root = FXMLLoader.load(getClass().getResource( "PlayCardScene.fxml"));
                                    Scene scene = new Scene(root);
                                    Main.appWindow.setScene(scene);

                                    model.cardsOnHand.add((String)tuple[3]);
                                    Controller.loadHand(model.cardsOnHand, root);
                                    System.out.println("Hand : " + model.cardsOnHand.get(0) + " and " + model.cardsOnHand.get(1));

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {

                        Platform.runLater(new Runnable() {
                            public void run() {

                                //Update GUI to show whos turn it is
                            }
                        });

                    }
                } else if (tuple[1].equals(Model.GAME_START_UPDATE)){

                    System.out.println("New card for the new round");
                    model.cardsOnHand.add((String)tuple[3]);

                } else if (tuple[1].equals(Model.OUTCOME)){

                    System.out.println("Outcome of card : " + tuple[3]);

                    model.actionHistory.add((String)tuple[4]);

                    switch ((String)tuple[3]){
                        case "KING":
                            if(!tuple[5].equals("")){
                                System.out.println("Someone used king and you switched : " + model.cardsOnHand.get(0) + " for " + tuple[5]);
                                model.cardsOnHand.remove(0);
                                model.cardsOnHand.add((String)tuple[5]);
                            }
                            break;
                        case "PRINCE":
                            if(!tuple[5].equals("")){
                                System.out.println("You were targeted by prince and got : " + tuple[5]);
                                model.cardsOnHand.remove(0);
                                model.cardsOnHand.add((String)tuple[5]);
                            }
                            break;
                    }

                } else if (tuple[1].equals(Model.KNOCK_OUT)){

                    Platform.runLater(new Runnable() {
                        public void run() {

                        }
                    });

                } else if (tuple[1].equals(Model.WIN)){
                    System.out.println("The round is over");
                    model.cardsOnHand.clear();
                }

            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Den er blevet catched!");
                break updateLoop;
            }
        }

    }
}
