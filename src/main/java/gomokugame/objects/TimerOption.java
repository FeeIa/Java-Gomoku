package gomokugame.objects;

import java.io.Serializable;

public class TimerOption implements Serializable {
    public int timerPerTurnInMilliseconds;

    public TimerOption(int timerPerTurnInMilliseconds) {
        this.timerPerTurnInMilliseconds = timerPerTurnInMilliseconds;
    }
}