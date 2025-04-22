package gomokugame.objects;

import java.io.Serializable;

public class InvisibleModeOption implements Serializable {
    public int invisibleModeRevealChances;

    public InvisibleModeOption(int invisibleModeRevealChances) {
        this.invisibleModeRevealChances = invisibleModeRevealChances;
    }
}