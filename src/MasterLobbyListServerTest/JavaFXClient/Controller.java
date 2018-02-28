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

    @FXML
    private ListView lobbyList;
    @FXML
    private TextField IP;
    @FXML
    private Button joinServerButton;
    @FXML
    private Button createUserNameButton;
    @FXML
    private TextField userName;
    @FXML
    private Label instructions;

    private static Model model;

    @FXML
    public void joinServer(ActionEvent event) throws IOException {
        System.out.println("Joining main server...");
        String urlForRemoteSpace = IP.getText();
        model = new Model();
        model.addIpToRemoteSpaces(urlForRemoteSpace);

        Stage stage;
        Parent root;

        //get reference to the button's stage
        stage=(Stage) joinServerButton.getScene().getWindow();
        //load up OTHER FXML document
        root = FXMLLoader.load(getClass().getResource("UserNameScene.fxml"));

        //create a new scene with root and set the stage
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void createUser(ActionEvent event) throws InterruptedException {
        String userNameString = userName.getText();
        if(HelperFunctions.validName(userNameString)) {

            System.out.println("c 63");

            createUserNameButton.setDisable(true);
            instructions.setText("");
            model.getRequest().put(model.REQUEST_CODE, model.CREATE_USERNAME_REQ, userNameString, "");

            System.out.println("c 69");

            Object[] tuple = model.getResponseSpace().get(new ActualField(model.RESPONSE_CODE), new ActualField(model.CREATE_UNIQUE_USERNAME),
                    new ActualField(userNameString), new FormalField(String.class)); // TODO: why does it not block?

            System.out.println("c 74");

            System.out.println(tuple.toString());
            System.out.println(tuple.length);

            System.out.println((String) tuple[3]);

            model.setUniqueName((String) tuple[3]);



            System.out.println("Creating user name...");
            System.out.println("Unique name:");
            System.out.println(model.getUniqueName());

        } else {
            instructions.setText("Please only apply alphabetic characters (between 2-15 characters).");
        }

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
