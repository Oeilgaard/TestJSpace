package main_system_files.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Bloom;
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
import java.security.NoSuchAlgorithmException;
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
    @FXML
    private ImageView card1;
    @FXML
    private ImageView card2;


    public static ArrayList<UUID> lobbyIds;
    public static Model model;
    public static Thread gameAgent;

    //public static Boolean connectedToLobby = false;

    private static int pickedCard = 2;
    private static boolean selectCardIsGuard = false;
    private static int indexOfTarget = -1;
    //private static int currentThreadNumber = 0;

    private static boolean[] playerEnableClick = {false,false,false,false,false};

    public Controller() {}

    /* GUI */

    public void hoverCardOne(){
        Bloom bloom = new Bloom();
        bloom.setThreshold(0.8);
//        ScaleTransition st = new ScaleTransition(Duration.millis(200), card1);
//        st.fromXProperty();
//        st.toXProperty();
//        st.setByX(1.0000001f);
//        st.setByY(1.0000001f);
//        st.setCycleCount(1);
//        st.setAutoReverse(true);
//        st.play();
        card1.setEffect(bloom);
    }

    public void unhoverCardOne(){
        card1.setEffect(null);
//        ScaleTransition st = new ScaleTransition(Duration.millis(200), card1);
//        st.setByX(-1.0000001f);
//        st.setByY(-1.0000001f);
        //st.play();
    }

    public void hoverCardTwo(){
        Bloom bloom = new Bloom();
        bloom.setThreshold(0.8);
//        ScaleTransition st = new ScaleTransition(Duration.millis(200), card1);
//        st.setFromX(1);
//        st.setFromY(1);
//        st.setByX(0.8);
//        st.setByY(0.8);
//        st.play();
        card2.setEffect(bloom);
    }

    public void unhoverCardTwo(){
        card2.setEffect(null);
//        ScaleTransition st = new ScaleTransition(Duration.millis(200), card1);
//        st.setByX(-0.8);
//        st.setByY(-0.8);
//        st.play();
    }

    @FXML
    public void requestNameViaEnterKey(javafx.scene.input.KeyEvent keyEvent) throws InterruptedException, IOException, IllegalBlockSizeException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) { createUser(); }
    }

    @FXML
    public void createLobbyViaEnterKey(javafx.scene.input.KeyEvent keyEvent) throws InterruptedException, IOException, IllegalBlockSizeException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) { createLobby(); }
    }

    private void changeScene(String sceneName) throws IOException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        model.currentSceneIsGameScene = false;

        Parent root = FXMLLoader.load(getClass().getResource(sceneName + ".fxml"));
        model.currentRoot = root;
        Scene scene = new Scene(root);
        Main.appWindow.setScene(scene);

        switch (sceneName) {
            case LOBBY_LIST_SCENE: {
                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                label.setText(model.getUserName());

                ListView updateListView = ((ListView) root.lookup("#lobbyList"));
                updateListView.getItems().clear();
                lobbyIds.clear();

                //[0] lobby code [1] Lobby name [2] Lobby ID
                List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField(Model.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));
                for (Object[] obj : tuple) {
                    updateListView.getItems().add(obj[1]);
                    lobbyIds.add((UUID) obj[2]);
                }
                break;
            }
            case PLAY_CARD_SCENE: {
                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                label.setText(model.getUserName());

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

                model.currentSceneIsGameScene = true;

                ImageView card1 = ((ImageView) model.currentRoot.lookup("#cur_card"));
                card1.setImage(new Image("resources/" + model.cardsOnHand.get(0).toLowerCase() + ".jpg")); //"main_system_files/client/resources/"

                Label label = (Label)model.currentRoot.lookup("#usernameLabel");
                label.setText(model.getUserName());

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

                SealedObject encryptedMessage = new SealedObject(model.getUserID() + "!" + 2,model.getLobbyCipher());

                model.getLobbySpace().put(Model.TARGETS_REQUEST,encryptedMessage);
                Object[] tuple = model.getLobbySpace().get(new ActualField(Model.TARGETS_RESPONSE), new FormalField(SealedObject.class), new ActualField(model.getIndexInLobby()));

                String[] listOfNames = (String[]) ((SealedObject)tuple[1]).getObject(model.personalCipher);

                ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
                updatePlayerListView.getItems().clear();
                for (int i = 0; i < 4; i++) {
                    if (!listOfNames[i].equals("")) {
                        updatePlayerListView.getItems().add(i + 1 + ". " + listOfNames[i]);
                    }
                }

                break;
            }
            case PICK_PLAYER_SCENE: {
                Label label = (Label) model.currentRoot.lookup("#usernameLabel");
                label.setText(model.getUserName());

                ListView targetablePlayers = ((ListView) root.lookup("#targetablePlayers"));
                targetablePlayers.getItems().clear();

                SealedObject encryptedMessage = new SealedObject(model.getUserID() + "!" + HelperFunctions.isPrince(model.cardsOnHand.get(pickedCard)), model.getLobbyCipher());

                model.getLobbySpace().put(Model.TARGETS_REQUEST, encryptedMessage);

                Object[] tuple = model.getLobbySpace().get(new ActualField(Model.TARGETS_RESPONSE), new FormalField(SealedObject.class), new ActualField(model.getIndexInLobby()));

                boolean noTargets = true;

                String[] listOfNames = (String[]) ((SealedObject)tuple[1]).getObject(model.personalCipher);

                for (int i = 0; i < 4; i++) {
                    if (!listOfNames[i].equals("")) {
                        targetablePlayers.getItems().add(i + 1 + ". " + listOfNames[i]);
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

                ((Label) root.lookup("#lobbyTitle")).setText("Lobby name : " + model.getCurrentLobbyName());
                updatePlayerLobbyList(root);

                Label label = (Label)root.lookup("#usernameLabel");
                label.setText(model.getUserName());

                if (model.isLeader()) {
                    root.lookup("#beginButton").disableProperty().setValue(false);
                }

                // Note: hard to move to model, as we need to make sure we are infact in the Lobby when we start
                // to fetch for lobbies.
                model.updateAgent = new Thread(new LobbyCommunicationAgent(model, root, model.getCurrentThreadNumber()));
                model.updateAgent.start();
                model.incrementCurrentThreadNumber();
                if(model.getCurrentThreadNumber() > 9){
                    model.setCurrentThreadNumber(0);
                }
                break;
            }
        }
    }

    public static void loadHand(ArrayList<String> hand, Parent root) {
        ImageView card1 = ((ImageView) root.lookup("#card1"));
        card1.setImage(new Image("resources/" + hand.get(0).toLowerCase() + ".jpg")); //main_system_files/
        ImageView card2 = ((ImageView) root.lookup("#card2"));
        card2.setImage(new Image("resources/" + hand.get(1).toLowerCase() + ".jpg")); //main_system_files/
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

    @FXML
    public void enterIPField(javafx.scene.input.KeyEvent keyEvent) throws IOException, InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        if (keyEvent.getCode().equals(KeyCode.ENTER)) {
            joinServer();
        }
    }

    @FXML
    public void joinServer() throws IOException, InterruptedException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        String urlForRemoteSpace = IP.getText();
        model = new Model();
        model.joinServerLogic(urlForRemoteSpace);
        lobbyIds = new ArrayList<>();
        changeScene(USER_NAME_SCENE);
    }


    /* LOGIC */

    public void pickCardOne() throws IOException, InterruptedException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {

        if (HelperFunctions.isTargeted(model.cardsOnHand.get(0))) {
            pickedCard = 0;
            changeScene(PICK_PLAYER_SCENE);
            selectCardIsGuard = HelperFunctions.isGuard(model.cardsOnHand.get(0));
        } else {
            model.cardsOnHand.remove(0);
            changeScene(GAME_SCENE);

            //Encrypting message
            String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUserID() + "?0=*¤";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted, model.getLobbyCipher());
            SealedObject filler = new SealedObject("filler",model.getLobbyCipher());

            model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage,filler); // Send the action to the server
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

            String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUserID() + "?1=*¤";
            SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getLobbyCipher());
            SealedObject filler = new SealedObject("filler",model.getLobbyCipher());

            model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage, filler); // Send the action to the server

        }
    }

    @FXML
    public void createUser() throws InterruptedException, IOException, IllegalBlockSizeException {

        String userNameString = userName.getText(); // get the inputted name in the GUI

            if (model.createUserLogic(userNameString)){
                // Goto Lobby List
                try {
                    changeScene(LOBBY_LIST_SCENE);
                } catch (BadPaddingException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else  {
                // should ideally never happen, however can happen if the sanity check is bypassed client-side
                instructionsUserName.setText("Server denied username. Please try again. Please only apply alphabetic characters (between 2-15 characters)");
                createUserNameButton.setDisable(false);
            }
    }

    @FXML
    public void createLobby() throws InterruptedException, IOException, IllegalBlockSizeException {

        String lobbyNameString = lobbyName.getText();

        if (model.createLobbyLogic(lobbyNameString)) {
            try {
                changeScene(LOBBY_LIST_SCENE);
            } catch (IOException | BadPaddingException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Was denied");
            instructionsLobbyName.setText("Server denied to create lobby. Please try again. Note that you can only apply alphabetic characters (between 2-15 characters)");
            createLobbyButton.setDisable(false);
        }
    }

    @FXML
    public void queryServers() throws InterruptedException {
        lobbyList.getItems().clear();
        lobbyIds.clear();

        //[0] lobby code [1] Lobby name [2] Lobby ID
        List<Object[]> tuple = model.getLobbyListSpace().queryAll(new ActualField(Model.LOBBY_INFO), new FormalField(String.class), new FormalField(UUID.class));

        for (Object[] obj : tuple) {
            lobbyList.getItems().add(obj[1]);
            lobbyIds.add((UUID) obj[2]);
        }
    }

    @FXML
    public void textToChat() throws InterruptedException, IOException, IllegalBlockSizeException {

        String text = chatTxtField.getText();
        model.textToChatLogic(text);
        chatTxtField.clear();
    }

    public void joinLobbyButton() throws InterruptedException, ClassNotFoundException, BadPaddingException, IllegalBlockSizeException, IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        if (lobbyList.getSelectionModel().getSelectedItems().isEmpty()){
            return;
        }
        int index = lobbyList.getSelectionModel().getSelectedIndex();

        changeScene(LOADING_LOBBY_SCENE);

        int result = model.joinLobbyLogic((String) lobbyList.getSelectionModel().getSelectedItem(), lobbyIds.get(index), model.getCurrentThreadNumber());

        switch (result) {
            case Model.NO_RESPONSE:
                changeScene(LOBBY_LIST_SCENE);
                model.changeResponseFromLobby(Model.NO_RESPONSE);
                break;
            case Model.BAD_REQUEST:
                changeScene(LOBBY_LIST_SCENE);
                model.changeResponseFromLobby(Model.NO_RESPONSE);
                break;
            case Model.OK:
                changeScene(LOBBY_SCENE);
                model.changeResponseFromLobby(Model.NO_RESPONSE);

                break;
        }
    }

    public void joinLobby(javafx.scene.input.MouseEvent mouseEvent) throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {

                int index = lobbyList.getSelectionModel().getSelectedIndex();

                if (lobbyList.getSelectionModel().getSelectedItem() == null){
                    return;
                }

                changeScene(LOADING_LOBBY_SCENE);

                int result = model.joinLobbyLogic((String) lobbyList.getSelectionModel().getSelectedItem(), lobbyIds.get(index), model.getCurrentThreadNumber());

                switch (result) {
                    case Model.NO_RESPONSE:
                        changeScene(LOBBY_LIST_SCENE);
                        model.changeResponseFromLobby(Model.NO_RESPONSE);
                        break;
                    case Model.BAD_REQUEST:
                        changeScene(LOBBY_LIST_SCENE);
                        model.changeResponseFromLobby(Model.NO_RESPONSE);
                        break;
                    case Model.OK:
                        changeScene(LOBBY_SCENE);
                        model.changeResponseFromLobby(Model.NO_RESPONSE);

                        break;
                }
            }
        }
    }

    private void updatePlayerLobbyList(Parent root) throws InterruptedException, IOException, IllegalBlockSizeException {

        ArrayList<String> currentPlayers = model.updatePlayerLobbyListLogic();

        if (root == null) {
            listOfPlayers.getItems().clear();
            for (String user : currentPlayers) {
                listOfPlayers.getItems().add(new Label(user));
            }
        } else {
            ListView updatePlayerListView = ((ListView) root.lookup("#listOfPlayers"));
            updatePlayerListView.getItems().clear();
            for (String user : currentPlayers) {
                updatePlayerListView.getItems().add(new Label(user));
            }
        }
    }

    @FXML
    public void pressBegin() throws InterruptedException, IOException, IllegalBlockSizeException { model.pressBeginLogic(); }

    @FXML
    public void pressLeaveLobby() throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {


        model.setIsLeader(false);

        model.updateAgent.interrupt();

        //updateAgent = null;

        model.sendDisconnectTuple();

        model.resetLobbyInfo();

        model.setIndexInLobby(-1);

        changeScene(LOBBY_LIST_SCENE);
    }

    public static void sendDisconnectTuple() throws InterruptedException, IOException, IllegalBlockSizeException {
        model.sendDisconnectTuple();
    }

    public void clickTarget(javafx.scene.input.MouseEvent mouseEvent) throws InterruptedException, IOException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {
                indexOfTarget = targetablePlayers.getSelectionModel().getSelectedIndex();

                System.out.println(indexOfTarget);

                if(indexOfTarget == -1 || !playerEnableClick[indexOfTarget]){
                    return;
                }

                if(!selectCardIsGuard) {
                    model.cardsOnHand.remove(pickedCard);
                    changeScene(GAME_SCENE);

                    String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUserID() + "?" + Integer.toString(pickedCard) + "=" + Integer.toString(indexOfTarget) + "*¤";
                    SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getLobbyCipher());
                    SealedObject filler = new SealedObject("filler",model.getLobbyCipher());

                    model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage, filler); // Send the action to the server

                    pickedCard = 2;
                    indexOfTarget = -1;
                } else {
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

        String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUserID() + "?" + Integer.toString(pickedCard) + "=" + Integer.toString(indexOfTarget) + "*" + btn.getId() + "¤";
        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getLobbyCipher());
        SealedObject filler = new SealedObject("filler",model.getLobbyCipher());

        model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage, filler); // Send the action to the server

        pickedCard = 2;
        indexOfTarget = -1;
        selectCardIsGuard = false;
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

        String messageToBeEncrypted = "" + Model.DISCARD + "!" + model.getUserID() + "?" + Integer.toString(pickedCard) + "=*¤";
        SealedObject encryptedMessage = new SealedObject(messageToBeEncrypted,model.getLobbyCipher());
        SealedObject filler = new SealedObject("filler",model.getLobbyCipher());

        model.getLobbySpace().put(Model.SERVER_UPDATE, encryptedMessage, filler); // Send the action to the server

        pickedCard = 2;
        indexOfTarget = -1;
        selectCardIsGuard = false;

    }

}
