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
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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
    private ImageView card1;
    @FXML
    private ImageView card2;

    private static ArrayList<UUID> lobbyIds;
    private static Model model;
    private static Thread updateAgent;

    public static Boolean connectedToLobby = false;


    public void pickCardOne(MouseEvent mouseEvent) {
        System.out.println("Card one");
    }

    public void pickCardTwo(MouseEvent mouseEvent) {
        System.out.println("Card two");
    }

    public void loadCardOne(MouseEvent mouseEvent) {
        Image baron = new Image("MasterLobbyListServerTest/JavaFXClient/resources/baron.jpg");
        card1.setImage(baron);
        card2.setImage(baron);
    }

    @FXML
    public void joinServer(ActionEvent event) throws IOException, InterruptedException {

        String urlForRemoteSpace = IP.getText();
        model = new Model();
        model.addIpToRemoteSpaces(urlForRemoteSpace);
        lobbyIds = new ArrayList<>();

        card1 = new ImageView();
        card2 = new ImageView();

        changeScene(USER_NAME_SCENE);
    }

    @FXML
    public void createUser(ActionEvent event) throws InterruptedException {

        String userNameString = userName.getText();

        if(HelperFunctions.validName(userNameString)) {

            createUserNameButton.setDisable(true);
            instructionsUserName.setText("");

            model.getRequestSpace().put(model.REQUEST_CODE, model.CREATE_USERNAME_REQ, userNameString, "");

            // Blocks until user receives unique username (due to 'get')
            Object[] tuple = model.getResponseSpace().get(new ActualField(model.RESPONSE_CODE), new ActualField(model.ASSIGN_UNIQUE_USERNAME_RESP),
                    new FormalField(Integer.class), new ActualField(userNameString), new FormalField(String.class));

            if((int) tuple[2] == model.OK) {
                model.setUniqueName((String) tuple[4]); // Setting the user's name
                System.out.println("Unique name:");
                System.out.println(model.getUniqueName());

                // Goto Lobby List
                try {
                    changeScene(PLAY_CARD_SCENE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // should ideally never happen, however can happen if the sanity check is bypassed client-side
            } else if((int) tuple[2] == model.BAD_REQUEST) {
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
        Scene scene = new Scene(root);
        Main.appWindow.setScene(scene);

        if (sceneName == LOBBY_LIST_SCENE) {
            ListView updateListView = ((ListView) root.lookup("#lobbyList"));

            updateListView.getItems().clear();
            List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
            for (Object[] obj : tuple) {
                updateListView.getItems().add(obj[1]);
                lobbyIds.add((UUID) obj[2]);
            }
        }

        if (sceneName == PLAY_CARD_SCENE){
            ArrayList<String> hand = new ArrayList<>();
            hand.add("baron");
            hand.add("prince");
            loadHand(hand, root);
        }
    }

    public void loadHand(ArrayList<String> hand, Parent root){
        ImageView card1 = ((ImageView) root.lookup("#card1"));
        card1.setImage(new Image("MasterLobbyListServerTest/JavaFXClient/resources/" + hand.get(0) + ".jpg"));
        ImageView card2 = ((ImageView) root.lookup("#card2"));
        card2.setImage(new Image("MasterLobbyListServerTest/JavaFXClient/resources/" + hand.get(1) + ".jpg"));
    }

    @FXML
    public void createLobby(ActionEvent event) throws InterruptedException {

        String lobbyNameString = lobbyName.getText();

        if(HelperFunctions.validName(lobbyNameString)) {
            createLobbyButton.setDisable(true);
            instructionsLobbyName.setText("");

            model.getRequestSpace().put(model.REQUEST_CODE, model.CREATE_LOBBY_REQ, lobbyNameString, model.getUniqueName());

            // Wait for server to be created
            Object[] tuple = model.getResponseSpace().get(new ActualField(model.RESPONSE_CODE), new FormalField(Integer.class),
                    new ActualField(model.getUniqueName()), new FormalField(UUID.class));

            if((int) tuple[1] == model.OK){
                try {
                    model.joinLobby((UUID) tuple[3]);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                connectedToLobby = true;

                try {
                    //TODO: Update list automatically when joining.
                    changeScene(LOBBY_LIST_SCENE);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if((int) tuple[1] == model.BAD_REQUEST){
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
        List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"),new FormalField(String.class),new FormalField(UUID.class));

        for (Object[] obj : tuple) {
            lobbyList.getItems().add(obj[1]);
            lobbyIds.add((UUID) obj[2]);
        }
    }

    //TODO Add button to join selected lobby

    @FXML
    public void goToCreateLobbyScene(ActionEvent event) throws InterruptedException {
        try {
            changeScene(CREATE_LOBBY_SCENE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendDisconnectTuple() throws InterruptedException {
        model.getLobbySpace().put("Connection",false,model.getUniqueName());
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

        System.out.println("Sending update tuple");
        model.getLobbySpace().put("Chat",model.getUniqueName(),text);
    }


    //TODO: implement Join-lobby button for highlighted choice
    public void clickLobby(javafx.scene.input.MouseEvent mouseEvent) throws InterruptedException, IOException {
        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
            if(mouseEvent.getClickCount() == 2){

                Object[] tuple = null;

                int index = lobbyList.getSelectionModel().getSelectedIndex();

                System.out.println("Double clicked join: " + lobbyList.getSelectionModel().getSelectedItem()
                        + " index: " + index);

                // Query the desired lobby-tuple (non-blocking)

                tuple = model.getLobbyListSpace().queryp(new ActualField("Lobby"),
                        new ActualField(lobbyList.getSelectionModel().getSelectedItem()),
                        new ActualField(lobbyIds.get(index)));

                //TODO: NullPointerException?
                model.joinLobby((UUID) tuple[2]);

                model.getLobbySpace().put("Connection",true,model.getUniqueName());

                Thread tryToJoinLobby = new Thread(new TimerForLobbyJoining(model,this));
                tryToJoinLobby.start();

                changeScene(LOADING_LOBBY_SCENE);

                model.getServerResponseMonitor().sync();

                switch (model.getResponseFromLobby()){
                    case 0:
                        changeScene(LOBBY_LIST_SCENE);
                        break;
                    case 1:
                        changeScene(LOBBY_LIST_SCENE);
                        break;
                    case 2:
                        Parent root = FXMLLoader.load(getClass().getResource("LobbyScene.fxml"));
                        Scene scene = new Scene(root);
                        Main.appWindow.setScene(scene);
                        ((Label)root.lookup("#lobbyTitle")).setText("Lobby name : " + lobbyList.getSelectionModel().getSelectedItem());
                        updatePlayerLobbyList(root);
                        connectedToLobby = true;

                        updateAgent = new Thread(new ClientChatUpdateAgent(model, root));
                        updateAgent.start();

                        break;
                }
            }
        }
    }

    public void updatePlayerLobbyList(Parent root) throws InterruptedException {
        List<Object[]> tuple = model.getLobbySpace().queryAll(new ActualField("playerField"),new FormalField(Integer.class), new FormalField(String.class));

        if(root == null) {
            listOfPlayers.getItems().clear();
            for (Object[] obj : tuple) {
                listOfPlayers.getItems().add(new Label((String)obj[2]));
            }
        } else {
            ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
            updatePlayerListView.getItems().clear();
            for (Object[] obj : tuple) {

                System.out.println("Got one!");

                String s = (String) obj[2];
                s = s.substring(0, s.indexOf("#"));
                updatePlayerListView.getItems().add(new Label(s));
            }
        }
    }



}
