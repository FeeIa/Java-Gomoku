package gomokugame.guis.elements;

import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import static gomokugame.guis.GUI.*;

// Represents individual tile UI
public class TileUi extends StackPane {
    private Ellipse occupantStone;
    private Ellipse invalidMoveIndicator;
    public Ellipse hoverIndicator; // The clickable region;
    public Rectangle tileSquare;

    public TileUi(double xScale, double yScale, Region parent, boolean isInteractable) {
        this.tileSquare = new Rectangle();
        this.tileSquare.setFill(Color.TRANSPARENT);
        bindSizeToParent(this.tileSquare, parent, xScale, yScale);

        if (isInteractable) {
            this.hoverIndicator = new Ellipse();
            this.hoverIndicator.setFill(Color.TRANSPARENT);
            bindSizeToParent(this.hoverIndicator, parent, xScale / 2 * 1.625, yScale / 2 * 1.625);

            this.occupantStone = new Ellipse();
            this.occupantStone.setFill(Color.TRANSPARENT);
            bindSizeToParent(this.occupantStone, parent, xScale / 2 * 1.875, yScale / 2 * 1.875);

            this.invalidMoveIndicator = new Ellipse();
            this.invalidMoveIndicator.setFill(Color.RED);
            this.invalidMoveIndicator.setOpacity(0);
            bindSizeToParent(this.invalidMoveIndicator, parent, xScale / 2 * 2, yScale / 2 * 2);

            this.getChildren().addAll(this.hoverIndicator, this.occupantStone, this.invalidMoveIndicator);
        } else {
            this.tileSquare.setStroke(Color.BLACK);
        }

        this.getChildren().add(this.tileSquare);
    }

    public void showOccupant(String occupant) {
        FillTransition fadeInAnim = new FillTransition(
                Duration.millis(25),
                this.occupantStone,
                Color.TRANSPARENT,
                occupant.equals("WHITE") ? Color.WHITE : Color.BLACK
        );
        fadeInAnim.play();
    }

    public void hideOccupant() {
        this.occupantStone.setFill(Color.TRANSPARENT);
    }

    public void indicateInvalidMove() {
        FadeTransition flashingAnim = new FadeTransition(Duration.millis(250), this.invalidMoveIndicator);
        flashingAnim.setFromValue(1.0);
        flashingAnim.setToValue(0.2);
        flashingAnim.setCycleCount(4);
        flashingAnim.setAutoReverse(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this.invalidMoveIndicator);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0); // Fully invisible
        fadeOut.setCycleCount(1);

        SequentialTransition seq = new SequentialTransition(flashingAnim, fadeOut);
        seq.setOnFinished(event -> this.invalidMoveIndicator.setOpacity(0));
        seq.play();
    }
}