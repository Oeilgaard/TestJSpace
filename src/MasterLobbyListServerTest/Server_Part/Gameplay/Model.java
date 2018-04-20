package MasterLobbyListServerTest.Server_Part.Gameplay;

import MasterLobbyListServerTest.Server_Part.LobbyUser;
import MasterLobbyListServerTest.Server_Part.ServerData;
import org.jspace.SequentialSpace;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.ArrayList;

public class Model {

    public final static int AFFECTION_GOAL_TWO_PLAYERS = 7;
    public final static int AFFECTION_GOAL_THREE_PLAYERS = 5;
    public final static int AFFECTION_GOAL_FOUR_PLAYERS = 20;

    public final static int REVEALED_CARDS_TWO_PLAYER = 3;

    public final static int NO_OF_GUARD = 5;
    public final static int NO_OF_PRIEST = 2;
    public final static int NO_OF_BARON = 2;
    public final static int NO_OF_HANDMAID = 2;
    public final static int NO_OF_PRINCE = 2;
    public final static int NO_OF_KING = 1;
    public final static int NO_OF_COUNTESS = 1;
    public final static int NO_OF_PRINCESS = 1;

    public final static int CLIENT_UPDATE = 10;
    public final static int NEW_TURN = 11;
    public final static int DISCARD = 12;
    public final static int TARGETTED = 121;
    public final static int UNTARGETTED = 122;
    public final static int OUTCOME = 13;
    public final static int KNOCK_OUT = 14;
    public final static int WIN = 15;
    public final static int GAME_START_UPDATE = 16;
    public final static int ACTION_DENIED = 17;
    public final static int GAME_ENDING = 18;
    public final static int GAME_DISCONNECT = 19;


    public final static int SERVER_UPDATE = 20;
    //public final static int DISCARD = 21;

    protected ArrayList<Player> players;
    protected int turn;
    protected int affectionGoal;
    protected int playerPointer; // index of the current player's turn
    protected int round;
    protected Deck deck;
    protected boolean roundWon;
    protected ArrayList<Card> revealedCards;
    protected Card secretCard;
    private SequentialSpace lobbySpace;

    public Model(ArrayList<LobbyUser> users, SequentialSpace lobbySpace){
        this.turn = 0;
        this.round = 0;
        this.deck = new Deck();
        this.roundWon = false;
        this.revealedCards = new ArrayList<Card>();
        this.lobbySpace = lobbySpace;
        this.players = new ArrayList<Player>();
        for(LobbyUser user : users) {
            this.players.add(new Player(user.name,user.userNr,user.personalCipher));
        }
    }

    // RULES & UTILITY

    public void determineAffectionGoal(){
        switch (players.size()) {
            case 2:
                affectionGoal = AFFECTION_GOAL_TWO_PLAYERS;
                break;
            case 3:
                affectionGoal = AFFECTION_GOAL_THREE_PLAYERS;
                break;
            case 4:
                affectionGoal = AFFECTION_GOAL_FOUR_PLAYERS;
                break;
            default:
                break;
        }
    }

