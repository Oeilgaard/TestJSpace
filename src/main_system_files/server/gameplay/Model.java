package main_system_files.server.gameplay;

import main_system_files.server.LobbyUser;
import org.jspace.SequentialSpace;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.util.ArrayList;

public class Model {

    public final static int AFFECTION_GOAL_TWO_PLAYERS = 7;
    public final static int AFFECTION_GOAL_THREE_PLAYERS = 5;
    public final static int AFFECTION_GOAL_FOUR_PLAYERS = 4;

    public final static int REVEALED_CARDS_TWO_PLAYER = 3;

    public final static int NO_OF_GUARD = 5;
    public final static int NO_OF_PRIEST = 2;
    public final static int NO_OF_BARON = 2;
    public final static int NO_OF_HANDMAID = 2;
    public final static int NO_OF_PRINCE = 2;
    public final static int NO_OF_KING = 1;
    public final static int NO_OF_COUNTESS = 1;
    public final static int NO_OF_PRINCESS = 1;

    public final static int S2C_GAME = 10;
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

    public final static int C2S_TARGETS_REQ = 85;
    public final static int S2C_TARGETS_RESP = 86;

    public final static int C2S_LOBBY_GAME = 20;
    //public final static int DISCARD = 21;

    protected ArrayList<Player> players;
    protected int turn;
    protected int affectionGoal;
    protected int playerPointer; // index of the current player's turn
    protected int round;
    protected Deck deck;
    protected boolean roundWon;
    protected ArrayList<Card> revealedCards;
    public Card secretCard;
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
            this.players.add(new Player(user.userID,user.userNr,user.personalCipher));
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

