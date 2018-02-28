package MasterLobbyListServerTest.JavaFXClient;

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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Controller {

    public static final String LOBBY_LIST_SCENE = "LobbyListScene";
    public static final String USER_NAME_SCENE = "UserNameScene";

    @FXML
    private ListView lobbyList;
    @FXML
    private TextField IP;
    @FXML
    private Button createUserNameButton;
    @FXML
    private TextField userName;
    @FXML
    private Label instructions;

    private static Model model;

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
            instructions.setText("");
            model.getRequest().put(model.REQUEST_CODE, model.CREATE_USERNAME_REQ, userNameString, "");

            // Blocking until user recieves unique username
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
            instructions.setText("Please only apply alphabetic characters (between 2-15 characters).");
        }

    }

    public void changeScene(String sceneName) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(sceneName + ".fxml"));
        Scene scene = new Scene(root);
        Main.appWindow.setScene(scene);
    }

    @FXML
    public void queryServers(ActionEvent event) throws InterruptedException {
        lobbyList.getItems().clear();

        List<Object[]> tuple = model.getLobbyList().queryAll(new ActualField("Lobby"),new FormalField(String.class),new FormalField(UUID.class));

        for (Object[] obj : tuple) {
            lobbyList.getItems().add(obj[1]);
        }
    }

    @FXML
    public void requestEnLobby(ActionEvent event) throws InterruptedException {
        model.getRequest().put(1,11,"Super fun Lobby!","John");
    }

}
