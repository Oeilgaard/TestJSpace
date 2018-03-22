package MasterLobbyListServerTest.JavaFXClient;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Controller {

    public static final String LOBBY_LIST_SCENE = "LobbyListScene";
    public static final String USER_NAME_SCENE = "UserNameScene";
    public static final String CREATE_LOBBY_SCENE = "CreateLobbyScene";
    public static final String LOADING_LOBBY_SCENE = "ConnectingToLobby";
    public static final String PLAY_CARD_SCENE = "PlayCardScene";
    public static final String PICK_PLAYER_SCENE = "PickPlayerScene";

    @FXML
    private ScrollPane scroll;
    @FXML
    private TextField chatTxtField;
    @FXML
    private VBox vb1;
    @FXML
    private ListView lobbyList;
    @FXML
    private ListView listOfPlayers;
    @FXML
    private TextField IP;
    @FXML
    private Button createUserNameButton;
    @FXML
    private TextField userName;
    @FXML
    private Label instructionsUserName;
    @FXML
    private Label lobbyTitle;
    @FXML
    private TextField lobbyName;
    @FXML
    private Button createLobbyButton;
    @FXML
    private Label instructionsLobbyName;
    @FXML
    private Pane cardListPane;
    @FXML
    private ImageView card1;
    @FXML
    private ImageView card2;
    @FXML
    private ListView targetablePlayers;


    protected static ArrayList<UUID> lobbyIds;
    private static Model model;
    public static Thread updateAgent;
    public static Thread gameAgent;

    public static Boolean connectedToLobby = false;

    public static int pickedCard = 2;
    public static boolean selectCardIsGuard = false;
    public static int indexOfTarget = -1;


    public void pickCardOne(MouseEvent mouseEvent) throws IOException, InterruptedException {
        System.out.println("Pick card one");
        System.out.println("Returned : " + model.cardsOnHand.get(0));
        if (HelperFunctions.isTargeted(model.cardsOnHand.get(0))) {
            changeScene(PICK_PLAYER_SCENE);
            pickedCard = 0;
            selectCardIsGuard = HelperFunctions.isGuard(model.cardsOnHand.get(0));
        } else {
            model.getLobbySpace().put(Model.SERVER_UPDATE, Model.DISCARD, model.getUniqueName(),"0","",""); // Send the action to the server
            model.cardsOnHand.remove(0);
            changeScene("GameScene");
        }
        // if card one is targeted
        // go to pick player scene
        // else next player's turn
    }

    public void pickCardTwo(MouseEvent mouseEvent) throws IOException, InterruptedException {
        System.out.println("Pick card two");
        System.out.println("Returned : " + model.cardsOnHand.get(1));
        if (HelperFunctions.isTargeted(model.cardsOnHand.get(1))) {
            changeScene(PICK_PLAYER_SCENE);
            pickedCard = 1;
            selectCardIsGuard = HelperFunctions.isGuard(model.cardsOnHand.get(1));
        } else {
            model.getLobbySpace().put(Model.SERVER_UPDATE, Model.DISCARD, model.getUniqueName(),"1","",""); // Send the action to the server
            model.cardsOnHand.remove(1);
            changeScene("GameScene");
        }
    }

    @FXML
    public void joinServer(ActionEvent event) throws IOException, InterruptedException {

        String urlForRemoteSpace = IP.getText();
        model = new Model();
        model.addIpToRemoteSpaces(urlForRemoteSpace);
        lobbyIds = new ArrayList<>();

        changeScene(USER_NAME_SCENE);
        //changeScene(PLAY_CARD_SCENE);
    }

    @FXML
    public void createUser(ActionEvent event) throws InterruptedException {

        String userNameString = userName.getText();

        if (HelperFunctions.validName(userNameString)) {

            createUserNameButton.setDisable(true);
            instructionsUserName.setText("");

            model.getRequestSpace().put(Model.REQUEST_CODE, Model.CREATE_USERNAME_REQ, userNameString, "");

            // Blocks until user receives unique username (due to 'get')
            // [0] response code [1] Response [2] Ok or error [3] Username of receiver [4] Username with ID
            Object[] tuple = model.getResponseSpace().get(new ActualField(Model.RESPONSE_CODE), new ActualField(Model.ASSIGN_UNIQUE_USERNAME_RESP),
                    new FormalField(Integer.class), new ActualField(userNameString), new FormalField(String.class));

            if ((int) tuple[2] == Model.OK) {
                model.setUniqueName((String) tuple[4]); // Setting the user's name
                System.out.println("Unique name:");
                System.out.println(model.getUniqueName());

                // Goto Lobby List
                try {
                    changeScene(LOBBY_LIST_SCENE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // should ideally never happen, however can happen if the sanity check is bypassed client-side
            } else if ((int) tuple[2] == Model.BAD_REQUEST) {
                instructionsUserName.setText("Server denied username. Please try again.");
                createUserNameButton.setDisable(false);
            }
        } else {
            instructionsUserName.setText("Please only apply alphabetic characters (between 2-15 characters).");
            createUserNameButton.setDisable(false);
        }
    }

    public void changeScene(String sceneName) throws IOException, InterruptedException {

        Parent root = FXMLLoader.load(getClass().getResource(sceneName + ".fxml"));
        model.currentRoot = root;
        Scene scene = new Scene(root);
        Main.appWindow.setScene(scene);

        if (sceneName == LOBBY_LIST_SCENE) {
            ListView updateListView = ((ListView) root.lookup("#lobbyList"));
            updateListView.getItems().clear();
            lobbyIds.clear();

            //[0] lobby code [1] Lobby name [2] Lobby ID
            List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
            for (Object[] obj : tuple) {
                updateListView.getItems().add(obj[1]);
                lobbyIds.add((UUID) obj[2]);
            }
        } else if (sceneName == PLAY_CARD_SCENE) {
            loadHand(model.cardsOnHand, root);
        } else if (sceneName.equals("GameScene")){
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

        } else if (sceneName == PICK_PLAYER_SCENE) {

            //TODO This needs to be updated

            ListView targetablePlayers = ((ListView) root.lookup("#targetablePlayers"));
            targetablePlayers.getItems().clear();

            //LOAD DUMMY PLAYER ARRAY
            ArrayList<ArrayList> dummyPlayers = new ArrayList();

            ArrayList<String> bob = new ArrayList();
            bob.add("Bob");
            bob.add("1");

            ArrayList<String> alice = new ArrayList();
            alice.add("Alice");
            alice.add("1");

            ArrayList<String> charles = new ArrayList();
            charles.add("Charles");
            charles.add("1");

            dummyPlayers.add(bob);
            dummyPlayers.add(alice);
            dummyPlayers.add(charles);

            //model.getLobbySpace().put(Model.GAMEPLAY_INFO, Model.PLAYERS_IN_ROUND, dummyPlayers);
            //LOAD DUMMY PLAYER END

            //Object[] tp = model.getLobbySpace().query(new ActualField(Model.GAMEPLAY_INFO), new ActualField(Model.PLAYERS_IN_ROUND), new FormalField(ArrayList.class));

            int index = 0;
            //for (ArrayList<String> p : (ArrayList<ArrayList>) tp[2]) {
            for (ArrayList<String> p : dummyPlayers) {
                if (p.get(1) == "1") {
                    targetablePlayers.getItems().add(index + ". " + p.get(0));
                }
                index++;
            }
        }
    }

    public static void loadHand(ArrayList<String> hand, Parent root) {
        ImageView card1 = ((ImageView) root.lookup("#card1"));
        card1.setImage(new Image("MasterLobbyListServerTest/JavaFXClient/resources/" + hand.get(0) + ".jpg"));
        ImageView card2 = ((ImageView) root.lookup("#card2"));
        card2.setImage(new Image("MasterLobbyListServerTest/JavaFXClient/resources/" + hand.get(1) + ".jpg"));
    }

    @FXML
    public void createLobby(ActionEvent event) throws InterruptedException {

        String lobbyNameString = lobbyName.getText();

        if (HelperFunctions.validName(lobbyNameString)) {
            createLobbyButton.setDisable(true);
            instructionsLobbyName.setText("");

            model.getRequestSpace().put(Model.REQUEST_CODE, Model.CREATE_LOBBY_REQ, lobbyNameString, model.getUniqueName());

            // Wait for server to be created
            // [0] response code [1] Ok or deny [2] username of receiver [3] ID for lobby
            Object[] tuple = model.getResponseSpace().get(new ActualField(Model.RESPONSE_CODE), new FormalField(Integer.class),
                    new ActualField(model.getUniqueName()), new FormalField(UUID.class));

            if ((int) tuple[1] == Model.OK) {
                try {
                    changeScene(LOBBY_LIST_SCENE);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if ((int) tuple[1] == Model.BAD_REQUEST) {
                instructionsLobbyName.setText("Server denied to create lobby. Please try again.");
                createLobbyButton.setDisable(false);
            }
        } else {
            instructionsLobbyName.setText("Please only apply alphabetic characters (between 2-15 characters).");
        }
    }

    @FXML
    public void queryServers(ActionEvent event) throws InterruptedException {
        lobbyList.getItems().clear();
        lobbyIds.clear();
        //[0] lobby code [1] Lobby name [2] Lobby ID
        List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));

        for (Object[] obj : tuple) {
            lobbyList.getItems().add(obj[1]);
            lobbyIds.add((UUID) obj[2]);
        }
    }

    @FXML
    public void goToCreateLobbyScene(ActionEvent event) throws InterruptedException {
        try {
            changeScene(CREATE_LOBBY_SCENE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendDisconnectTuple() throws InterruptedException {
        model.getLobbySpace().put(Model.LOBBY_REQ, Model.DISCONNECT, model.getUniqueName());
        connectedToLobby = false;
    }

    @FXML
    public void textToChat(ActionEvent e) throws InterruptedException {

        String text = chatTxtField.getText();

        String s = model.getUniqueName();
        s = s.substring(0, s.indexOf("#"));
        Label chatText = new Label(s + " : " + text);
        chatText.setWrapText(true);
        chatText.prefWidth(254);

        vb1.getChildren().add(chatText);
        chatTxtField.clear();
        scroll.setVvalue(1.0);

        model.getLobbySpace().put("Chat", model.getUniqueName(), text);
    }


    //TODO: implement Join-lobby button for highlighted choice
    public void clickLobby(javafx.scene.input.MouseEvent mouseEvent) throws InterruptedException, IOException {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {

                Object[] tuple = null;

                int index = lobbyList.getSelectionModel().getSelectedIndex();

                System.out.println("Double clicked join: " + lobbyList.getSelectionModel().getSelectedItem()
                        + " index: " + index);

                // Query the desired lobby-tuple (non-blocking)

                //[0] lobby code [1] lobby name [2] lobby id
                tuple = model.getLobbyListSpace().queryp(new ActualField("Lobby"),
                        new ActualField(lobbyList.getSelectionModel().getSelectedItem()),
                        new ActualField(lobbyIds.get(index)));

                if (tuple != null) {

                    model.joinLobby((UUID) tuple[2]);

                    model.getLobbySpace().put(Model.LOBBY_REQ, Model.CONNECT, model.getUniqueName());

                    Thread tryToJoinLobby = new Thread(new TimerForLobbyJoining(model, this));
                    tryToJoinLobby.start();

                    changeScene(LOADING_LOBBY_SCENE);

                    model.getServerResponseMonitor().sync();

                }

                switch (model.getResponseFromLobby()) {
                    case 0:
                        changeScene(LOBBY_LIST_SCENE);
                        model.changeResponseFromLobby(0);
                        break;
                    case 1:
                        changeScene(LOBBY_LIST_SCENE);
                        model.changeResponseFromLobby(0);
                        break;
                    case 2:

                        Parent root = FXMLLoader.load(getClass().getResource("LobbyScene.fxml"));
                        Scene scene = new Scene(root);
                        Main.appWindow.setScene(scene);

                        ((Label) root.lookup("#lobbyTitle")).setText("Lobby name : " + lobbyList.getSelectionModel().getSelectedItem());
                        updatePlayerLobbyList(root);

                        connectedToLobby = true;
                        if (model.leaderForCurrentLobby) {
                            ((Button) root.lookup("#beginButton")).disableProperty().setValue(false);
                        }

                        updateAgent = new Thread(new ClientUpdateAgent(model, root));
                        updateAgent.start();

                        model.changeResponseFromLobby(0);
                        break;
                }
            }
        }
    }

    @FXML
    public void enterIPField(javafx.scene.input.KeyEvent keyEvent) throws IOException, InterruptedException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            String urlForRemoteSpace = IP.getText();
            model = new Model();
            model.addIpToRemoteSpaces(urlForRemoteSpace);
            lobbyIds = new ArrayList<>();

            changeScene(USER_NAME_SCENE);
        }
    }

    @FXML
    public void requestNameVhaEnter(javafx.scene.input.KeyEvent keyEvent) throws InterruptedException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            createUser(null);
        }
    }

    @FXML
    public void createLobbyVhaEnter(javafx.scene.input.KeyEvent keyEvent) throws InterruptedException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            String lobbyNameString = lobbyName.getText();

            if (HelperFunctions.validName(lobbyNameString)) {
                createLobbyButton.setDisable(true);
                instructionsLobbyName.setText("");

                model.getRequestSpace().put(Model.REQUEST_CODE, Model.CREATE_LOBBY_REQ, lobbyNameString, model.getUniqueName());

                // Wait for server to be created
                // [0] response code [1] Ok or deny [2] username of receiver [3] ID for lobby

                Object[] tuple = model.getResponseSpace().get(new ActualField(Model.RESPONSE_CODE), new FormalField(Integer.class),
                        new ActualField(model.getUniqueName()), new FormalField(UUID.class));

                if ((int) tuple[1] == Model.OK) {
                    try {
                        changeScene(LOBBY_LIST_SCENE);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if ((int) tuple[1] == Model.BAD_REQUEST) {
                    instructionsLobbyName.setText("Server denied to create lobby. Please try again.");
                    createLobbyButton.setDisable(false);
                }
            } else {
                instructionsLobbyName.setText("Please only apply alphabetic characters (between 2-15 characters).");
            }
        }
    }

    public void updatePlayerLobbyList(Parent root) throws InterruptedException {
        model.getLobbySpace().put(Model.LOBBY_REQ, Model.GET_PLAYERLIST, model.getUniqueName());

        // [0] response code [1] list of playernames [2] username
        Object[] tuple = model.getLobbySpace().get(new ActualField(Model.LOBBY_RESP), new FormalField(ArrayList.class), new ActualField(model.getUniqueName()));

        if (root == null) {
            listOfPlayers.getItems().clear();
            for (String user : (ArrayList<String>) tuple[1]) {
                listOfPlayers.getItems().add(new Label(user));
            }
        } else {
            ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
            updatePlayerListView.getItems().clear();
            for (String user : (ArrayList<String>) tuple[1]) {
                updatePlayerListView.getItems().add(new Label(user));
            }
        }
    }

    @FXML
    public void pressBegin(ActionEvent event) throws InterruptedException {
        if (model.leaderForCurrentLobby) {
            System.out.println("BEGIN");
            model.getLobbySpace().put(Model.LOBBY_REQ, Model.BEGIN, model.getUniqueName());
        }
    }

    @FXML
    public void pressLeaveLobby(ActionEvent event) throws InterruptedException, IOException {
        model.leaderForCurrentLobby = false;

        updateAgent.interrupt();

        sendDisconnectTuple();

        model.resetLobbyInfo();

        changeScene(LOBBY_LIST_SCENE);
    }

    @FXML
    public void showCardList(ActionEvent event){
        cardListPane.setVisible(true);
        cardListPane.setMouseTransparent(false);
    }

    @FXML
    public void hideCardList(ActionEvent event){
        cardListPane.setVisible(false);
        cardListPane.setMouseTransparent(true);
    }

    public void clickTarget(javafx.scene.input.MouseEvent mouseEvent) throws InterruptedException, IOException {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {
                indexOfTarget = targetablePlayers.getSelectionModel().getSelectedIndex();

                if(!selectCardIsGuard) {
                    model.getLobbySpace().put(Model.SERVER_UPDATE, Model.DISCARD, model.getUniqueName(), Integer.toString(pickedCard), Integer.toString(indexOfTarget), ""); // Send the action to the server
                    model.cardsOnHand.remove(pickedCard);
                    pickedCard = 2;
                    indexOfTarget = -1;
                    changeScene("GameScene");
                }else {
                    changeScene("ChooseGuessScene");
                }
            }
        }
    }

    @FXML
    private void GuessSelect(ActionEvent event) throws InterruptedException, IOException {
        Button btn =(Button) event.getSource();
        model.getLobbySpace().put(Model.SERVER_UPDATE, Model.DISCARD, model.getUniqueName(), Integer.toString(pickedCard), Integer.toString(indexOfTarget), btn.getId()); // Send the action to the server
        model.cardsOnHand.remove(pickedCard);
        pickedCard = 2;
        indexOfTarget = -1;
        selectCardIsGuard = false;

        changeScene("GameScene");
    }

}