    public Player getUserfromUserID(String UserID) {
        for (Player user : players){
            if(user.getuserID().equals(UserID)){
                return user;
            }
        }
        return null;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public void nextTurn() { turn++; }

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
            System.out.println(p.getuserID() + "'s affection is: " + p.getAffection());
            System.out.println("The goal: " + affectionGoal);
            if(p.getAffection()==affectionGoal){
                return p.getuserID();
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
                max = p.discardPileSum();
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

        String cardOne = p.getHand().getCards().get(0).getRole().toString();
        String cardTwo = p.getHand().getCards().get(1).getRole().toString();

        System.out.print("Countess rule is ");
        System.out.println(((cardOne.equals(Role.COUNTESS.toString()) || cardTwo.equals(Role.COUNTESS.toString())) &&
                ( (cardOne.equals(Role.PRINCE.toString()) || cardOne.equals(Role.KING.toString())) ||
                        (cardTwo.equals(Role.PRINCE.toString()) || cardTwo.equals(Role.KING.toString())))));

        return ((cardOne.equals(Role.COUNTESS.toString()) || cardTwo.equals(Role.COUNTESS.toString())) &&
                ( (cardOne.equals(Role.PRINCE.toString()) || cardOne.equals(Role.KING.toString())) ||
                (cardTwo.equals(Role.PRINCE.toString()) || cardTwo.equals(Role.KING.toString()))));
    }

    public String removeIDFromPlayername(String username){
        return username.substring(0, username.indexOf("#"));
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

    public void guardAction(int sendersIndex, int cardIndex, int targetPlayersIndex, Role guess){

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getuserID());

        players.get(sendersIndex).discardCard(cardIndex);

        if(players.get(targetPlayersIndex).getHand().getCards().get(0).getRole() == guess) {
            knockOut(targetPlayersIndex);

            String msgSender = "You used GUARD guessed correct with the guess " + guess.toString() + "!";
            String msgTarget = senderName + " uses GUARD on you and correctly guesses that you have a " + guess.toString();
            String msgOthers = senderName + " uses GUARD and correctly guessed that " +
                    targetName + " has a " + guess.toString();

            informPlayersAboutTargetedPlay(Role.GUARD.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");

//         players.get(sender).getDiscardPile().addToDiscardPile(players.get(sender).getHand().getCards().get(index));
//         players.get(sender).getHand().getCards().remove(index);
        } else {
            String msgSender = "Incorrect! You used GUARD on " + targetName + " with the guess " + guess.toString();
            String msgTarget = senderName + " uses GUARD on you and incorrectly guesses that you have a " + guess.toString();
            String msgOthers = senderName + " uses GUARD on + " + targetName
                    + " and incorrectly guesses " + guess.toString();
            informPlayersAboutTargetedPlay(Role.GUARD.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
        }
    }

    public void priestAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getuserID());

        players.get(sendersIndex).discardCard(cardIndex);

        String msgSender = "You play PRIEST on " + targetName + ". " +
                targetName + " has a " + players.get(targetPlayersIndex).getHand().getCards().get(0);
        String msgTarget = senderName + " uses PRIEST on you.";
        String msgOthers = senderName + " plays PRIEST on " + targetName;
        informPlayersAboutTargetedPlay(Role.PRIEST.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
    }

    public void baronAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getuserID());

        players.get(sendersIndex).discardCard(cardIndex);

        System.out.println(senderName + " has a " + players.get(sendersIndex).getHand().getCards().get(0).getRole() +
        " of value " + players.get(sendersIndex).getHand().getCards().get(0).getRole().getValue());
        System.out.println(targetName + " has a " + players.get(targetPlayersIndex).getHand().getCards().get(0).getRole() +
                " of value " + players.get(targetPlayersIndex).getHand().getCards().get(0).getRole().getValue() + Game.newLine);

        Role senderPlayerCharacter = players.get(sendersIndex).getHand().getCards().get(0).getRole();
        Role targetPlayerCharacter = players.get(targetPlayersIndex).getHand().getCards().get(0).getRole();

        if(senderPlayerCharacter.getValue() > targetPlayerCharacter.getValue()){
            knockOut(targetPlayersIndex);

            String msgSender = "You used BARON on " +targetName+" who has a " + targetPlayerCharacter.toString() + ". You win the duel with a " + senderPlayerCharacter.toString() + "!";
            String msgTarget = senderName + " used BARON on you and has a " + senderPlayerCharacter.toString() + ". You lost the duel with a " + targetPlayerCharacter.toString() + "!";
            String msgOthers = senderName + " used a baron on " + targetName + " and " + senderName + " won the duel.";
            informPlayersAboutTargetedPlay(Role.BARON.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");

        } else if(senderPlayerCharacter.getValue() < targetPlayerCharacter.getValue()){
            knockOut(sendersIndex);

            String msgSender = "You used BARON on " +targetName+" who has a " + targetPlayerCharacter.toString() + ". You lost the duel with a " + senderPlayerCharacter.toString() + "!";
            String msgTarget = senderName + " used BARON on you and has a " + senderPlayerCharacter.toString() + ". You won the duel with a " + targetPlayerCharacter.toString() + "!";
            String msgOthers = senderName + " used a BARON on " + targetName + " and " + targetName + " won the duel.";
            informPlayersAboutTargetedPlay(Role.BARON.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
        } else {
            System.out.println("Tie! No one is knocked out...");

            String msgSender = "You used BARON on " + targetName + ". You both have a " + targetPlayerCharacter.toString() + ". It's a draw!";
            String msgTarget = senderName + " used BARON on you. You both have a " + targetPlayerCharacter.toString() + ". It's a draw!";
            String msgOthers = senderName + "used BARON on " + targetName +". It's a draw! No one is knocked out!";
            informPlayersAboutTargetedPlay(Role.BARON.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");}
    }

    public void princeAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getuserID());
        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getRole().toString();

        players.get(sendersIndex).discardCard(cardIndex);
        Card discardedCard = players.get(sendersIndex).getHand().getCards().get(0);

        if(players.get(targetPlayersIndex).getHand().getCards().get(0).getRole() == Role.PRINCESS){
            knockOut(targetPlayersIndex);
        } else {
            players.get(targetPlayersIndex).discardHand();
        }

        //Fixed if it was a princess the client still gets a new card
        if ((!deck.getCards().isEmpty()) && players.get(targetPlayersIndex).isInRound()) {
            deck.drawCard(players.get(targetPlayersIndex).getHand());

            if(sendersIndex == targetPlayersIndex){

                String msgSender = "You play " + cardName + " on yourself and discard "
                        + discardedCard.getRole().toString() + " and draw "
                        + players.get(targetPlayersIndex).getHand().getCards().get(0);
                String msgOthers = senderName + " plays " + cardName + " on self and discards "
                        + discardedCard.getRole().toString() + " and draws a new card";

                for(Player p : players){
                    if(p.getuserID().equals(players.get(sendersIndex).getuserID())){
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgSender + "=" + players.get(targetPlayersIndex).getHand().getCards().get(0).getRole().toString(), p.getPlayerCipher());
                            lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgOthers + "=" + "", p.getPlayerCipher());
                            lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                String msgSender = "You play PRINCE on " + targetName + " who discards " + discardedCard.getRole().toString() + " and draws a new card.";
                String msgTarget = senderName + " plays PRINCE on you. You discard" +
                        discardedCard.getRole().toString() + " and draw a " + players.get(targetPlayersIndex).getHand().getCards().get(0).getRole().toString();
                String msgOthers = senderName + " played PRINCE on " + targetName + " who discards " + discardedCard.getRole().toString() + " and draw a new card.";

                informPlayersAboutTargetedPlay(Role.PRINCE.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", players.get(targetPlayersIndex).getHand().getCards().get(0).getRole().toString());
            }
        } else if (!players.get(targetPlayersIndex).isInRound()){
            if(sendersIndex == targetPlayersIndex){

                String msgSender = "You play " + cardName + " on yourself and discard "
                        + discardedCard.getRole().toString();
                String msgOthers = senderName + " plays " + cardName + " on self and discards "
                        + discardedCard.getRole().toString();

                for(Player p : players){
                    if(p.getuserID().equals(players.get(sendersIndex).getuserID())){
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgSender + "=", p.getPlayerCipher());
                            lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + cardName + "?" + msgOthers + "=" + "", p.getPlayerCipher());
                            lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                        } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                String msgSender = "You play PRINCE on " + targetName + " who discards "  + discardedCard.getRole().toString() + " and draw a new card.";
                String msgTarget = senderName + " plays PRINCE on you. You discard " + discardedCard.getRole().toString() + " and draw a new card";
                String msgOthers = senderName + " played PRINCE on " + targetName + " who discards "  + discardedCard.getRole().toString() + " and draw a new card.";

                informPlayersAboutTargetedPlay(Role.PRINCE.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", "");
            }
        } else {
            //TODO self-target scenario
            drawSecretCard(players.get(targetPlayersIndex).getHand());

            String msgSender = "You play PRINCE on " + targetName + " who draws the secret card.";
            String msgTarget = senderName + " plays PRINCE on you. You discard your hand and draw the secret card " + players.get(targetPlayersIndex).getHand().getCards().get(0).getRole().toString();
            String msgOthers = senderName + " played PRINCE on " + targetName + " who drew the secret card.";
            informPlayersAboutTargetedPlay(Role.PRINCE.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, "", players.get(targetPlayersIndex).getHand().getCards().get(0).getRole().toString());
        }
    }

    public void kingAction(int sendersIndex, int cardIndex, int targetPlayersIndex) {

        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());
        String targetName = removeIDFromPlayername(players.get(targetPlayersIndex).getuserID());

        players.get(sendersIndex).discardCard(cardIndex);

        Card senderCard = players.get(sendersIndex).getHand().getCards().get(0);
        Card targetCard = players.get(targetPlayersIndex).getHand().getCards().get(0);

        players.get(sendersIndex).discardHand();
        players.get(targetPlayersIndex).discardHand();
        players.get(sendersIndex).getHand().getCards().add(targetCard);
        players.get(targetPlayersIndex).getHand().getCards().add(senderCard);

        String msgSender = "You played KING on " + targetName + " and got a " + targetCard.getRole().toString();
        String msgTarget = senderName + " played KING on you. You now have a " + senderCard.getRole().toString();
        String msgOthers = targetName + " played KING on " + senderName;

        informPlayersAboutTargetedPlay(Role.KING.toString(), msgSender, msgTarget, msgOthers, sendersIndex, targetPlayersIndex, targetCard.getRole().toString(), senderCard.getRole().toString());

    }

    // Untargeted cases

    public void noAction(int sendersIndex, int cardIndex){

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getRole().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());

        String msgSender = "You played " + cardName + " with no effect (no available targets)";
        String msgOthers =  senderName  + " played " + cardName + " with no effect (no available targets)";

        informPlayersAboutUntargetedPlay(cardName, sendersIndex, msgSender, msgOthers);

        // Remove card from the hand
        players.get(sendersIndex).discardCard(cardIndex);
    }

    public void handmaidAction(int sendersIndex, int cardIndex){

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getRole().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());

        players.get(sendersIndex).discardCard(cardIndex);
        players.get(sendersIndex).activateHandmaid();

        String msgSender = "You played " + cardName + " and is untargetable until your next turn";
        String msgOthers = senderName + " played " + cardName + " and is untargetable until " +
                senderName + "'s next turn";
        informPlayersAboutUntargetedPlay(cardName, sendersIndex, msgSender, msgOthers);
    }