    public Player getUserfromName(String name) {
        for (Player user : players){
            if(user.getName().equals(name)){
                return user;
            }
        }
        return null;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    //TODO: Should they be synchronized?
    public void nextTurn() { turn++; }

    //TODO: Should they be synchronized?
    public void nextRound() {
        round++;
    }

    public int currentMaxAffection(){
        int max = 0;
        for(Player p : players){
            if(max < p.getAffection()) { max = p.getAffection(); }
        }
        return max;
    }

    public int indexOfCurrentPlayersTurn(){
        return playerPointer % players.size();
    } // circular0 array principle

    public boolean isLastManStanding() {
        int counter = 0;
        for(Player p : players) {
            if(p.isInRound()){ counter++; }
        }
        if(counter == 1) {
            return true;
        } else { return false; }
    }

    public Player lastMan() {
        for(Player p : players) {
            if(p.isInRound()){ return p; }
        }
        return null;
    }

    public String getWinner(){
        for(Player p : players){
            if(p.getAffection()==affectionGoal){
                return p.getName();
            }
        }
        return null;
    }

    public ArrayList<Player> nearestToPrincess(){
        ArrayList<Player> winners = new ArrayList<>();
        for(Player p : players){
            if(p.isInRound() && p.getHand().getCards().get(0).getValue() == highestCard()) {
                winners.add(p);
            }
        }
        return winners;
    }

    public int highestCard(){
        int max = -1;
        for(Player p : players){
            if(p.isInRound() && p.getHand().getCards().get(0).getValue() > max) {
                max = p.getHand().getCards().get(0).getValue();
            }
        }
        return max;
    }

    public int highestDiscardPile(){
        int max = -1;
        for(Player p : nearestToPrincess()){
            if(p.discardPileSum() > max) {
                max = p.getHand().getCards().get(0).getValue();
            }
        }
        return max;
    }

    public ArrayList<Player> compareHandWinners(){
        ArrayList<Player> winners = new ArrayList<>();
        for(Player p : nearestToPrincess()){
            if(p.discardPileSum() == highestDiscardPile()){
                winners.add(p);
            }
        }
        return winners;
    }

    public void drawSecretCard(Hand hand) {
        hand.getCards().add(secretCard);
        secretCard = null;
    }

    public void setRoundWon(boolean roundWon){
        this.roundWon = roundWon;
    }

    public boolean countessRule(Player p){

        String cardOne = p.getHand().getCards().get(0).getCharacter().toString();
        String cardTwo = p.getHand().getCards().get(1).getCharacter().toString();

        System.out.print("Countess rule is ");
        System.out.println(((cardOne.equals(Character.COUNTESS.toString()) || cardTwo.equals(Character.COUNTESS.toString())) &&
                ( (cardOne.equals(Character.PRINCE.toString()) || cardOne.equals(Character.KING.toString())) ||
                        (cardTwo.equals(Character.PRINCE.toString()) || cardTwo.equals(Character.KING.toString())))));

        return ((cardOne.equals(Character.COUNTESS.toString()) || cardTwo.equals(Character.COUNTESS.toString())) &&
                ( (cardOne.equals(Character.PRINCE.toString()) || cardOne.equals(Character.KING.toString())) ||
                (cardTwo.equals(Character.PRINCE.toString()) || cardTwo.equals(Character.KING.toString()))));
    }

    public String removeIDFromPlayername(String username){
        String newName = username.substring(0, username.indexOf("#"));
        return newName;
    }

    public int playersIndex(Player p){
        for(int i = 0; i < players.size(); i++){
            if(players.get(i).equals(p)){
                return i;
            }
        }
        return -1;
    }

    // CARD ACTIONS

    // Targeted cases

    public void guardAction(int sendersIndex, int cardIndex, int targetPlayersIndex, Character guess){

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getName());

        players.get(sendersIndex).discardCard(cardIndex);

        if(players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter() == guess) {
            knockOut(targetPlayersIndex);

            String msgSender = "You guessed correct!";
            String msgTarget = senderName + " uses GUARD on you and correctly guesses that you have a " + guess.toString();
            String msgOthers = senderName + " uses GUARD on correctly guessed that " +
                    targetName + " has a " + guess.toString();

            informPlayersAboutTargetedPlay(Character.GUARD.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");

//         players.get(sender).getDiscardPile().addToDiscardPile(players.get(sender).getHand().getCards().get(index));
//         players.get(sender).getHand().getCards().remove(index);
        } else {
            String msgSender = "Incorrect! You used Guard on " + targetName + " with the guess " + guess.toString();
            String msgTarget = senderName + " uses GUARD on you and incorrectly guesses that you have a " + guess.toString();
            String msgOthers = senderName + " uses GUARD on + " + targetName
                    + " and incorrectly guesses " + guess.toString();
            informPlayersAboutTargetedPlay(Character.GUARD.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
        }
    }

    public void priestAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getName());

        players.get(sendersIndex).discardCard(cardIndex);

        String msgSender = "You play PRIEST on " + targetName + ". " +
                targetName + " has a " + players.get(targetPlayersIndex).getHand().getCards().get(0);
        String msgTarget = senderName + " uses PRIEST on you.";
        String msgOthers = senderName + " plays PRIEST on " + targetName;
        informPlayersAboutTargetedPlay(Character.PRIEST.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
    }

    public void baronAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getName());

        players.get(sendersIndex).discardCard(cardIndex);

        System.out.println(senderName + " has a " + players.get(sendersIndex).getHand().getCards().get(0).getCharacter() +
        " of value " + players.get(sendersIndex).getHand().getCards().get(0).getCharacter().getValue());
        System.out.println(targetName + " has a " + players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter() +
                " of value " + players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter().getValue() + Game.newLine);

        Character senderPlayerCharacter = players.get(sendersIndex).getHand().getCards().get(0).getCharacter();
        Character targetPlayerCharacter = players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter();

