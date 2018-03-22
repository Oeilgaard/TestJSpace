package MasterLobbyListServerTest.Server_Part.Gameplay;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;

import java.util.ArrayList;

public class PossibleTargetsThread implements Runnable{

    SequentialSpace lobbySpace;
    Model model;
    ArrayList<String> targets;

    public PossibleTargetsThread(SequentialSpace lobbySpace, Model model){
        this.lobbySpace = lobbySpace;
        this.model = model;
        targets = new ArrayList<>();
    }

    @Override
    public void run() {
        while(true) {
            try {

                Object[] tuple = lobbySpace.get(new ActualField("TargetablePlayersRequest"), new FormalField(String.class), new FormalField(Boolean.class));

                targets.clear();

                if (!(Boolean) tuple[2]) {
                    for (Player p : model.players) {
                        if (p.isInRound() && !p.isHandMaidProtected() && !p.isMe((String) tuple[1])) {
                            String s = p.getName();
                            s = s.substring(0, s.indexOf("#"));
                            targets.add(s);
                        }
                    }
                } else {
                    for (Player p : model.players) {
                        if (p.isInRound() && !p.isHandMaidProtected()) {
                            String s = p.getName();
                            s = s.substring(0, s.indexOf("#"));
                            targets.add(s);
                        }
                    }
                }

                lobbySpace.put("TargetablePlayersResponse", tuple[1], targets);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}