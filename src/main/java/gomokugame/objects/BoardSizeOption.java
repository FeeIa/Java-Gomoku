package gomokugame.objects;

import java.io.Serializable;

public class BoardSizeOption implements Serializable {
    public int boardSize;

    public BoardSizeOption(int boardSize) {
        this.boardSize = boardSize;
    }
}