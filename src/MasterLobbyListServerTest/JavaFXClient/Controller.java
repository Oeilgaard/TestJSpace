package MasterLobbyListServerTest.JavaFXClient;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
    private ListView listView;
    @FXML
    private TextField txtfield1;
    @FXML
    private Button btn1;

    private static Model model;

    @FXML
    public void trykPåKnappen(ActionEvent event) throws IOException {
        System.out.println("Du trykkede på knappen");
        String urlForRemoteSpace = txtfield1.getText();
        model = new Model();
        model.addIpToRemoteSpaces(urlForRemoteSpace);

        Stage stage;
        Parent root;

        //get reference to the button's stage
        stage=(Stage) btn1.getScene().getWindow();
        //load up OTHER FXML document
        root = FXMLLoader.load(getClass().getResource("sample2.fxml"));

        //create a new scene with root and set the stage
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void fåList(ActionEvent event) throws InterruptedException {
        listView.getItems().clear();

        List<Object[]> tuple = model.getLobbyList().queryAll(new ActualField("Lobby"),new FormalField(String.class),new FormalField(UUID.class));

        for (Object[] obj : tuple) {
            listView.getItems().add(obj[1]);
        }
    }

    @FXML
    public void requestEnLobby(ActionEvent event) throws InterruptedException {
        model.getRequest().put(1,11,"Super fun Lobby!","John");
    }

}
