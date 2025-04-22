package gomokugame.objects;

import java.io.Serializable;

public class Move implements Serializable {
    public int targetRow;
    public int targetCol;
    public String moveMaker; // Either WHITE or BLACK

    public Move(int targetRow, int targetCol) {
        this.targetRow = targetRow;
        this.targetCol = targetCol;
    }
}