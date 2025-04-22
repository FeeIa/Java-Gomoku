package gomokugame.objects;

import java.io.Serializable;

// Represents the NxN board of the game
public class Board implements Serializable {
    public Tile[][] boardArray; // Represents the playing field
    public int size;

    public Board(int size) {
        this.size = size;
        this.boardArray = new Tile[size + 1][size + 1]; // The playing field so add 1
    }
}