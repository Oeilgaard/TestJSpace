package MasterLobbyListServerTest.JavaFXClient;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.jspace.ActualField;
import org.jspace.FormalField;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Controller {

    public static final String LOBBY_LIST_SCENE = "LobbyListScene";
    private static final String USER_NAME_SCENE = "UserNameScene";
    private static final String CREATE_LOBBY_SCENE = "CreateLobbyScene";
    private static final String LOADING_LOBBY_SCENE = "ConnectingToLobby";
    private static final String PLAY_CARD_SCENE = "PlayCardScene";
    private static final String PICK_PLAYER_SCENE = "PickPlayerScene";
    protected static final String GAME_SCENE = "GameScene";
    private static final String LOBBY_SCENE = "LobbyScene";

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
    private TextField lobbyName;
    @FXML
    private Button createLobbyButton;
    @FXML
    private Label instructionsLobbyName;
    @FXML
    private Pane cardListPane;
    @FXML
    private ListView targetablePlayers;


    public static ArrayList<UUID> lobbyIds;
    private static Model model;
    public static Thread gameAgent;

    //public static Boolean connectedToLobby = false;

    private static int pickedCard = 2;
    private static boolean selectCardIsGuard = false;
    private static int indexOfTarget = -1;
    private static int threadGlobalId = 0;

    private static boolean[] playerEnableClick = {false,false,false,false,false};

    private static String lobbyTitleName;

    public Controller() {}


    public void pickCardOne() throws IOException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        if (HelperFunctions.isTargeted(model.cardsOnHand.get(0))) {
            pickedCard = 0;
            changeScene(PICK_PLAYER_SCENE);
            selectCardIsGuard = HelperFunctions.isGuard(model.cardsOnHand.get(0));
        } else {
            model.cardsOnHand.remove(0);
            changeScene(GAME_SCENE);

            //Encrypting message

            String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUniqueName() + "?0=*";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

            model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage); // Send the action to the server
        }

    }

    public void pickCardTwo() throws IOException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        if (HelperFunctions.isTargeted(model.cardsOnHand.get(1))) {
            pickedCard = 1;
            changeScene(PICK_PLAYER_SCENE);
            selectCardIsGuard = HelperFunctions.isGuard(model.cardsOnHand.get(1));
        } else {
            model.cardsOnHand.remove(1);
            changeScene(GAME_SCENE);

            String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUniqueName() + "?1=*";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

            model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage); // Send the action to the server

        }
    }

    @FXML
    public void joinServer() throws IOException, InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {

        String urlForRemoteSpace = IP.getText();
        model = new Model();
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        Key key = kg.generateKey();
        model.key = key;
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE,key);
        model.personalCipher = cipher;
        model.addIpToRemoteSpaces(urlForRemoteSpace);
        lobbyIds = new ArrayList<>();

        // (Blocking) query of the server's public key and set it in client's model
        Object[] tuple = model.getRequestSpace().query(new FormalField(PublicKey.class));
        model.setPublicKey((PublicKey) tuple[0]);

        changeScene(USER_NAME_SCENE);
    }

    @FXML
    public void createUser() throws InterruptedException, IOException, IllegalBlockSizeException {

        String userNameString = userName.getText();
        SealedObject encryptedUserNameString = new SealedObject(userNameString + "!", model.getCipher());

        if (HelperFunctions.validName(userNameString)) {

            createUserNameButton.setDisable(true);
            instructionsUserName.setText("");

            SealedObject encryptedKey = new SealedObject(model.key,model.getCipher());

            model.getRequestSpace().put(Model.REQUEST_CODE, Model.CREATE_USERNAME_REQ, encryptedUserNameString, encryptedKey);

            Object[] tuple;
            int field1;
            String field3;

            while (true){
                // Blocks until user receives unique username (due to 'get')
                // [0] response code [1] Response [2] Ok or error [3] Username of receiver [4] Username with ID
                try {
                    tuple = model.getResponseSpace().query(new ActualField(Model.RESPONSE_CODE), new ActualField(Model.ASSIGN_UNIQUE_USERNAME_RESP), new FormalField(SealedObject.class));

                    if (tuple != null) {

                        String decryptedMessage = (String) ((SealedObject) tuple[2]).getObject(model.personalCipher);

                        String field1text = decryptedMessage.substring(0,decryptedMessage.indexOf('!'));
                        field1 = Integer.parseInt(field1text);
                        field3 = decryptedMessage.substring(decryptedMessage.indexOf('?')+1,decryptedMessage.length());

                        model.getResponseSpace().get(new ActualField(Model.RESPONSE_CODE), new ActualField(Model.ASSIGN_UNIQUE_USERNAME_RESP), new FormalField(SealedObject.class));

                        break;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    //e.printStackTrace();
                }

            }

            //TODO lav en query der tjekke om det er det rigtige username

            if ((int) field1 == Model.OK) {
                model.setUniqueName((String) field3); // Setting the user's name

                // Goto Lobby List
                try {
                    changeScene(LOBBY_LIST_SCENE);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                // should ideally never happen, however can happen if the sanity check is bypassed client-side
            } else if ((int) field1 == Model.BAD_REQUEST) {
                instructionsUserName.setText("Server denied username. Please try again.");
                createUserNameButton.setDisable(false);
            }
        } else {
            instructionsUserName.setText("Please only apply alphabetic characters (between 2-15 characters).");
            createUserNameButton.setDisable(false);
        }
    }

    private void changeScene(String sceneName) throws IOException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {

        Parent root = FXMLLoader.load(getClass().getResource(sceneName + ".fxml"));
        model.currentRoot = root;
        Scene scene = new Scene(root);
        Main.appWindow.setScene(scene);

        switch (sceneName) {
            case LOBBY_LIST_SCENE: {
                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                label.setText(removedIdFromUsername());

                ListView updateListView = ((ListView) root.lookup("#lobbyList"));
                updateListView.getItems().clear();
                lobbyIds.clear();

                //[0] lobby code [1] Lobby name [2] Lobby ID
                List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField("Lobby"), new FormalField(String.class), new FormalField(UUID.class));
                for (Object[] obj : tuple) {
                    updateListView.getItems().add(obj[1]);
                    lobbyIds.add((UUID) obj[2]);
                }
                break;
            }
            case PLAY_CARD_SCENE: {
                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                label.setText(removedIdFromUsername());

                loadHand(model.cardsOnHand, root);

                VBox vb = ((VBox) model.currentRoot.lookup("#vb1playcard"));
                ScrollPane sp = ((ScrollPane) model.currentRoot.lookup("#scrollplaycard"));
                vb.getChildren().clear();
                for (String message : model.actionHistory) {
                    Label chatText = new Label(message);
                    chatText.setWrapText(true);
                    //chatText.prefWidth(184);

                    vb.getChildren().add(chatText);
                    sp.setVvalue(1.0);
                }

                break;
            }
            case GAME_SCENE: {

                ImageView card1 = ((ImageView) model.currentRoot.lookup("#cur_card"));
                card1.setImage(new Image("MasterLobbyListServerTest/JavaFXClient/resources/" + model.cardsOnHand.get(0) + ".jpg"));

                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                label.setText(removedIdFromUsername());

                VBox vb = ((VBox) model.currentRoot.lookup("#vb1"));
                ScrollPane sp = ((ScrollPane) model.currentRoot.lookup("#scroll"));
                vb.getChildren().clear();
                int everyOtherCounter = 0;
                for (String message : model.actionHistory) {
                    Label chatText = new Label(message);
                    chatText.setWrapText(true);
                    if(everyOtherCounter == 1) {
                        chatText.setTextFill(Color.web("#0F487F"));
                        everyOtherCounter = 0;
                    } else {
                        everyOtherCounter = 1;
                    }
                    //chatText.prefWidth(184);

                    vb.getChildren().add(chatText);
                    sp.setVvalue(1.0);
                }

                SealedObject encryptedMessage = new SealedObject(model.getUniqueName() + "!" + 2,model.getCipher());

                model.getLobbySpace().put("TargetablePlayersRequest",encryptedMessage);
                Object[] tuple = model.getLobbySpace().get(new ActualField("TargetablePlayersResponse"), new FormalField(SealedObject.class), new ActualField(model.indexInLobby));

                String[] listOfNames = (String[]) ((SealedObject)tuple[1]).getObject(model.personalCipher);

                ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
                updatePlayerListView.getItems().clear();
                for (int i = 0; i < 5; i++) {
                    if (!listOfNames[i].equals("")) {
                        updatePlayerListView.getItems().add(i + ". " + listOfNames[i]);
                    }
                }

                break;
            }
            case PICK_PLAYER_SCENE: {
                Label label = (Label) model.currentRoot.lookup("#usernameLabel");
                label.setText(removedIdFromUsername());

                ListView targetablePlayers = ((ListView) root.lookup("#targetablePlayers"));
                targetablePlayers.getItems().clear();

                SealedObject encryptedMessage = new SealedObject(model.getUniqueName() + "!" + HelperFunctions.isPrince(model.cardsOnHand.get(pickedCard)), model.getCipher());

                model.getLobbySpace().put("TargetablePlayersRequest", encryptedMessage);

                Object[] tuple = model.getLobbySpace().get(new ActualField("TargetablePlayersResponse"), new FormalField(SealedObject.class), new ActualField(model.indexInLobby));

                boolean noTargets = true;

                String[] listOfNames = (String[]) ((SealedObject)tuple[1]).getObject(model.personalCipher);

                for (int i = 0; i < 5; i++) {
                    if (!listOfNames[i].equals("")) {
                        targetablePlayers.getItems().add(i + ". " + listOfNames[i]);
                        playerEnableClick[i] = true;
                        noTargets = false;
                    } else {
                        targetablePlayers.getItems().add("");
                        playerEnableClick[i] = false;
                    }
                }

                if (noTargets) {
                    (model.currentRoot.lookup("#playNoTarget")).setDisable(false);
                } else {
                    (model.currentRoot.lookup("#playNoTarget")).setDisable(true);
                }
                break;
            }
            case LOBBY_SCENE: {

                ((Label) root.lookup("#lobbyTitle")).setText("Lobby name : " + lobbyTitleName);
                updatePlayerLobbyList(root);

                Label label = (Label)root.lookup("#usernameLabel");
                label.setText(removedIdFromUsername());

                model.setInLobby(true);
                if (model.leaderForCurrentLobby) {
                    root.lookup("#beginButton").disableProperty().setValue(false);
                }

                model.updateAgent = new Thread(new ClientUpdateAgent(model, root,threadGlobalId));
                model.updateAgent.start();
                threadGlobalId++;
                if(threadGlobalId > 9){
                    threadGlobalId = 0;
                }
                break;
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
    public void createLobby() throws InterruptedException, IOException, IllegalBlockSizeException {

        String lobbyNameString = lobbyName.getText();
        SealedObject encryptedLobbyNameString = new SealedObject(lobbyNameString + "!" + model.getUniqueName(), model.getCipher());

        if (HelperFunctions.validName(lobbyNameString)) {
            createLobbyButton.setDisable(true);
            instructionsLobbyName.setText("");

            SealedObject encryptedKey = new SealedObject(model.key,model.getCipher());

            model.getRequestSpace().put(Model.REQUEST_CODE, Model.CREATE_LOBBY_REQ, encryptedLobbyNameString, encryptedKey);

            // Wait for server to be created

            int field1;
            while (true) {
                try {
                    // [0] response code [1] Ok or deny [2] username of receiver [3] ID for lobby
                    Object[] tuple = model.getResponseSpace().query(new ActualField(Model.RESPONSE_CODE), new FormalField(SealedObject.class));
                    if (tuple != null) {

                        String decryptedMessage = (String) ((SealedObject) tuple[1]).getObject(model.personalCipher);

                        String field1text = decryptedMessage.substring(0, decryptedMessage.indexOf('!'));
                        field1 = Integer.parseInt(field1text);

                        model.getResponseSpace().get(new ActualField(Model.RESPONSE_CODE), new FormalField(SealedObject.class));

                        break;
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    //e.printStackTrace();
                }
            }

            //TODO indsæt SealedObject

            if ((int) field1 == Model.OK) {
                try {
                    changeScene(LOBBY_LIST_SCENE);

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else if ((int) field1 == Model.BAD_REQUEST) {
                System.out.println("Was denied");
                instructionsLobbyName.setText("Server denied to create lobby. Please try again.");
                createLobbyButton.setDisable(false);
            }
        } else {
            instructionsLobbyName.setText("Please only apply alphabetic characters (between 2-15 characters).");
        }
    }

    @FXML
    public void queryServers() throws InterruptedException {
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
    public void goToCreateLobbyScene() throws InterruptedException {
        try {
            changeScene(CREATE_LOBBY_SCENE);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void sendDisconnectTuple() throws InterruptedException, IOException, IllegalBlockSizeException {

        if(model.getInGame()){
            String messageToBeEncrypted = "" + Model.GAME_DISCONNECT + "!" + model.getUniqueName() + "?0=*";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());
            model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage); // Send the action to the server
        } else if(model.getInLobby()){
            //Tuple 1 - 3 sealed object
            String messageToBeEncrypted = "" + Model.LOBBY_DISCONNECT + "!" + model.getUniqueName() + "?" + -1;
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());
            SealedObject filler = new SealedObject("filler",model.getCipher());

            model.getLobbySpace().put(Model.LOBBY_REQ, encryptedMessage, filler);
            model.setInLobby(false);
        }
    }

    @FXML
    public void textToChat() throws InterruptedException, IOException, IllegalBlockSizeException {

        String text = chatTxtField.getText();

        Label chatText = new Label(removedIdFromUsername() + " : " + text);
        chatText.setWrapText(true);

        vb1.getChildren().add(chatText);
        chatTxtField.clear();
        scroll.setVvalue(1.0);

        //Encrypting message

        SealedObject encryptedMessage = new SealedObject(model.getUniqueName() + "!" +  text,model.getCipher());

        model.getLobbySpace().put("Chat", encryptedMessage);
    }


    //TODO: implement Join-lobby button for highlighted choice
    public void clickLobby(javafx.scene.input.MouseEvent mouseEvent) throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {

                int index = lobbyList.getSelectionModel().getSelectedIndex();

                // Query the desired lobby-tuple (non-blocking)

                //[0] lobby code [1] lobby name [2] lobby id
                Object[] tuple = model.getLobbyListSpace().queryp(new ActualField("Lobby"),
                        new ActualField(lobbyList.getSelectionModel().getSelectedItem()),
                        new ActualField(lobbyIds.get(index)));

                lobbyTitleName = (String) tuple[1];

                if (tuple != null) {

                    model.joinLobby((UUID) tuple[2]);

                    //Tuple 1 - 3 sealed object

                    String messageToBeEncrypted = "" + Model.CONNECT + "!" + model.getUniqueName() + "?" + Controller.threadGlobalId;

                    SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

                    SealedObject encryptedKey = new SealedObject(model.key,model.getCipher());

                    model.getLobbySpace().put(Model.LOBBY_REQ, encryptedMessage, encryptedKey);

                    Thread tryToJoinLobby = new Thread(new TimerForLobbyJoining(model));
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
                        changeScene(LOBBY_SCENE);
                        model.changeResponseFromLobby(0);

                        break;
                }
            }
        }
    }

    @FXML
    public void enterIPField(javafx.scene.input.KeyEvent keyEvent) throws IOException, InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            joinServer();
        }
    }

    @FXML
    public void requestNameVhaEnter(javafx.scene.input.KeyEvent keyEvent) throws InterruptedException, IOException, IllegalBlockSizeException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            createUser();
        }
    }

    @FXML
    public void createLobbyVhaEnter(javafx.scene.input.KeyEvent keyEvent) throws InterruptedException, IOException, IllegalBlockSizeException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            createLobby();
        }
    }

    private void updatePlayerLobbyList(Parent root) throws InterruptedException, IOException, IllegalBlockSizeException {

        //Encrypting the tuple

        String messageToBeEncrypted = "" + Model.GET_PLAYERLIST + "!" + model.getUniqueName() + "?" + -1;

        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

        SealedObject filler = new SealedObject("filler",model.getCipher());

        model.getLobbySpace().put(Model.LOBBY_REQ, encryptedMessage, filler);

        // [0] response code [1] list of playernames [2] username
        Object[] tuple = model.getLobbySpace().get(new ActualField(Model.LOBBY_RESP), new FormalField(ArrayList.class), new ActualField(model.indexInLobby));

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
    public void pressBegin() throws InterruptedException, IOException, IllegalBlockSizeException {
        if (model.leaderForCurrentLobby) {

            //Tuple 1 - 3 sealed object

            String messageToBeEncrypted = "" + Model.BEGIN + "!" + model.getUniqueName() + "?" + -1;

            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

            SealedObject filler = new SealedObject("filler",model.getCipher());

            model.getLobbySpace().put(Model.LOBBY_REQ, encryptedMessage, filler);
        }
    }

    @FXML
    public void pressLeaveLobby() throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        model.leaderForCurrentLobby = false;

        model.updateAgent.interrupt();

        //updateAgent = null;

        sendDisconnectTuple();

        model.resetLobbyInfo();

        model.indexInLobby = -1;

        changeScene(LOBBY_LIST_SCENE);
    }

    @FXML
    public void showCardList(){
        cardListPane.setVisible(true);
        cardListPane.setMouseTransparent(false);
    }

    @FXML
    public void hideCardList(){
        cardListPane.setVisible(false);
        cardListPane.setMouseTransparent(true);
    }

    public void clickTarget(javafx.scene.input.MouseEvent mouseEvent) throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {
                indexOfTarget = targetablePlayers.getSelectionModel().getSelectedIndex();

                if(!playerEnableClick[indexOfTarget]){
                    return;
                }

                if(!selectCardIsGuard) {
                    model.cardsOnHand.remove(pickedCard);
                    changeScene(GAME_SCENE);

                    String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUniqueName() + "?" + Integer.toString(pickedCard) + "=" + Integer.toString(indexOfTarget) + "*";
                    SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

                    model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage); // Send the action to the server

                    pickedCard = 2;
                    indexOfTarget = -1;
                }else {
                    changeScene("ChooseGuessScene");
                }
            }
        }
    }

    @FXML
    private void guessSelect(ActionEvent event) throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        Button btn =(Button) event.getSource();
        model.cardsOnHand.remove(pickedCard);
        changeScene(GAME_SCENE);

        String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUniqueName() + "?" + Integer.toString(pickedCard) + "=" + Integer.toString(indexOfTarget) + "*" + btn.getId();
        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

        model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage); // Send the action to the server

        pickedCard = 2;
        indexOfTarget = -1;
        selectCardIsGuard = false;

    }

    private String removedIdFromUsername(){
        String s = model.getUniqueName();
        s = s.substring(0, s.indexOf("#"));
        return s;
    }

    @FXML
    private void returnToPickingCard() throws IOException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        pickedCard = 2;
        selectCardIsGuard = false;
        changeScene(PLAY_CARD_SCENE);
    }

    @FXML
    private void playCardWithNoTargets() throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        model.cardsOnHand.remove(pickedCard);
        changeScene(GAME_SCENE);

        String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUniqueName() + "?" + Integer.toString(pickedCard) + "=*";
        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getCipher());

        model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage); // Send the action to the server

        pickedCard = 2;
        indexOfTarget = -1;
        selectCardIsGuard = false;

    }

}
