package gomokugame.guis.layouts;

import gomokugame.guis.elements.*;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static gomokugame.guis.GUI.*;

// Room list
public class RoomListGui extends VBox {
    public AutoTextLabel descriptionText;
    public VerticalScrollingContainer roomsContainer;
    public AutoButton backButton;
    public AutoButton createRoomButton;

    public RoomListGui() {
        this.setAlignment(Pos.CENTER);

        AutoTextLabel titleText = new AutoTextLabel(0.5, 0.15, this);
        titleText.text.setText("Room List");
        titleText.background.setOpacity(0);

        this.roomsContainer = new VerticalScrollingContainer(0.5, 0.5, this);

        this.descriptionText = new AutoTextLabel(0.5, 0.1, this);
        this.descriptionText.background.setOpacity(0);

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        bindSizeToParent(hBox, this, 1, 0.075);

        this.backButton = new AutoButton(0.15, 0.05, this);
        this.backButton.textProperty().set("Back");

        this.createRoomButton = new AutoButton(0.15, 0.05, this);
        this.createRoomButton.textProperty().set("Create Room");

        hBox.getChildren().addAll(this.backButton, new HSpacer(0.01, hBox), this.createRoomButton);
        this.getChildren().addAll(titleText, this.descriptionText, this.roomsContainer, hBox);
    }
}