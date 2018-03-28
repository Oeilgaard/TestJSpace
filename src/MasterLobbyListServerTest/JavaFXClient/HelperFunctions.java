package MasterLobbyListServerTest.JavaFXClient;

import java.util.ArrayList;

public class HelperFunctions {

    public static boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]{2,15}");
    }

    public static boolean isTargeted(String card) { return (card.equals("GUARD") || card.equals("BARON") || card.equals("KING") || card.equals("PRINCE") || card.equals("PRIEST")); }

    public static boolean isGuard(String card) { return card.equals("GUARD") ;}

    public static int isPrince(String card) {
        if(card.equals("PRINCE")){
            return 1;
        } else {
            return 0;
        }
    }

    public boolean countessRule(ArrayList<String> hand){
        System.out.print("Countess rule is ");

        return ((hand.contains("COUNTESS") && hand.contains("PRINCE")) ||
                (hand.contains("COUNTESS") && hand.contains("GUARD")));
    }

}
