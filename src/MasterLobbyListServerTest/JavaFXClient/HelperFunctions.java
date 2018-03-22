package MasterLobbyListServerTest.JavaFXClient;

public class HelperFunctions {

    public static boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]{2,15}");
    }

    public static boolean isTargeted(String card) { return (card.equals("GUARD") || card.equals("BARON") || card.equals("KING") || card.equals("PRINCE") || card.equals("PRIEST")); }

    public static boolean isGuard(String card) { return card.equals("GUARD") ;}

    public static boolean isPrince(String card) { return card.equals("PRINCE");}

}
