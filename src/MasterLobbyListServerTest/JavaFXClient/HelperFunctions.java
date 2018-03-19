package MasterLobbyListServerTest.JavaFXClient;

public class HelperFunctions {

    public static boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]{2,15}");
    }

    public static boolean isTargeted(String card) { return (card == "guard" || card == "baron" || card == "king"); }

    public static boolean isGuard(String card) { return card == "guard" ;}

}