        if(senderPlayerCharacter.getValue() > targetPlayerCharacter.getValue()){
            knockOut(targetPlayersIndex);

            String msgSender = "Target player has a " + targetPlayerCharacter.toString() + ". You win the duel!";
            String msgTarget = senderName + " has a " + senderPlayerCharacter.toString() + ".";
            String msgOthers = senderName + " wins the duel.";
            informPlayersAboutTargetedPlay(Character.BARON.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");

        } else if(senderPlayerCharacter.getValue() < targetPlayerCharacter.getValue()){
            knockOut(sendersIndex);

            String msgSender = "Target player has a " + targetPlayerCharacter.toString() + ". You lose the duel!";
            String msgTarget = senderName + " has a " + senderPlayerCharacter.toString() + ". You win the duel!";
            String msgOthers = targetName + " wins the duel.";
            informPlayersAboutTargetedPlay(Character.BARON.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
        } else {
            System.out.println("Tie! No one is knocked out...");

            String msgSender = "Target player has a " + targetPlayerCharacter.toString() + ". It's a draw!";
            String msgTarget = senderName + " has a " + senderPlayerCharacter.toString() + ". It's a draw!";
            String msgOthers = "It's a draw! No one is knocked out!";
            informPlayersAboutTargetedPlay(Character.BARON.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");}
    }

    public void princeAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getName());
        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getCharacter().toString();

        players.get(sendersIndex).discardCard(cardIndex);
        Card discardedCard = players.get(sendersIndex).getHand().getCards().get(0);

        if(players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter() == Character.PRINCESS){
            knockOut(targetPlayersIndex);
        } else {
            players.get(targetPlayersIndex).discardHand();
        }

        //TODO if it was a princess the client still gets a new card
        if ((!deck.getCards().isEmpty()) && players.get(targetPlayersIndex).isInRound()) {
            deck.drawCard(players.get(targetPlayersIndex).getHand());

            if(sendersIndex == targetPlayersIndex){

                String msgSender = "You play " + cardName + " on yourself and discard "
                        + discardedCard.getCharacter().toString() + " and draw "
                        + players.get(targetPlayersIndex).getHand().getCards().get(0);
                String msgOthers = senderName + " plays " + cardName + " on self and discards "
                        + discardedCard.getCharacter().toString() + " and draws a new card";

                for(Player p : players){
                    if(p.getName().equals(players.get(sendersIndex).getName())){
                        System.out.println("linie 291 " + senderName + " " + p.getName());
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgSender + "=" + players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter().toString(), p.getPlayerCipher());
                            lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgOthers + "=" + "", p.getPlayerCipher());
                            lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                String msgSender = "You play PRINCE on " + targetName + " who discard the hand and draws a new card.";
                String msgTarget = senderName + " plays PRINCE on you. You discard" +
                        " your hand and draw a " + players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter().toString();
                String msgOthers = senderName + " played PRINCE on " + targetName + " who draw a new card.";

                informPlayersAboutTargetedPlay(Character.PRINCE.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter().toString());
            }
        } else if (!players.get(targetPlayersIndex).isInRound()){
            if(sendersIndex == targetPlayersIndex){

                String msgSender = "You play " + cardName + " on yourself and discard "
                        + discardedCard.getCharacter().toString();
                String msgOthers = senderName + " plays " + cardName + " on self and discards "
                        + discardedCard.getCharacter().toString();

                for(Player p : players){
                    if(p.getName().equals(players.get(sendersIndex).getName())){
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgSender + "=", p.getPlayerCipher());
                            lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgOthers + "=" + "", p.getPlayerCipher());
                            lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                String msgSender = "You play PRINCE on " + targetName + " who discard the hand.";
                String msgTarget = senderName + " plays PRINCE on you. You discard" +
                        " your hand.";
                String msgOthers = senderName + " played PRINCE on " + targetName;

                informPlayersAboutTargetedPlay(Character.PRINCE.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
            }
        } else {
            //TODO self-target scenario
            drawSecretCard(players.get(targetPlayersIndex).getHand());

            String msgSender = "You play PRINCE on " + targetName + " who draws the secret card.";
            String msgTarget = senderName + " plays PRINCE on you. You discard" +
                    " your hand and draw the secret card " + players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter().toString();
            String msgOthers = senderName + " played PRINCE on " + targetName + " who draw the secret card.";
            informPlayersAboutTargetedPlay(Character.PRINCE.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", players.get(targetPlayersIndex).getHand().getCards().get(0).getCharacter().toString());
        }
    }

    public void kingAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getName());

        players.get(sendersIndex).discardCard(cardIndex);

        Card senderCard = players.get(sendersIndex).getHand().getCards().get(0);
        Card targetCard = players.get(targetPlayersIndex).getHand().getCards().get(0);

        players.get(sendersIndex).discardHand();
        players.get(targetPlayersIndex).discardHand();
        players.get(sendersIndex).getHand().getCards().add(targetCard);
        players.get(targetPlayersIndex).getHand().getCards().add(senderCard);

        String msgSender = "You played KING on " + targetName + " and got a " + targetCard.getCharacter().toString();
        String msgTarget = senderName + " played KING on you. You now have a " + senderCard.getCharacter().toString();
        String msgOthers = targetName + " played KING on " + senderName;

        informPlayersAboutTargetedPlay(Character.KING.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, targetCard.getCharacter().toString(), senderCard.getCharacter().toString());

    }

    // Untargeted cases

    public void noAction(int sendersIndex, int cardIndex){

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getCharacter().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());

        String msgSender = "You played " + cardName + " with no effect (no available targets)";
        String msgOthers =  senderName  + " played " + cardName + " with no effect (no available targets)";

        informPlayersAboutUntargetedPlay(cardName, sendersIndex, msgSender, msgOthers);

        // Remove card from the hand
        players.get(sendersIndex).discardCard(cardIndex);
    }

