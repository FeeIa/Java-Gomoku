package gomokugame.guis.layouts;

import gomokugame.guis.elements.*;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

// Room creation when you create a room
public class RoomCreationGui extends VBox {
    public AutoTextField roomNameTextField;
    public AutoButton createRoomButton;

    public RoomCreationGui() {
        this.setAlignment(Pos.CENTER);

        AutoTextLabel titleText = new AutoTextLabel(1, 0.125, this);
        titleText.text.setText("Create Room");
        titleText.background.setOpacity(0);

        AutoTextLabel roomNameTitleText = new AutoTextLabel(0.2, 0.075, this);
        roomNameTitleText.text.setText("Enter Room Name:");
        roomNameTitleText.background.setOpacity(0);

        this.roomNameTextField = new AutoTextField(0.2, 0.0625, this);
        this.roomNameTextField.setText("");

        this.createRoomButton = new AutoButton(0.2, 0.05, this);
        this.createRoomButton.setText("Create");

        this.getChildren().addAll(titleText, roomNameTitleText, this.roomNameTextField, this.createRoomButton);
    }
}