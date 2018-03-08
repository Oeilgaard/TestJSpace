package MasterLobbyListServerTest.JavaFXClient;

public class HelperFunctions {

    public static boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]{2,15}");
    }

}