    public void handmaidAction(int sendersIndex, int cardIndex){

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getCharacter().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());

        players.get(sendersIndex).discardCard(cardIndex);
        players.get(sendersIndex).activateHandmaid();

        String msgSender = "You played " + cardName + " and is untargetable until your next turn";
        String msgOthers = senderName + " played " + cardName + " and is untargetable until " +
                senderName + "'s next turn";
        informPlayersAboutUntargetedPlay(cardName, sendersIndex, msgSender, msgOthers);
    }

    public void countessAction(int sendersIndex, int cardIndex){

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getCharacter().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());

        players.get(sendersIndex).discardCard(cardIndex);

        String msgSender = "You played " + cardName;
        String msgOthers = senderName + " played " + cardName;
        informPlayersAboutUntargetedPlay(cardName,sendersIndex, msgSender, msgOthers);
    }

    public void princessAction(int sendersIndex, int cardIndex) {

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getCharacter().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getName());

        players.get(sendersIndex).discardCard(cardIndex);
        players.get(sendersIndex).setInRound(false);

        knockOut(sendersIndex);

        String msgSender = "You played " + cardName;
        String msgOthers = senderName + " played " + cardName;
        informPlayersAboutUntargetedPlay(cardName, sendersIndex, msgSender, msgOthers);
    }

    // Knock-out scenario

    public void knockOut(int knockedOutIndex) {
        Player p = players.get(knockedOutIndex);
        System.out.println("Player " + removeIDFromPlayername(p.getName()) + " is out of the round" + Game.newLine);
        p.discardHand();
        p.setInRound(false);

        String msgOthers = "Player " + removeIDFromPlayername(p.getName()) + " is out of the round";
        String msgSender = "You are out of the round";

        for(Player rcpt : players) { // 'rcpt' for recipient
            // [0]: update, [1]: type, [2]: recipient name, [3]: knocked out player's name, [4]: Game-log message, [5]; -
            if(rcpt.getName().equals(p.getName())){
                try {
                    SealedObject encryptedMessage = new SealedObject(KNOCK_OUT + "!?" + msgSender + "=",rcpt.getPlayerCipher());
                    lobbySpace.put(CLIENT_UPDATE, encryptedMessage ,rcpt.getPlayerIndex());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    SealedObject encryptedMessage = new SealedObject(KNOCK_OUT + "!?" + msgOthers + "=",rcpt.getPlayerCipher());
                    lobbySpace.put(CLIENT_UPDATE, encryptedMessage ,rcpt.getPlayerIndex());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // INFORM PLAYERS

    public void informPlayersAboutTargetedPlay(String card, String msgSender, String msgTarget, String msgOthers, int senderIndex,
                                               int receiverIndex, String kingCardToSender, String kingCardToTarget){

        for(Player p : players){
            if(p.getName().equals(players.get(senderIndex).getName())){
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgSender + "=" + kingCardToSender,p.getPlayerCipher());
                    lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else if(p.getName().equals(players.get(receiverIndex).getName())){
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgTarget + "=" + kingCardToTarget,p.getPlayerCipher());
                    lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgOthers + "=",p.getPlayerCipher());
                    lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void informPlayersAboutUntargetedPlay(String card, int senderIndex, String msgSender, String msgOthers){
        for(Player p : players){
            if(!p.getName().equals(players.get(senderIndex).getName())){
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgOthers + "=",p.getPlayerCipher());
                    lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgSender + "=",p.getPlayerCipher());
                    lobbySpace.put(CLIENT_UPDATE, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}
