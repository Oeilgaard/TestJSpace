package MasterLobbyListServerTest.JavaFXClient;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Controller {

    public static final String LOBBY_LIST_SCENE = "LobbyListScene";
    public static final String USER_NAME_SCENE = "UserNameScene";
    public static final String CREATE_LOBBY_SCENE = "CreateLobbyScene";

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

    private static Model model;

    public static Boolean connectedToLobby = false;



    @FXML
    public void joinServer(ActionEvent event) throws IOException {

        String urlForRemoteSpace = IP.getText();
        model = new Model();
        model.addIpToRemoteSpaces(urlForRemoteSpace);

        changeScene(USER_NAME_SCENE);
    }

    @FXML
    public void createUser(ActionEvent event) throws InterruptedException {

        String userNameString = userName.getText();

        if(HelperFunctions.validName(userNameString)) {

            createUserNameButton.setDisable(true);
            instructionsUserName.setText("");

            model.getRequest().put(model.REQUEST_CODE, model.CREATE_USERNAME_REQ, userNameString, "");

            // Blocks until user receives unique username (due to 'get')
            Object[] tuple = model.getResponseSpace().get(new ActualField(model.RESPONSE_CODE), new ActualField(model.CREATE_UNIQUE_USERNAME),
                    new ActualField(userNameString), new FormalField(String.class));

            model.setUniqueName((String) tuple[3]); // Setting the user's name
            System.out.println("Unique name:");
            System.out.println(model.getUniqueName());

            // Goto Lobby List
            try {
                changeScene(LOBBY_LIST_SCENE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            instructionsUserName.setText("Please only apply alphabetic characters (between 2-15 characters).");
        }

    }

    public void changeScene(String sceneName) throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource(sceneName + ".fxml"));
        Scene scene = new Scene(root);
        Main.appWindow.setScene(scene);

//        if(sceneName.equals(LOBBY_LIST_SCENE)){
//            updateLobbyList();
//        }
    }

    public void createLobby(ActionEvent event) throws InterruptedException {

        String lobbyNameString = lobbyName.getText();

        if(HelperFunctions.validName(lobbyNameString)) {
            createLobbyButton.setDisable(true);
            instructionsLobbyName.setText("");

            model.getRequest().put(model.REQUEST_CODE, model.CREATE_LOBBY_REQ, lobbyNameString, model.getUniqueName());

            // Wait for server to be created
            Object[] tuple = model.getResponseSpace().get(new ActualField(model.RESPONSE_CODE), new ActualField(model.getUniqueName()), new FormalField(UUID.class));

            try {
                model.joinLobby((UUID) tuple[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            connectedToLobby = true;

            try {
                //TODO: Go to Lobby
                changeScene(LOBBY_LIST_SCENE);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            instructionsLobbyName.setText("Please only apply alphabetic characters (between 2-15 characters).");
        }
    }

//    public void updateLobbyList(){
//        lobbyList.getItems().clear();
//
//        List<Object[]> tuple = null;
//
//        try {
//            tuple = model.getLobbyList().queryAll(new ActualField("Lobby"),new FormalField(String.class),new FormalField(UUID.class));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        for (Object[] obj : tuple) {
//            lobbyList.getItems().add(obj[1]);
//        }
//    }

    @FXML
    public void queryServers(ActionEvent event) throws InterruptedException {
        lobbyList.getItems().clear();

        List<Object[]> tuple = model.getLobbyList().queryAll(new ActualField("Lobby"),new FormalField(String.class),new FormalField(UUID.class));

        for (Object[] obj : tuple) {
            lobbyList.getItems().add(obj[1]);
        }
    }

    @FXML
    public void goToCreateLobbyScene(ActionEvent event) throws InterruptedException {
        //model.getRequest().put(1,11,"Super fun Lobby!","John");
        try {
            changeScene(CREATE_LOBBY_SCENE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    @FXML
//    public void joinCreatedLobby(ActionEvent event) throws InterruptedException, IOException {
//        Object[] tuple = model.getRequest().get(new ActualField(2),new ActualField("John"),new FormalField(UUID.class));
//        model.joinLobby((UUID) tuple[2]);
//        model.getLobbySpace().put("Connection",true,model.getUniqueName());
//        connectedToLobby = true;
//    }

    public static void sendDisconnectTuple() throws InterruptedException {
        model.getLobbySpace().put("Connection",false,"John");
        connectedToLobby = false;
    }

    public void clickLobby(javafx.scene.input.MouseEvent mouseEvent) {
        System.out.println("clicked on " + lobbyList.getSelectionModel().getSelectedItem());
    }
}
