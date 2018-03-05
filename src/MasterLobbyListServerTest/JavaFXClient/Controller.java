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
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Controller {

    public static final String LOBBY_LIST_SCENE = "LobbyListScene";
    public static final String USER_NAME_SCENE = "UserNameScene";
    public static final String CREATE_LOBBY_SCENE = "CreateLobbyScene";
    public static final String LOADING_LOBBY_SCENE = "ConnectingToLobby";

    @FXML
    private ScrollPane scroll;
    @FXML
    private TextField chatTxtField;
    @FXML
    private VBox vb1;
    @FXML
    private ListView lobbyList;
    @FXML
    private TextField IP;
    @FXML
    private Button createUserNameButton;
    @FXML
    private TextField userName;
    @FXML
    private Label instructionsUserName;
    @FXML
    private TextField lobbyName;
    @FXML
    private Button createLobbyButton;
    @FXML
    private Label instructionsLobbyName;

    private static ArrayList<UUID> lobbyIds;
    private static Model model;

    public static Boolean connectedToLobby = false;
    public static Boolean readyForGameplay = false;

    @FXML
    public void joinServer(ActionEvent event) throws IOException {

        String urlForRemoteSpace = IP.getText();
        model = new Model();
        model.addIpToRemoteSpaces(urlForRemoteSpace);
        lobbyIds = new ArrayList<>();

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
                    changeScene(LOBBY_LIST_SCENE);
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

    public void changeScene(String sceneName) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource(sceneName + ".fxml"));
        Scene scene = new Scene(root);
        Main.appWindow.setScene(scene);

//        if(sceneName.equals(LOBBY_LIST_SCENE)){
//            try {
//                update();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
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
            createLobbyButton.setDisable(true);
        }
    }

    public void updateLobbyList(){
        //lobbyList.getItems().clear();
        try {
            List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"),new FormalField(String.class),new FormalField(UUID.class));
            for (Object[] obj : tuple) {
                lobbyList.getItems().add(obj[1]);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void update() throws InterruptedException {
        lobbyList.getItems().clear();

        List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"),new FormalField(String.class),new FormalField(UUID.class));

        for (Object[] obj : tuple) {
            lobbyList.getItems().add(obj[1]);
            lobbyIds.add((UUID) obj[2]);
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

    @FXML
    public void joinCreatedLobby(ActionEvent event) throws InterruptedException, IOException {
        Object[] tuple = model.getRequestSpace().get(new ActualField(2),new ActualField("John"),new FormalField(UUID.class));
        model.joinLobby((UUID) tuple[2]);

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
                changeScene("LobbyScene");
                break;
        }

    }

    public static void sendDisconnectTuple() throws InterruptedException {
        model.getLobbySpace().put("Connection",false,"John");
        connectedToLobby = false;
    }

    @FXML
    public void textToChat(ActionEvent e){

        String text = chatTxtField.getText();

        Label chatText = new Label(text);
        chatText.setWrapText(true);
        chatText.prefWidth(254);

        vb1.getChildren().add(chatText);
        chatTxtField.clear();
        scroll.setVvalue(1.0);
    }

    @FXML
    public void pressReadyButton(ActionEvent e) throws InterruptedException {

        readyForGameplay = !readyForGameplay;

        if(readyForGameplay) {
            model.getLobbySpace().getp(new ActualField("Ready"),new ActualField(!readyForGameplay),new ActualField(model.getUniqueName()));
            model.getLobbySpace().put("Ready", readyForGameplay, model.getUniqueName());
        } else {
            model.getLobbySpace().get(new ActualField("Ready"),new ActualField(!readyForGameplay),new ActualField(model.getUniqueName()));
            model.getLobbySpace().put("Ready", readyForGameplay, model.getUniqueName());
        }
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

                model.joinLobby((UUID) tuple[2]);

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
                        changeScene("LobbyScene");
                        break;
                }
            }
        }
    }

}
