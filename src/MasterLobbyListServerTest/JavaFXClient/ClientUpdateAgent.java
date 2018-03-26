package MasterLobbyListServerTest.JavaFXClient;

import MasterLobbyListServerTest.Server_Part.Lobby;
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

                //[0] update code [1] type of update [2] name of user [3] chat text combined with username (situational)
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

                                // [0] response code [1] list of playernames [2] username
                                tuple2 = model.getLobbySpace().get(new ActualField(model.LOBBY_RESP),new FormalField(ArrayList.class),new ActualField(model.getUniqueName()));


                                ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
                                updatePlayerListView.getItems().clear();
                                for (String user : (ArrayList<String>) tuple2[1]) {

                                    updatePlayerListView.getItems().add(new Label(user));

                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else if (tuple[1].equals(model.CLOSE)){

                    System.out.println("DETECTED A SHUTDOWN");

                    //TODO burde de her change scene ting ikke ske i Controller for at 'seperate concern'?

                    model.resetLobbyInfo();

                    Platform.runLater(new Runnable() {
                        public void run() {
                            try {
                                model.currentRoot = FXMLLoader.load(getClass().getResource("LobbyListScene.fxml"));
                                Scene scene = new Scene(model.currentRoot);
                                Main.appWindow.setScene(scene);

                                ListView updateListView = ((ListView) model.currentRoot.lookup("#lobbyList"));

                                Controller.lobbyIds.clear();
                                updateListView.getItems().clear();
                                List<Object[]> tuple = null;

                                //[0] lobby code [1] Lobby name [2] Lobby ID
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
                } else if (tuple[1].equals(model.BEGIN)){

                    Platform.runLater(new Runnable() {
                        public void run() {
                            try {
                                model.currentRoot = FXMLLoader.load(getClass().getResource("GameScene.fxml"));
                                Scene scene = new Scene(model.currentRoot);
                                Main.appWindow.setScene(scene);

                                VBox vb = ((VBox) model.currentRoot.lookup("#vb1"));
                                ScrollPane sp = ((ScrollPane) model.currentRoot.lookup("#scroll"));
                                vb.getChildren().clear();
                                for (String message : model.actionHistory){
                                    Label chatText = new Label(message);
                                    chatText.setWrapText(true);
                                    chatText.prefWidth(184);

                                    vb.getChildren().add(chatText);
                                    sp.setVvalue(1.0);
                                }

                                model.getLobbySpace().put("TargetablePlayersRequest",model.getUniqueName(),2);
                                Object[] tuple = model.getLobbySpace().get(new ActualField("TargetablePlayersResponse"),new ActualField(model.getUniqueName()), new FormalField(ArrayList.class));

                                ListView updatePlayerListView = ((ListView) model.currentRoot.lookup("#listOfPlayers"));
                                updatePlayerListView.getItems().clear();
                                for (String user : (ArrayList<String>) tuple[2]) {

                                    updatePlayerListView.getItems().add(new Label(user));

                                }

                                Controller.gameAgent = new Thread(new ClientGameUpdate(model));
                                Controller.gameAgent.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break updateLoop;
                }

            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Den er blevet catched!");
                break updateLoop;
            }
        }

    }
}
