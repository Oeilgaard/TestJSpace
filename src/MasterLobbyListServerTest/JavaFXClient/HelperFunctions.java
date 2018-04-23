package MasterLobbyListServerTest.JavaFXClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public class HelperFunctions {
    static final String legalCharacters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_";
    static SecureRandom rnd = new SecureRandom();

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

    //TODO make static?
    public boolean countessRule(ArrayList<String> hand){
        System.out.print("Countess rule is ");

        return ((hand.contains("COUNTESS") && hand.contains("PRINCE")) ||
                (hand.contains("COUNTESS") && hand.contains("GUARD")));
    }

    // Given an user-id, returns just the user name
    public static String removeUUIDFromUserName(String userID){
        return userID.substring(0, userID.indexOf("#"));
    }

    public static String randomLegalName(int len){
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++)
            sb.append(legalCharacters.charAt( rnd.nextInt(legalCharacters.length())));
        return sb.toString();
    }

    public static int randomLegalNameLength(){
        Random ran = new Random();
        int x = ran.nextInt(14) + 2;
        return x;
    }

    public static String currentLocalIP() throws UnknownHostException {
        return InetAddress.getLocalHost().toString().substring(InetAddress.getLocalHost().toString().indexOf("/")+1,
                InetAddress.getLocalHost().toString().length());
    }

    public static boolean stringMatchesUUIDPattern(String s){
        return s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }


}
