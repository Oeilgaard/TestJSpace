package MasterLobbyListServerTest.JavaFXClient;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jspace.ActualField;
import org.jspace.FormalField;

import java.util.List;

public class ClientChatUpdateAgent implements Runnable{

    private Model model;
    private Parent root;

    public ClientChatUpdateAgent(Model model, Parent root){
        this.model = model;
        this.root = root;
    }

    @Override
    public void run() {
        while(true) {
            try {

                Object[] tuple = model.getLobbySpace().get(new ActualField("ChatUpdate"),new FormalField(String.class), new FormalField(String.class), new ActualField(model.getUniqueName()));

                System.out.println("Got Chat update!");
                    Platform.runLater(new Runnable() {
                        public void run() {
                            String s = (String) tuple[1];
                            s = s.substring(0, s.indexOf("#"));
                            Label chatText = new Label(s + " : " + tuple[2]);
                            chatText.setWrapText(true);
                            chatText.prefWidth(254);

                            ((VBox) root.lookup("#vb1")).getChildren().add(chatText);
                            ((ScrollPane) root.lookup("#scroll")).setVvalue(1.0);
                        }
                    });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
