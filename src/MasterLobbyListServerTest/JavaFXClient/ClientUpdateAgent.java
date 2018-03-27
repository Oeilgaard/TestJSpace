package MasterLobbyListServerTest.JavaFXClient;

import javafx.application.Platform;
import javafx.collections.ObservableList;
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

public class ClientUpdateAgent implements Runnable{

    private Model model;
    private Parent root;

    ClientUpdateAgent(Model model, Parent root){
        this.model = model;
        this.root = root;
    }

    @Override
    public void run() {

        while (true) {
            try {

                //[0] update code [1] type of update [2] name of user [3] chat text combined with username (situational)
                Object[] tuple = model.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new FormalField(Integer.class), new ActualField(model.getUniqueName()), new FormalField(String.class));

                if (tuple[1].equals(Model.CHAT_MESSAGE)) {
                    Platform.runLater(() -> {

                        Label chatText = new Label((String) tuple[3]);
                        chatText.setWrapText(true);
                        chatText.prefWidth(254);

                        ((VBox) root.lookup("#vb1")).getChildren().add(chatText);
                        ((ScrollPane) root.lookup("#scroll")).setVvalue(1.0);
                    });
                } else if (tuple[1].equals(Model.CONNECT) || tuple[1].equals(Model.DISCONNECT)) {
                    model.getLobbySpace().put(Model.LOBBY_REQ, Model.GET_PLAYERLIST, model.getUniqueName());

                    Platform.runLater(() -> {
                        Object[] tuple2;
                        try {

                            // [0] response code [1] list of playernames [2] username
                            tuple2 = model.getLobbySpace().get(new ActualField(Model.LOBBY_RESP), new FormalField(ArrayList.class), new ActualField(model.getUniqueName()));


                            ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
                            ObservableList updItems = updatePlayerListView.getItems();
                            updItems.clear();
                            for (String user : (ArrayList<String>) tuple2[1]) {

                                updItems.add(new Label(user));

                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } else if (tuple[1].equals(Model.CLOSE)) {

                    System.out.println("DETECTED A SHUTDOWN");

                    //TODO burde de her change scene ting ikke ske i Controller for at 'seperate concern'?

                    model.resetLobbyInfo();

                    Platform.runLater(new Runnable() {
                        public void run() {
                            try {
                                model.currentRoot = FXMLLoader.load(getClass().getResource(Controller.LOBBY_LIST_SCENE));
                                Scene scene = new Scene(model.currentRoot);
                                Main.appWindow.setScene(scene);

                                ListView updateListView = ((ListView) model.currentRoot.lookup("#lobbyList"));
                                ObservableList updItems = updateListView.getItems();
                                updItems.clear();


                                Controller.lobbyIds.clear();
                                List<Object[]> tuple;

                                //[0] lobby code [1] Lobby name [2] Lobby ID
                                tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

                                for (Object[] obj : tuple) {
                                    updItems.add(obj[1]);
                                    Controller.lobbyIds.add((UUID) obj[2]);
                                }

                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    break;
                } else if (tuple[1].equals(Model.BEGIN)) {

                    Platform.runLater(new Runnable() {
                        public void run() {
                            try {
                                model.currentRoot = FXMLLoader.load(getClass().getResource("GameScene.fxml"));
                                Scene scene = new Scene(model.currentRoot);
                                Main.appWindow.setScene(scene);

                                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                                label.setText(model.getUniqueName().substring(0, model.getUniqueName().indexOf("#")));


                                VBox vb = ((VBox) model.currentRoot.lookup("#vb1"));
                                ScrollPane sp = ((ScrollPane) model.currentRoot.lookup("#scroll"));
                                vb.getChildren().clear();
                                for (String message : model.actionHistory) {
                                    Label chatText = new Label(message);
                                    chatText.setWrapText(true);
                                    chatText.prefWidth(184);

                                    vb.getChildren().add(chatText);
                                    sp.setVvalue(1.0);
                                }

                                model.getLobbySpace().put("TargetablePlayersRequest", model.getUniqueName(), 2);
                                Object[] tuple = model.getLobbySpace().get(new ActualField("TargetablePlayersResponse"), new ActualField(model.getUniqueName()), new FormalField(String[].class));

                                ListView updatePlayerListView = ((ListView) model.currentRoot.lookup("#listOfPlayers"));
                                ObservableList updItems = updatePlayerListView.getItems();
                                updItems.clear();

                                for (int i = 0; i < 5; i++) {
                                    if (!((String[]) tuple[2])[i].equals(""))
                                        updItems.add(i + ". " + ((String[]) tuple[2])[i]);
                                }

                                Controller.gameAgent = new Thread(new ClientGameUpdate(model));
                                Controller.gameAgent.start();
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
                }

            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Den er blevet catched!");
                break;
            }
        }

    }
}
