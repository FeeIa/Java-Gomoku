package gomokugame.guis.layouts;

import gomokugame.guis.elements.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static gomokugame.guis.GUI.*;

// Main game when player is in a match
public class GameMatchGui extends AnchorPane {
    public AutoTextLabel turnTimer;
    public AutoTextLabel upperText;
    public AutoTextLabel bottomText;
    public AutoButton revealStones;
    public AutoTextLabel revealAmount;
    private final VBox vBox;
    private final VBox leftVBox;

    public GameMatchGui() {
        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        bindSizeToParent(hBox, this, 0.95, 1);

        this.turnTimer = new AutoTextLabel(0.25, 0.3, hBox);
        this.turnTimer.background.setOpacity(0);

        hBox.getChildren().addAll(new HSpacer(0.75, hBox), this.turnTimer);

        this.vBox = new VBox();
        this.vBox.setAlignment(Pos.CENTER);
        bindSizeToParent(this.vBox, this, 1, 1);

        this.upperText = new AutoTextLabel(0.5, 0.0625, this.vBox);
        this.upperText.background.setOpacity(0);

        this.bottomText = new AutoTextLabel(0.5, 0.0625, this.vBox);
        this.bottomText.background.setOpacity(0);

        HBox leftHBox = new HBox();
        leftHBox.setAlignment(Pos.CENTER_LEFT);
        bindSizeToParent(leftHBox, this, 0.25, 1);

        this.leftVBox = new VBox();
        this.leftVBox.setAlignment(Pos.CENTER_LEFT);
        bindSizeToParent(this.leftVBox, leftHBox, 0.8, 1);

        leftHBox.getChildren().addAll(new HSpacer(0.15, leftHBox), this.leftVBox);

        this.revealStones = new AutoButton(1, 0.075, this.leftVBox);
        this.revealStones.setText("Reveal Stones");

        this.revealAmount = new AutoTextLabel(1, 0.075, this.leftVBox);
        this.revealAmount.background.setOpacity(0);

        this.getChildren().addAll(hBox, this.vBox, leftHBox);
    }

    public void addBoardAndInit(BoardUi boardUi) {
        this.vBox.getChildren().addAll(this.upperText, boardUi, this.bottomText);
    }

    public void hideRevealStonesButton() {
        Platform.runLater(() -> this.leftVBox.getChildren().clear());
    }

    public void showRevealStonesButton() {
        Platform.runLater(() -> this.leftVBox.getChildren().addAll(this.revealStones, this.revealAmount));
    }
}