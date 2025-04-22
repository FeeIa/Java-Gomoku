package gomokugame.objects;

import java.io.Serializable;

public class MatchEndResult implements Serializable {
    public boolean wasAnAbort; // True if the match ended because a player left or aborted
    public String colorThatWon;
    public boolean winner;
    public boolean loser;
    public boolean spectator;
}