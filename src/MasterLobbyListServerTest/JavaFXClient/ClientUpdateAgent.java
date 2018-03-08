package MasterLobbyListServerTest.JavaFXClient;

import MasterLobbyListServerTest.Server_Part.Lobby;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static MasterLobbyListServerTest.JavaFXClient.Controller.LOBBY_LIST_SCENE;

public class ClientUpdateAgent implements Runnable{

    private Model model;
    private Parent root;

    public ClientUpdateAgent(Model model, Parent root){
        this.model = model;
        this.root = root;
    }

    @Override
    public void run() {

        updateLoop:
        while(true) {
            try {

                Object[] tuple = model.getLobbySpace().get(new ActualField(model.LOBBY_UPDATE),new FormalField(Integer.class), new ActualField(model.getUniqueName()), new FormalField(String.class));

                if (tuple[1].equals(model.CHAT_MESSAGE)) {
                    Platform.runLater(new Runnable() {
                        public void run() {

                            Label chatText = new Label((String)tuple[3]);
                            chatText.setWrapText(true);
                            chatText.prefWidth(254);

                            ((VBox) root.lookup("#vb1")).getChildren().add(chatText);
                            ((ScrollPane) root.lookup("#scroll")).setVvalue(1.0);
                        }
                    });
                } else if (tuple[1].equals(model.CONNECT) || tuple[1].equals(model.DISCONNECT)){
                    model.getLobbySpace().put(model.LOBBY_REQ,model.GET_PLAYERLIST,model.getUniqueName());

                    Platform.runLater(new Runnable() {
                        public void run() {
                            Object[] tuple2 = new Object[0];
                            try {
                                tuple2 = model.getLobbySpace().get(new ActualField(model.LOBBY_RESP),new FormalField(ArrayList.class),new ActualField(model.getUniqueName()));


                                ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
                                updatePlayerListView.getItems().clear();
                                for (String user : (ArrayList<String>) tuple2[1]) {
                                    String s = user;
                                    s = s.substring(0, s.indexOf("#"));
                                    updatePlayerListView.getItems().add(new Label(s));

                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else if (tuple[1].equals(model.CLOSE)){

                    System.out.println("DETECTED A SHUTDOWN");

                    model.resetLobbyInfo();

                    Platform.runLater(new Runnable() {
                        public void run() {
                            Parent root = null;
                            try {
                                root = FXMLLoader.load(getClass().getResource("LobbyListScene.fxml"));
                                Scene scene = new Scene(root);
                                Main.appWindow.setScene(scene);

                                ListView updateListView = ((ListView) root.lookup("#lobbyList"));

                                Controller.lobbyIds.clear();
                                updateListView.getItems().clear();
                                List<Object[]> tuple = null;
                                tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

                                for (Object[] obj : tuple) {
                                    updateListView.getItems().add(obj[1]);
                                    Controller.lobbyIds.add((UUID) obj[2]);
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    break updateLoop;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
