package gomokugame.guis.layouts;

import gomokugame.Main;
import gomokugame.guis.elements.*;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static gomokugame.guis.GUI.*;

// GUI that shows after a room is created
public class RoomSettingsGui extends VBox {
    public AutoTextLabel roomName;
    public AutoTextLabel roomCreator;
    public AutoTextLabel boardSize;
    public AutoTextLabel timerPerTurn;
    public AutoTextLabel invisibleModeRevealChances;
    public AutoTextLabel connectedPlayers;
    public VBox settings;
    public DropdownList boardSizeSettings;
    public DropdownList timerPerTurnSettings;
    public DropdownList invisibleModeRevealChancesSettings;
    public AutoTextLabel notification;
    public AutoTextLabel warning;
    public HBox hBox;
    public HBox hBox2;
    public AutoButton startButton;
    public AutoButton leaveRoom;

    public RoomSettingsGui() {
        this.setAlignment(Pos.CENTER);

        AutoTextLabel titleText = new AutoTextLabel(0.2, 0.15, this);
        titleText.text.setText("Room Settings");
        titleText.background.setOpacity(0);

        this.hBox = new HBox();
        this.hBox.setAlignment(Pos.CENTER);
        bindSizeToParent(this.hBox, this, 0.5, 0.3);

        VBox roomDesc = new VBox();
        bindSizeToParent(roomDesc, this.hBox, 0.6, 1);

        this.roomName = new AutoTextLabel(1, 0.2, roomDesc);
        this.roomName.setAlignment(Pos.CENTER_LEFT);
        this.roomName.background.setOpacity(0);

        this.roomCreator = new AutoTextLabel(1, 0.2, roomDesc);
        this.roomCreator.setAlignment(Pos.CENTER_LEFT);
        this.roomCreator.background.setOpacity(0);

        this.boardSize = new AutoTextLabel(1, 0.2, roomDesc);
        this.boardSize.setAlignment(Pos.CENTER_LEFT);
        this.boardSize.background.setOpacity(0);

        this.timerPerTurn = new AutoTextLabel(1, 0.2, roomDesc);
        this.timerPerTurn.setAlignment(Pos.CENTER_LEFT);
        this.timerPerTurn.background.setOpacity(0);

        this.invisibleModeRevealChances = new AutoTextLabel(1, 0.2, roomDesc);
        this.invisibleModeRevealChances.setAlignment(Pos.CENTER_LEFT);
        this.invisibleModeRevealChances.background.setOpacity(0);

        this.connectedPlayers = new AutoTextLabel(1, 0.2, roomDesc);
        this.connectedPlayers.setAlignment(Pos.CENTER_LEFT);
        this.connectedPlayers.background.setOpacity(0);

        roomDesc.getChildren().addAll(
                this.roomName,
                this.roomCreator,
                this.boardSize,
                this.timerPerTurn,
                this.invisibleModeRevealChances,
                this.connectedPlayers
        );

        this.settings = new VBox();
        this.settings.setAlignment(Pos.TOP_CENTER);
        bindSizeToParent(this.settings, this.hBox, 0.4, 1);

        this.boardSizeSettings = new DropdownList(1, 0.15, this.settings);
        for (int size : Main.BOARD_SIZE_OPTIONS) {
            this.boardSizeSettings.getItems().add(String.format("%dx%d", size, size));
        }

        this.timerPerTurnSettings = new DropdownList(1, 0.15, this.settings);
        this.timerPerTurnSettings.getItems().add("N/A");
        for (double time : Main.TIMER_PER_TURN_OPTIONS) {
            this.timerPerTurnSettings.getItems().add(String.format("%.1fs/turn", time));
        }

        this.invisibleModeRevealChancesSettings = new DropdownList(1, 0.15, this.settings);
        this.invisibleModeRevealChancesSettings.getItems().add("N/A");
        for (int turn : Main.INVISIBLE_MODE_REVEAL_CHANCES) {
            this.invisibleModeRevealChancesSettings.getItems().add(turn + " reveal chance(s)");
        }

        this.settings.getChildren().addAll(
                new VSpacer(0.2, this.settings),
                new VSpacer(0.225, this.settings),
                this.boardSizeSettings,
                new VSpacer(0.025, this.settings),
                this.timerPerTurnSettings,
                new VSpacer(0.025, this.settings),
                this.invisibleModeRevealChancesSettings
        );
        this.hBox.getChildren().addAll(roomDesc, new HSpacer(0.2, this.hBox), this.settings);

        this.notification = new AutoTextLabel(1, 0.05, this);
        this.notification.background.setOpacity(0);

        this.hBox2 = new HBox();
        this.hBox2.setAlignment(Pos.CENTER);
        bindSizeToParent(this.hBox2, this, 0.4, 0.0625);

        this.leaveRoom = new AutoButton(0.4, 1, this.hBox2);
        this.leaveRoom.setText("Leave Room");

        this.startButton = new AutoButton(0.4, 1, this.hBox2);
        this.startButton.textProperty().set("Start Match");

        this.hBox2.getChildren().addAll(this.leaveRoom, new HSpacer(0.025, this.hBox2), this.startButton);

        this.warning = new AutoTextLabel(1, 0.05, this);
        this.warning.background.setOpacity(0);

        this.getChildren().addAll(
                titleText,
                new VSpacer(0.1, this),
                this.hBox,
                new VSpacer(0.1, this),
                this.notification,
                new VSpacer(0.025, this),
                this.hBox2,
                new VSpacer(0.025, this),
                this.warning
        );
    }

    public void setDefaultSettingsValue() {
        this.boardSizeSettings.setValue("20x20");
        this.timerPerTurnSettings.setValue("N/A");
        this.invisibleModeRevealChancesSettings.setValue("N/A");
    }
}