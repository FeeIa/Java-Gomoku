package gomokugame.objects;

import java.io.Serializable;

public class MoveRequest implements Serializable {
    public int timerPerTurnInMilliseconds;

    public MoveRequest(int timerPerTurnInMilliseconds) {
        this.timerPerTurnInMilliseconds = timerPerTurnInMilliseconds;
    }
}