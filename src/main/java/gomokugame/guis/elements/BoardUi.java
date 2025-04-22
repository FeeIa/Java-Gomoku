package gomokugame.guis.elements;

import gomokugame.objects.Board;
import gomokugame.objects.Tile;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import static gomokugame.guis.GUI.*;

// Represents the board UI
public class BoardUi extends StackPane {
    public BoardUi(Board board, double xScale, double yScale, Region parent) {
        // Create the visual tile
        final double VISUAL_TILES_SIZE_SCALE = 1.0 / board.size;

        GridPane visualTilesGrid = new GridPane();
        visualTilesGrid.setBackground(new Background(new BackgroundFill(Color.BURLYWOOD, null, null)));
        bindSizeToParent(visualTilesGrid, parent, xScale, yScale);

        for (int row = 0; row < (board.size); row++) {
            // Column constraint (fixed size for columns)
            ColumnConstraints colConstraint = new ColumnConstraints();
            colConstraint.setPercentWidth(VISUAL_TILES_SIZE_SCALE * 100);
            visualTilesGrid.getColumnConstraints().add(colConstraint);

            // Row constraint (fixed size for rows)
            RowConstraints rowConstraint = new RowConstraints();
            rowConstraint.setPercentHeight(VISUAL_TILES_SIZE_SCALE * 100);
            visualTilesGrid.getRowConstraints().add(rowConstraint);

            for (int col = 0; col < (board.size); col++) {
                TileUi tile = new TileUi(VISUAL_TILES_SIZE_SCALE, VISUAL_TILES_SIZE_SCALE, visualTilesGrid, false);
                visualTilesGrid.add(tile, col, row);
            }
        }

        // Create the actual playing tile
        Tile[][] boardArray = board.boardArray;
        final double ACTUAL_TILES_SIZE_SCALE = 1.0 / boardArray.length;

        GridPane actualTilesGrid = new GridPane();
        actualTilesGrid.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        bindSizeToParent(actualTilesGrid, parent, xScale * (1 + VISUAL_TILES_SIZE_SCALE), yScale * (1 + VISUAL_TILES_SIZE_SCALE));

        // Iterating through the current tiles in boardArray
        for (Tile[] tiles : boardArray) {
            // Column constraint (fixed size for columns)
            ColumnConstraints colConstraint = new ColumnConstraints();
            colConstraint.setPercentWidth(ACTUAL_TILES_SIZE_SCALE * 100);
            actualTilesGrid.getColumnConstraints().add(colConstraint);

            // Row constraint (fixed size for rows)
            RowConstraints rowConstraint = new RowConstraints();
            rowConstraint.setPercentHeight(ACTUAL_TILES_SIZE_SCALE * 100);
            actualTilesGrid.getRowConstraints().add(rowConstraint);

            for (Tile tile : tiles) {
                tile.ui = new TileUi(ACTUAL_TILES_SIZE_SCALE / 2, ACTUAL_TILES_SIZE_SCALE / 2, actualTilesGrid, true);

                if (tile.occupant != null) {
                    tile.ui.showOccupant(tile.occupant);
                }

                actualTilesGrid.add(tile.ui, tile.col, tile.row);
            }
        }

        this.getChildren().addAll(visualTilesGrid, actualTilesGrid);
    }
}