    public void countessAction(int sendersIndex, int cardIndex){

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getRole().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());

        players.get(sendersIndex).discardCard(cardIndex);

        String msgSender = "You played " + cardName;
        String msgOthers = senderName + " played " + cardName;
        informPlayersAboutUntargetedPlay(cardName,sendersIndex, msgSender, msgOthers);
    }

    public void princessAction(int sendersIndex, int cardIndex) {

        String cardName = players.get(sendersIndex).getHand().getCards().get(cardIndex).getRole().toString();
        String senderName = removeIDFromPlayername(players.get(sendersIndex).getuserID());

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
        System.out.println("Player " + removeIDFromPlayername(p.getuserID()) + " is out of the round " + Game.newLine);
        p.discardHand();
        p.setInRound(false);

        String msgOthers = "Player " + removeIDFromPlayername(p.getuserID()) + " is out of the round";
        String msgSender = "You are out of the round";

        for(Player rcpt : players) { // 'rcpt' for recipient
            // [0]: update, [1]: type, [2]: recipient name, [3]: knocked out player's name, [4]: Game-log message, [5]; -
            if(rcpt.getuserID().equals(p.getuserID())){
                try {
                    SealedObject encryptedMessage = new SealedObject(KNOCK_OUT + "!?" + msgSender + "=",rcpt.getPlayerCipher());
                    lobbySpace.put(S2C_GAME, encryptedMessage ,rcpt.getPlayerIndex());
                } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else try {
                SealedObject encryptedMessage = new SealedObject(KNOCK_OUT + "!?" + msgOthers + "=", rcpt.getPlayerCipher());
                lobbySpace.put(S2C_GAME, encryptedMessage, rcpt.getPlayerIndex());
            } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
    }

    // INFORM PLAYERS

    public void informPlayersAboutTargetedPlay(String card, String msgSender, String msgTarget, String msgOthers, int senderIndex,
                                               int receiverIndex, String kingCardToSender, String kingCardToTarget){

        for(Player p : players){
            if(p.getuserID().equals(players.get(senderIndex).getuserID())){
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgSender + "=" + kingCardToSender,p.getPlayerCipher());
                    lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else if(p.getuserID().equals(players.get(receiverIndex).getuserID())){
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgTarget + "=" + kingCardToTarget,p.getPlayerCipher());
                    lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgOthers + "=",p.getPlayerCipher());
                    lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void informPlayersAboutUntargetedPlay(String card, int senderIndex, String msgSender, String msgOthers){
        for(Player p : players){
            if(!p.getuserID().equals(players.get(senderIndex).getuserID())){
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgOthers + "=",p.getPlayerCipher());
                    lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    SealedObject encryptedMessage = new SealedObject(OUTCOME + "!" + card + "?" + msgSender + "=",p.getPlayerCipher());
                    lobbySpace.put(S2C_GAME, encryptedMessage, p.getPlayerIndex());
                } catch (InterruptedException | IOException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}