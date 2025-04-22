package gomokugame.guis.elements;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static gomokugame.guis.GUI.*;

// Represents the listed room UI in room list
public class ListedRoomUi extends StackPane {
    public Rectangle background;
    public AutoTextLabel roomName;
    public AutoTextLabel roomId;
    public AutoTextLabel roomCreator;
    public AutoTextLabel playersAmount;
    public AutoButton joinButton;
    public AutoTextLabel matchStatus;

    public ListedRoomUi(double xScale, double yScale, Region parent) {
        this.background = new Rectangle();
        this.background.setOpacity(0.5);
        this.background.setFill(Color.ORANGE);
        bindSizeToParent(this.background, parent, xScale, yScale);

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        bindSizeToParent(hBox, parent, xScale, yScale);

        VBox roomDesc = new VBox();
        bindSizeToParent(roomDesc, hBox, 0.4, 1);

        this.roomName = new AutoTextLabel(1, 0.2, roomDesc);
        this.roomName.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(this.roomName, new Insets(5, 0, 0, 5));
        this.roomName.background.setOpacity(0);

        this.roomId = new AutoTextLabel(1, 0.2, roomDesc);
        this.roomId.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(this.roomId, new Insets(0, 0, 0, 5));
        this.roomId.background.setOpacity(0);

        this.roomCreator = new AutoTextLabel(1, 0.2, roomDesc);
        this.roomCreator.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(this.roomCreator, new Insets(0, 0, 0, 5));
        this.roomCreator.background.setOpacity(0);

        this.playersAmount = new AutoTextLabel(1, 0.2, roomDesc);
        this.playersAmount.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(this.playersAmount, new Insets(0, 0, 0, 5));
        this.playersAmount.background.setOpacity(0);

        roomDesc.getChildren().addAll(this.roomName, this.roomId, this.roomCreator, this.playersAmount);

        VBox gameStatusDesc = new VBox();
        gameStatusDesc.setAlignment(Pos.CENTER);
        bindSizeToParent(gameStatusDesc, hBox, 0.2, 1);

        this.joinButton = new AutoButton(1, 0.3, gameStatusDesc);
        this.joinButton.setText("Join Room");

        this.matchStatus = new AutoTextLabel(1, 0.2, gameStatusDesc);
        this.matchStatus.background.setOpacity(0);

        gameStatusDesc.getChildren().addAll(this.joinButton, this.matchStatus);

        hBox.getChildren().addAll(roomDesc, new HSpacer(0.375, hBox), gameStatusDesc);
        this.getChildren().addAll(this.background, hBox);
    }
}
