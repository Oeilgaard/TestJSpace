package MasterLobbyListServerTest.Server_Part.Gameplay;

import java.util.ArrayList;
import java.util.Scanner;

public class Game {

    public final static String newLine = System.getProperty("line.separator");

    private Model model;

    public Game(ArrayList<String> players) {
        model = new Model(players);
    }

    public void startGame() {

        Scanner scanner = new Scanner(System.in);

        int cardPick;
        int playerPick;
        int guardGuess;
        Character guardGuessCharacter;
        Character chosenCharacter;

        while (model.currentMaxAffection() < model.AFFECTION_GOAL_TWO_PLAYER) {

            System.out.println("Shuffling cards...");

            for(Player p : model.players) {
                p.getHand().getCards().clear();
                p.getDiscardPile().getCards().clear();
                p.setInRound(true);
                p.deactivateHandmaid();
            }

            model.turn = 0;
            model.round++;

            model.deck.getCards().clear();
            model.deck.fillDeck();
            model.deck.shuffle();
            model.setRoundWon(false);

            System.out.println("The revealed cards are:");
            for(int i = 0; i < model.REVEALED_CARDS_TWO_PLAYER; i++) {
                model.deck.drawCard(model.revealedCards);
                System.out.println(model.revealedCards.get(i).getCharacter());
            }

            // Secret card
            System.out.println("and a secret card is set aside..." + newLine);
            model.secretCard = model.deck.drawCard();
            //System.out.println(model.secretCard.getCharacter() + newLine);

            System.out.println("Each player draws a card..." + newLine);
            //Both players draw a card
            for(Player p : model.players) {
                model.deck.drawCard(p.getHand());
                System.out.println(p.getName() + " start with a " + p.getHand().getCards().get(0).getCharacter());
            }

            System.out.print(newLine);

            while(!model.roundWon) {

                if(model.players.get(model.indexOfCurrentPlayersTurn()).isInRound()) {

                    if(model.players.get(model.indexOfCurrentPlayersTurn()).isHandMaidProtected()) {
                        model.players.get(model.indexOfCurrentPlayersTurn()).deactivateHandmaid();
                    }

                    // States current player's turn
                    System.out.println("Round no. " + model.round + newLine + "Turn no. " + (model.turn+1) + newLine + model.players.get(model.indexOfCurrentPlayersTurn()).getName() + "'s turn" + newLine);

                    // 1. DRAW
                    model.deck.drawCard(model.players.get(model.indexOfCurrentPlayersTurn()).getHand());
                    System.out.println(model.players.get(model.indexOfCurrentPlayersTurn()).getName() + " drew a " + model.players.get(model.indexOfCurrentPlayersTurn()).getHand().getCards().get(1).getCharacter() + newLine);


                    System.out.println(model.players.get(model.indexOfCurrentPlayersTurn()).getName() + "'s current hand: ");
                    model.players.get(model.indexOfCurrentPlayersTurn()).getHand().printHand();
                    System.out.print(newLine);


                    // 2. DISCARD

                    System.out.print("Discard " + model.players.get(model.indexOfCurrentPlayersTurn()).getHand().getCards().get(0).getCharacter()
                            + " (press 1) or " + model.players.get(model.indexOfCurrentPlayersTurn()).getHand().getCards().get(1).getCharacter() +
                            " (press 2)" + newLine);

                    cardPick = scanner.nextInt();
                    chosenCharacter = model.players.get(model.indexOfCurrentPlayersTurn()).getHand().getCards().get(cardPick-1).getCharacter();

                    // 2.1 TARGETED CASE
                    targetedCase:
                    if(chosenCharacter.isTargeted()) {

                        int i = 1;
                        boolean possibleTargets = false;

                        for(Player p : model.players) {

                            if(p.isInRound() && !p.isHandMaidProtected() && !p.isMe(model.players.get(model.indexOfCurrentPlayersTurn()).getName())) {
                                possibleTargets = true;
                                System.out.println("Target player " + p.getName() + " with " + chosenCharacter + " (press " + i + ")");
                            }
                            i++;
                        }

                        if(!possibleTargets){
                            // goto roundEndChecks;
                            model.noAction(model.indexOfCurrentPlayersTurn(), cardPick-1);
                            break targetedCase;
                        }

                        playerPick = scanner.nextInt();

                        if(chosenCharacter == Character.GUARD) {

                            int j = 1;
                            System.out.println("To guess: " + newLine);
                            for(Character c : Character.values()){
                                if(c != Character.GUARD) {
                                    System.out.println(c + " (press " + j + ")");
                                    j++;
                                }
                            }

                            guardGuess = scanner.nextInt();
                            guardGuessCharacter = Character.values()[guardGuess];
                            System.out.println("You guessed " + Character.values()[guardGuess]);
                            model.guardAction(model.indexOfCurrentPlayersTurn(), cardPick-1, playerPick-1, guardGuessCharacter);

                        } else if(chosenCharacter == Character.PRIEST) {
                            model.priestAction(model.indexOfCurrentPlayersTurn(), cardPick-1, playerPick-1);
                        } else if(chosenCharacter == Character.BARON) {
                            //System.out.println("Player index " + model.indexOfCurrentPlayersTurn() + " card index " + (cardPick-1) + " player index" + (playerPick-1));
                            model.baronAction(model.indexOfCurrentPlayersTurn(), cardPick-1, playerPick-1);
                        } else if(chosenCharacter == Character.PRINCE) {
                            model.princeAction(model.indexOfCurrentPlayersTurn(), cardPick-1, playerPick-1);
                        } else { // i.e. chosenCharacter == Character.KING
                            //model.players.get(model.indexOfCurrentPlayersTurn()).getHand().printHand();
                            System.out.println(model.players.get(model.indexOfCurrentPlayersTurn()).getName() + " gets " + model.players.get(playerPick-1).getHand().getCards().get(0).getCharacter());
                            System.out.println(model.players.get(playerPick-1).getName() + " gets " + model.players.get(model.indexOfCurrentPlayersTurn()).getHand().getCards().get(cardPick%2).getCharacter());
                            model.kingAction(model.indexOfCurrentPlayersTurn(), cardPick-1, playerPick-1);

                        }
                        // 2.2 UNTARGETED CASE
                    } else {
                        if(chosenCharacter == Character.HANDMAID){
                            System.out.println(model.players.get(model.indexOfCurrentPlayersTurn()).getName() + " is handmaid protected until next turn...");
                            model.handmaidAction(model.indexOfCurrentPlayersTurn(), cardPick-1);
                        } else if(chosenCharacter == Character.COUNTESS) {
                            model.countessAction(model.indexOfCurrentPlayersTurn(), cardPick-1);
                        } else {
                            // Princess
                            model.princessAction(model.indexOfCurrentPlayersTurn(), cardPick-1);
                        }
                    }

                    // 3. ROUND END CHECKS

                    // 3.1 LMS
                    if(model.isLastManStanding()) {

                        for(Player p : model.players) {
                            System.out.println(p.getName() + p.isInRound());
                        }

                        System.out.println(model.lastMan().getName() + " won the round as last man standing!");
                        model.lastMan().incrementAffection();
                        System.out.println(model.lastMan().getName() + "'s affection is now " + model.lastMan().getAffection() + newLine);
                        model.setRoundWon(true);
                        //model.players.
                        // 3.2 EMPTY DECK
                    } else if(model.deck.getCards().isEmpty()) {
                        if(model.nearestToPrincess().size() == 1){
                            System.out.println("The deck is empty! " + model.nearestToPrincess().get(0).getName() + " won the round with the highest card!");
                            model.nearestToPrincess().get(0).incrementAffection();
                            System.out.println(model.nearestToPrincess().get(0).getName() + "'s affection is now " + model.nearestToPrincess().get(0).getAffection() + newLine);
                            model.setRoundWon(true);
                        } else if(model.deck.getCards().size() > 0) {
                            if(model.compareHandWinners().size() == 1) {
                                System.out.println("The deck is empty! " + model.nearestToPrincess().get(0).getName() + " won the round with the highest discard pile!");
                                model.compareHandWinners().get(0).incrementAffection();
                                System.out.println(model.nearestToPrincess().get(0).getName() + "'s affection is now " + model.nearestToPrincess().get(0).getAffection() + newLine);
                                model.setRoundWon(true);
                            }
                        } else {
                            model.setRoundWon(true); // corresponds to draw...
                        }
                    }
                    model.turn++;
                }
            }
            System.out.println("Game is over");
        }
    }
}
