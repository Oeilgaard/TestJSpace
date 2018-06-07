package main_system_files.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Random;

public class HelperFunctionsServer {

    public static boolean validName(String name){
        return name.matches("[a-zA-Z0-9_]{2,15}");
    }

    public static boolean stringMatchesUUIDPattern(String s){
        return s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }


}
