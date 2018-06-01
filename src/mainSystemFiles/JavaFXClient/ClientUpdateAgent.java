package mainSystemFiles.JavaFXClient;

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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientUpdateAgent implements Runnable{

    private Model model;
    private Parent root;
    private int threadId;

    ClientUpdateAgent(Model model, Parent root, int threadId){
        this.model = model;
        this.root = root;
        this.threadId = threadId;
    }

    @Override
    public void run() {

        boolean running = true;

        while (running) {
            try {

                // [0] update code [1] type of update [2] name of user [3] chat text combined with username (situational) [4] thread id [5] index in lobby
                Object[] tuple = model.getLobbySpace().get(new ActualField(Model.LOBBY_UPDATE), new FormalField(Integer.class), new FormalField(String.class),new ActualField(threadId), new ActualField(model.getIndexInLobby()));
                System.out.println("got the tuple!: " + tuple[1]);

                if ((int)tuple[1] == Model.CHAT_MESSAGE) {
                    Platform.runLater(() -> {

                        Label chatText = new Label((String) tuple[2]);
                        chatText.setWrapText(true);
                        //chatText.prefWidth(254);

                        ((VBox) root.lookup("#vb1")).getChildren().add(chatText);
                        ((ScrollPane) root.lookup("#scroll")).setVvalue(1.0);
                    });
                } else if ((int)tuple[1] == Model.CONNECT ||(int) tuple[1]==Model.LOBBY_DISCONNECT) {
                    //Tuple 1 - 3 sealed object

                    String messageToBeEncrypted = "" + Model.GET_PLAYERLIST + "!" + model.getUserID() + "?" + -1 + "=*¤";

                    SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getLobbyCipher());
                    SealedObject filler = new SealedObject("filler",model.getLobbyCipher());

                    model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage, filler);

                    Platform.runLater(() -> {
                        Object[] tuple2;
                        try {

                            // [0] response code [1] list of playernames [2] username
                            tuple2 = model.getLobbySpace().get(new ActualField(Model.LOBBY_RESP), new FormalField(ArrayList.class), new ActualField(model.getIndexInLobby()));

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
                } else if ((int)tuple[1]==Model.CLOSE) {

                    System.out.println("DETECTED A SHUTDOWN");
                    model.setInLobby(false);

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
                                tuple = model.getLobbyListSpace().queryAll(new ActualField(Model.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

                                for (Object[] obj : tuple) {
                                    updItems.add(obj[1]);
                                    Controller.lobbyIds.add((UUID) obj[2]);
                                }

                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    running = false;
                } else if ((int)tuple[1] == Model.BEGIN) {

                    model.setInGame(true);
                    model.setInLobby(false);
                    model.currentSceneIsGameScene = true;

                    Platform.runLater(new Runnable() {
                        public void run() {
                            try {
                                model.currentRoot = FXMLLoader.load(getClass().getResource(Controller.GAME_SCENE + ".fxml"));
                                Scene scene = new Scene(model.currentRoot);
                                Main.appWindow.setScene(scene);

                                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                                label.setText(model.getUserID().substring(0, model.getUserID().indexOf("#")));


                                VBox vb = ((VBox) model.currentRoot.lookup("#vb1"));
                                ScrollPane sp = ((ScrollPane) model.currentRoot.lookup("#scroll"));
                                vb.getChildren().clear();
                                for (String message : model.actionHistory) {
                                    Label chatText = new Label(message);
                                    chatText.setWrapText(true);
                                    //chatText.prefWidth(184);

                                    vb.getChildren().add(chatText);
                                    sp.setVvalue(1.0);
                                }

                                SealedObject encryptedMessage = new SealedObject(model.getUserID() + "!" + 2, model.getLobbyCipher());

                                model.getLobbySpace().put(Model.TARGETS_REQUEST,encryptedMessage);
                                Object[] tuple = model.getLobbySpace().get(new ActualField(Model.TARGETS_RESPONSE), new FormalField(SealedObject.class), new ActualField(model.getIndexInLobby()));

                                ListView updatePlayerListView = ((ListView) model.currentRoot.lookup("#listOfPlayers"));
                                ObservableList updItems = updatePlayerListView.getItems();
                                updItems.clear();

                                String[] listOfnames = (String[]) ((SealedObject)tuple[1]).getObject(model.personalCipher);

                                for (int i = 0; i < 4; i++) {
                                    if (!listOfnames[i].equals(""))
                                        updItems.add(i + ". " + listOfnames[i]);
                                }

                                Controller.gameAgent = new Thread(new ClientGameUpdate(model));
                                Controller.gameAgent.start();

                                model = null;
                                root = null;
                            } catch (IOException | InterruptedException | ClassNotFoundException | BadPaddingException | IllegalBlockSizeException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    running = false;
                } else if((int)tuple[1] == Model.NOT_ENOUGH_PLAYERS){
                    Platform.runLater(() -> {

                        Label chatText = new Label("You need 2-4 players to start the game.");
                        chatText.setWrapText(true);
                        //chatText.prefWidth(254);

                        ((VBox) root.lookup("#vb1")).getChildren().add(chatText);
                        ((ScrollPane) root.lookup("#scroll")).setVvalue(1.0);
                    });
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Den er blevet catched!");
                model = null;
                root = null;
                return;
            } catch (IOException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }

    }
}
