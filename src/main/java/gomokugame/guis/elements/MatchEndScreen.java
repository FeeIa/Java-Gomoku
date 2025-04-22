package gomokugame.guis.elements;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static gomokugame.guis.GUI.bindSizeToParent;

// Ending screen when match is over
public class MatchEndScreen extends StackPane {
    public AutoTextLabel mainMessage;
    public AutoTextLabel subMessage;
    public AutoTextLabel notification;
    public HBox hBox;
    public AutoButton rematchButton;
    public AutoButton exitMatch;

    public MatchEndScreen(Region parent) {
        Rectangle background = new Rectangle();
        background.setOpacity(0.75);
        bindSizeToParent(background, parent, 1, 1);

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        bindSizeToParent(vBox, parent, 1, 1);

        this.mainMessage = new AutoTextLabel(0.5, 0.2, vBox);
        this.mainMessage.text.setFill(Color.WHITE);
        this.mainMessage.background.setOpacity(0);

        this.subMessage = new AutoTextLabel(0.75, 0.15, vBox);
        this.subMessage.text.setFill(Color.WHITE);
        this.subMessage.background.setOpacity(0);

        this.notification = new AutoTextLabel(0.75, 0.075, vBox);
        this.notification.text.setFill(Color.WHITE);
        this.notification.background.setOpacity(0);

        this.hBox = new HBox();
        this.hBox.setAlignment(Pos.CENTER);
        bindSizeToParent(this.hBox, vBox, 0.5, 0.2);

        this.rematchButton = new AutoButton(0.4, 0.4, this.hBox);
        this.rematchButton.setText("Rematch");

        this.exitMatch = new AutoButton(0.4, 0.4, this.hBox);
        this.exitMatch.setText("Exit");

        this.hBox.getChildren().addAll(this.rematchButton, new HSpacer(0.2, this.hBox), this.exitMatch);
        vBox.getChildren().addAll(this.mainMessage, this.subMessage, this.notification, this.hBox);
        this.getChildren().addAll(background, vBox);
    }
}