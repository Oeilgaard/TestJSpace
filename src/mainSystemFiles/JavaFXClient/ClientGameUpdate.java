package mainSystemFiles.JavaFXClient;

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
import javafx.scene.paint.Color;
import org.jspace.ActualField;
import org.jspace.FormalField;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ClientGameUpdate implements Runnable{

    private Model model;

    ClientGameUpdate(Model model){
        this.model = model;
    }

    @Override
    public void run() {

        while (true) {

            try {
                Object[] tuple = model.getLobbySpace().get(new ActualField(Model.CLIENT_UPDATE), new FormalField(SealedObject.class), new ActualField(model.getIndexInLobby()));

                String decryptedMessage = (String) ((SealedObject)tuple[1]).getObject(model.personalCipher);

                String field1text = decryptedMessage.substring(0,decryptedMessage.indexOf('!'));
                int field1 = Integer.parseInt(field1text);
                String field2 = decryptedMessage.substring(decryptedMessage.indexOf('!')+1,decryptedMessage.indexOf('?'));
                String field3 = decryptedMessage.substring(decryptedMessage.indexOf('?')+1,decryptedMessage.indexOf('='));
                String field4 = decryptedMessage.substring(decryptedMessage.indexOf('=')+1,decryptedMessage.length());

                if (field1 == Model.NEW_TURN) {

                    //If not empty, you have drawn a card, i.e. it's your turn
                    if (!field2.equals("")) {
                        model.currentSceneIsGameScene = false;

                        Platform.runLater(new Runnable() {
                            public void run() {

                                //Update GUI
                                try {
                                    Parent root = FXMLLoader.load(getClass().getResource("PlayCardScene.fxml"));
                                    Scene scene = new Scene(root);
                                    Main.appWindow.setScene(scene);

                                    model.cardsOnHand.add((String) field2);
                                    Controller.loadHand(model.cardsOnHand, root);

                                    model.actionHistory.add((String) field3);

                                    VBox vb = ((VBox) root.lookup("#vb1playcard"));
                                    ScrollPane sp = ((ScrollPane) root.lookup("#scrollplaycard"));
                                    vb.getChildren().clear();
                                    int everyOtherCounter = 0;
                                    for (String message : model.actionHistory){
                                        Label chatText = new Label(message);
                                        if(everyOtherCounter == 1) {
                                            chatText.setTextFill(Color.web("#0F487F"));
                                            everyOtherCounter = 0;
                                        } else {
                                            everyOtherCounter = 1;
                                        }
                                        chatText.setWrapText(true);

                                        vb.getChildren().add(chatText);
                                        sp.setVvalue(1.0);
                                    }

                                    Label label = (Label)root.lookup("#usernameLabel");
                                    label.setText(model.getUserID().substring(0, model.getUserID().indexOf("#")));

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {

                        Label chatText = new Label((String) field3);
                        chatText.setTextFill(Color.web("#0F487F"));
                        chatText.setWrapText(true);

                        Platform.runLater(() -> {

                            //Update GUI to show whos turn it is
                            ((VBox) model.currentRoot.lookup("#vb1")).getChildren().add(chatText);
                            ((ScrollPane) model.currentRoot.lookup("#scroll")).setVvalue(1.0);

                            model.actionHistory.add((String) field3);
                        });

                    }

                    SealedObject encryptedMessage = new SealedObject(model.getUserID() + "!" + 2,model.getLobbyCipher());

                    model.getLobbySpace().put(Model.TARGETS_REQUEST,encryptedMessage);
                    Object[] tuplename = model.getLobbySpace().get(new ActualField(Model.TARGETS_RESPONSE), new FormalField(SealedObject.class), new ActualField(model.getIndexInLobby()));

                    String[] listOfNames = (String[]) ((SealedObject)tuplename[1]).getObject(model.personalCipher);

                    Platform.runLater(() -> {
                                ListView updatePlayerListView = ((ListView) model.currentRoot.lookup("#listOfPlayers"));
                                updatePlayerListView.getItems().clear();
                                for (int i = 0; i < 4; i++) {
                                    if (!listOfNames[i].equals("")) {
                                        updatePlayerListView.getItems().add(i + ". " + listOfNames[i]);
                                    }
                                }
                            }
                    );


                } else if (field1 == Model.GAME_START_UPDATE) {

                    model.cardsOnHand.add((String) field2);

                    ImageView card1 = ((ImageView) model.currentRoot.lookup("#cur_card"));
                    //System.out.println(model.cardsOnHand.get(0).toLowerCase());
                    card1.setImage(new Image("resources/" + model.cardsOnHand.get(0).toLowerCase() + ".jpg"));


                    Label chatText = new Label((String) field3);
                    chatText.setWrapText(true);
                    //chatText.prefWidth(184);

                    Platform.runLater(() -> {

                                ((VBox) model.currentRoot.lookup("#vb1")).getChildren().add(chatText);
                                ((ScrollPane) model.currentRoot.lookup("#scroll")).setVvalue(1.0);

                                model.actionHistory.add((String) field3);
                            }
                    );

                } else if (field1 == Model.OUTCOME ) {

                    Label chatText = new Label((String) field3);
                    chatText.setWrapText(true);

                    Platform.runLater(() -> {

                                ((VBox) model.currentRoot.lookup("#vb1")).getChildren().add(chatText);
                                ((ScrollPane) model.currentRoot.lookup("#scroll")).setVvalue(1.0);

                                model.actionHistory.add((String) field3);
                            }
                    );


                    switch ((String) field2) {
                        case "KING":
                            if (!field4.equals("")) {
                                model.cardsOnHand.remove(0);
                                model.cardsOnHand.add((String) field4);
                                ImageView card1 = ((ImageView) model.currentRoot.lookup("#cur_card"));
                                card1.setImage(new Image("resources/" + model.cardsOnHand.get(0).toLowerCase() + ".jpg")); //mainSystemFiles/
                            }
                            break;
                        case "PRINCE":
                            if (!field4.equals("")) {
                                model.cardsOnHand.remove(0);
                                model.cardsOnHand.add((String) field4);
                                ImageView card1 = ((ImageView) model.currentRoot.lookup("#cur_card"));
                                card1.setImage(new Image("resources/" + model.cardsOnHand.get(0).toLowerCase() + ".jpg")); //mainSystemFiles/
                            }
                            break;
                    }

                } else if (field1 == Model.KNOCK_OUT) {

                    Label chatText = new Label((String) field3);
                    chatText.setWrapText(true);
                    //chatText.prefWidth(184);
                    SealedObject encryptedMessage = new SealedObject(model.getUserID() + "!" + 2,model.getLobbyCipher());

                    model.getLobbySpace().put(Model.TARGETS_REQUEST,encryptedMessage);
                    Object[] tuplename = model.getLobbySpace().get(new ActualField(Model.TARGETS_RESPONSE), new FormalField(SealedObject.class), new ActualField(model.getIndexInLobby()));

                    String[] listOfNames = (String[]) ((SealedObject)tuplename[1]).getObject(model.personalCipher);

                    Platform.runLater(() -> {
                        //Update GUI to tell who has been knocked out
                        ((VBox) model.currentRoot.lookup("#vb1")).getChildren().add(chatText);
                        ((ScrollPane) model.currentRoot.lookup("#scroll")).setVvalue(1.0);

                        model.actionHistory.add((String) field3);

                        if (model.currentSceneIsGameScene) {
                            ListView updatePlayerListView = ((ListView) model.currentRoot.lookup("#listOfPlayers"));
                            updatePlayerListView.getItems().clear();
                            for (int i = 0; i < 4; i++) {
                                if (!listOfNames[i].equals("")) {
                                    updatePlayerListView.getItems().add(i + ". " + listOfNames[i]);
                                }
                            }
                        }
                    });

                } else if (field1 == Model.WIN) {

                    String lastMessage = model.actionHistory.get(model.actionHistory.size()-1);
                    model.actionHistory.clear();
                    model.cardsOnHand.clear();
                    Label chatText = new Label((String) field2);
                    chatText.setWrapText(true);
                    //chatText.prefWidth(184);

                    SealedObject encryptedMessage = new SealedObject(model.getUserID() + "!" + 2,model.getLobbyCipher());
                    model.getLobbySpace().put(Model.TARGETS_REQUEST,encryptedMessage);
                    Object[] tuplename = model.getLobbySpace().get(new ActualField(Model.TARGETS_RESPONSE), new FormalField(SealedObject.class), new ActualField(model.getIndexInLobby()));

                    String[] listOfNames = (String[]) ((SealedObject)tuplename[1]).getObject(model.personalCipher);

                    Platform.runLater(() -> {
                        //Update GUI to tell who has been knocked out
                        ((VBox) model.currentRoot.lookup("#vb1")).getChildren().clear();
                        ((VBox) model.currentRoot.lookup("#vb1")).getChildren().add(chatText);
                        ((ScrollPane) model.currentRoot.lookup("#scroll")).setVvalue(1.0);

                        model.actionHistory.add(lastMessage);
                        model.actionHistory.add((String) field2);

                        if (model.currentSceneIsGameScene) {
                            ListView updatePlayerListView = ((ListView) model.currentRoot.lookup("#listOfPlayers"));
                            updatePlayerListView.getItems().clear();
                            for (int i = 0; i < 4; i++) {
                                if (!listOfNames[i].equals("")) {
                                    updatePlayerListView.getItems().add(i + ". " + listOfNames[i]);
                                }
                            }
                        }
                    });

                } else if (field1 == Model.GAME_ENDING){

                    model.currentSceneIsGameScene = false;

                    model.setInGame(false);

                    JOptionPane.showMessageDialog(new JFrame(),"The game is over! \n" + field2 + " has won the game"  );

                    Platform.runLater(() -> {

                        try {
                            Parent root = FXMLLoader.load(getClass().getResource("LobbyListScene.fxml"));

                            model.currentRoot = root;
                            Scene scene = new Scene(root);
                            Main.appWindow.setScene(scene);

                            Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                            label.setText(model.getUserID().substring(0,model.getUserID().indexOf("#")));

                            ListView updateListView = ((ListView) root.lookup("#lobbyList"));
                            updateListView.getItems().clear();
                            Controller.lobbyIds.clear();

                            //[0] lobby code [1] Lobby name [2] Lobby ID
                            List<Object[]> tuple2 = model.getLobbyListSpace().queryAll(new ActualField(Model.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
                            for (Object[] obj : tuple2) {
                                updateListView.getItems().add(obj[1]);
                                Controller.lobbyIds.add((UUID) obj[2]);
                            }

                            model.actionHistory.clear();

                            model.setIsLeader(false);

                            model.resetLobbyInfo();

                            model = null;

                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                    break;
                } else if (field1 == Model.ACTION_DENIED){
                    model.currentSceneIsGameScene = false;

                    Platform.runLater(() -> {
                        try {
                            Parent root = FXMLLoader.load(getClass().getResource("PlayCardScene.fxml"));
                            model.currentRoot = root;
                            Scene scene = new Scene(root);
                            Main.appWindow.setScene(scene);

                            model.cardsOnHand.clear();

                            model.cardsOnHand.add((String) field3);
                            model.cardsOnHand.add((String) field4);
                            Controller.loadHand(model.cardsOnHand, root);

                            model.actionHistory.add((String) field2);

                            VBox vb = ((VBox) root.lookup("#vb1playcard"));
                            ScrollPane sp = ((ScrollPane) root.lookup("#scrollplaycard"));
                            vb.getChildren().clear();
                            for (String message : model.actionHistory){
                                Label chatText = new Label(message);
                                chatText.setWrapText(true);

                                vb.getChildren().add(chatText);
                                sp.setVvalue(1.0);
                            }

                            Label label = (Label)root.lookup("#usernameLabel");
                            label.setText(model.getUserID().substring(0, model.getUserID().indexOf("#")));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    });
                } else if(field1 == Model.GAME_DISCONNECT){
                    model.setInGame(false);

                    JOptionPane.showMessageDialog(new JFrame(),"The game is over as a player disconnected");

                    model.currentSceneIsGameScene = false;

                    Platform.runLater(() -> {

                        try {
                            Parent root = FXMLLoader.load(getClass().getResource("LobbyListScene.fxml"));

                            model.currentRoot = root;
                            Scene scene = new Scene(root);
                            Main.appWindow.setScene(scene);

                            Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                            label.setText(model.getUserID().substring(0,model.getUserID().indexOf("#")));

                            ListView updateListView = ((ListView) root.lookup("#lobbyList"));
                            updateListView.getItems().clear();
                            Controller.lobbyIds.clear();

                            //[0] lobby code [1] Lobby name [2] Lobby ID
                            List<Object[]> tuple2 = model.getLobbyListSpace().queryAll(new ActualField(Model.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
                            for (Object[] obj : tuple2) {
                                updateListView.getItems().add(obj[1]);
                                Controller.lobbyIds.add((UUID) obj[2]);
                            }

                            model.actionHistory.clear();

                            model.setIsLeader(false);

                            model.resetLobbyInfo();

                            model = null;

                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }

                    });
                    break;

                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Den er blevet catched!");
                break;
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}