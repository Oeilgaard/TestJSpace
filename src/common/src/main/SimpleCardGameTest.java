package common.src.main;

import org.jspace.*;
import java.util.Random;

import java.io.IOException;
import java.util.Scanner;

public class SimpleCardGameTest {

    public static Scanner reader = new Scanner(System.in);  // Reading from System.in

    public static int[] Player1Cards = {0,0};
    public static int[] Player2Cards = {0,0};

    public static int[][] PlayerHands = {Player1Cards, Player2Cards};

    public static int currentTurn = 0;

    public static Random rand = new Random();

    Space ServerSpace = new SequentialSpace();

    public static void main(String[] argv) throws InterruptedException, IOException {

        for(int i = 0; i < 2; i++){
            PlayerHands[i][0] = rand.nextInt(8) + 1;
            PlayerHands[i][1] = rand.nextInt(8) + 1;
        }

        System.out.println("Player 1's cards are " + CardToString(0,0) + " and " + CardToString(0,1));
        System.out.println("Player 2's cards are " + CardToString(1,0) + " and " + CardToString(1,1));

        while(true){

            System.out.println("Turn nr : " + currentTurn);
            System.out.println("");

            switch (currentTurn % 2){



                case 0:
                    System.out.println("Player1: Your current hand is " +  CardToString(0,0)  + " and " +  CardToString(0,1));
                    System.out.println("Player1: Choose which card to play");

                    String n = reader.nextLine(); // Scans the next token of the input as an String.


                    if(n.equals("1")){
                        getNewCard (0,0);
                    } else if (n.equals("2")){
                        getNewCard (0,1);
                    } else {
                        System.out.println("Incorrect Syntax! her");
                    }
                    currentTurn++;

                    System.out.println("Player1: You new hand is " + CardToString(0,0)  + " and " +  CardToString(0,1));
                    System.out.println("");
                    break;

                case 1:
                    System.out.println("Player2: Your current hand is " +  CardToString(1,0)  + " and " +  CardToString(1,1));
                    System.out.println("Player2: Choose which card to play");

                    String k = reader.nextLine(); // Scans the next token of the input as an String.

                    if(k.equals("1")){
                        getNewCard (1,0);
                    } else if (k.equals("2")){
                        getNewCard (1,1);
                    } else {
                        System.out.println("Incorrect Syntax!");
                    }
                    currentTurn++;

                    System.out.println("Player2: You new hand is " + CardToString(1,0)  + " and " +  CardToString(1,1));
                    System.out.println("");
                    break;
            }

        }

    }



    public static void getNewCard (int playerNr, int cardNr){
        PlayerHands[playerNr][cardNr] = rand.nextInt(8) + 1;
    }

    public static String CardToString (int playerNr, int cardNr){
        switch (PlayerHands[playerNr][cardNr]){

            case 1:
                return "Guard";
            case 2:
                return "Priest";
            case 3:
                return "Baron";
            case 4:
                return "Handmaid";
            case 5:
                return "Prince";
            case 6:
                return "King";
            case 7:
                return "Countess";
            case 8:
                return "Princess";

            default:
                return "null";
        }
    }
}
