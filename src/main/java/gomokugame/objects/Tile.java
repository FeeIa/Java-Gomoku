package gomokugame.objects;

import gomokugame.guis.elements.TileUi;

import java.io.Serializable;

public class Tile implements Serializable {
    public TileUi ui; // The visual representation
    public int row;
    public int col;
    public String occupant; // Either WHITE or BLACK

    public Tile(int row, int col) {
        this.row = row;
        this.col = col;
    }
}