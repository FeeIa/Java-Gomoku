package org.example.gomokugame;

import gomokugame.objects.GameObject;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

// Everything is made to scale to their parent node.
// They either inherit an existing element or new one
// Texts can accept fontSize, if no fontSize is explicitly specified then it auto-fits to its parent container

public class GUI {
    /// INDIVIDUAL UIs
    // Represents a button UI that automatically scales according to their parent
    public static class AutoButton extends Button{
        public AutoButton(double xScale, double yScale, double fontSize, Region parent) {
            bindSizeToParent(this, parent, xScale, yScale);
            bindFontSizeToParentSize(this, parent, fontSize);
        }

        public AutoButton(double xScale, double yScale, Region parent) {
            bindSizeToParent(this, parent, xScale, yScale);
            bindFontToAlwaysFit(this, this);
        }
    }

    // Represents a text with a background behind it, uses AutoText as the actual text, scales to its parent
    public static class AutoTextLabel extends StackPane {
        protected Rectangle background;
        protected Text text;

        public AutoTextLabel(double xScale, double yScale, double fontSize, Region parent) {
            this.background = new Rectangle();
            bindSizeToParent(this.background, parent, xScale, yScale);

            this.text = new Text();
            bindFontSizeToParentSize(this.text, parent, fontSize);

            this.getChildren().addAll(this.background, this.text);
        }

        public AutoTextLabel(double xScale, double yScale, Region parent) {
            this.background = new Rectangle();
            bindSizeToParent(this.background, parent, xScale, yScale);

            this.text = new Text();
            bindFontToAlwaysFit(this.text, this);

            this.getChildren().addAll(this.background, this.text);
        }
    }

    // Represents a fill-able text field that scales to its parent
    public static class AutoTextField extends TextField {
        public AutoTextField(double xScale, double yScale, double fontSize, Region parent) {
            bindSizeToParent(this, parent, xScale, yScale);

            this.setEditable(true);
            this.setAlignment(Pos.CENTER);

            bindFontSizeToParentSize(this, parent, fontSize);
        }

        public AutoTextField(double xScale, double yScale, Region parent) {
            bindSizeToParent(this, parent, xScale, yScale);

            this.setEditable(true);
            this.setAlignment(Pos.CENTER);

            bindFontToAlwaysFit(this, this);
        }
    }

    // Represents a dropdown list that scales to its parent
    public static class DropdownList extends ComboBox<String> {
        public DropdownList(double xScale, double yScale, Region parent) {
            bindSizeToParent(this, parent, xScale, yScale);
            bindFontToAlwaysFit(this, this);
        }
    }

    // Horizontal spacer (empty) element for spacing purposes
    public static class HSpacer extends Region {
        public HSpacer(double xScale, Region parent) {
            this.minWidthProperty().bind(Bindings.multiply(parent.minWidthProperty(), xScale));
            this.maxWidthProperty().bind(Bindings.multiply(parent.maxWidthProperty(), xScale));
        }
    }

    // Vertical spacer (empty) element for spacing purposes
    public static class VSpacer extends Region {
        public VSpacer(double yScale, Region parent) {
            this.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
            this.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
        }
    }

    // Represents a vertical scrolling container that scales to its parent
    public static class VerticalScrollingContainer extends ScrollPane {
        protected VBox content;

        public VerticalScrollingContainer(double xScale, double yScale, Region parent) {
            bindSizeToParent(this, parent, xScale, yScale);

            this.setHbarPolicy(ScrollBarPolicy.NEVER);

            this.content = new VBox();
            this.setContent(this.content);
        }

        public void addElement(Node element) {
            this.content.getChildren().add(element);
        }

        public void removeElement(Node element) {
            this.content.getChildren().remove(element);
        }

        public void removeAllElements() {
            this.content.getChildren().clear();
        }
    }

    // Represents the board UI
    public static class BoardUi extends StackPane {
        public BoardUi(GameObject.Board board, double xScale, double yScale, Region parent) {
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
            GameObject.Tile[][] boardArray = board.boardArray;
            final double ACTUAL_TILES_SIZE_SCALE = 1.0 / boardArray.length;

            GridPane actualTilesGrid = new GridPane();
            actualTilesGrid.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
            bindSizeToParent(actualTilesGrid, parent, xScale * (1 + VISUAL_TILES_SIZE_SCALE), yScale * (1 + VISUAL_TILES_SIZE_SCALE));

            // Iterating through the current tiles in boardArray
            for (GameObject.Tile[] tiles : boardArray) {
                // Column constraint (fixed size for columns)
                ColumnConstraints colConstraint = new ColumnConstraints();
                colConstraint.setPercentWidth(ACTUAL_TILES_SIZE_SCALE * 100);
                actualTilesGrid.getColumnConstraints().add(colConstraint);

                // Row constraint (fixed size for rows)
                RowConstraints rowConstraint = new RowConstraints();
                rowConstraint.setPercentHeight(ACTUAL_TILES_SIZE_SCALE * 100);
                actualTilesGrid.getRowConstraints().add(rowConstraint);

                for (GameObject.Tile tile : tiles) {
                    tile.ui = new TileUi(ACTUAL_TILES_SIZE_SCALE / 2, ACTUAL_TILES_SIZE_SCALE / 2, actualTilesGrid, true);

                    if (tile.occupant != null) {
                        tile.ui.addOccupant(tile.occupant);
                    }

                    actualTilesGrid.add(tile.ui, tile.col, tile.row);
                }
            }

            this.getChildren().addAll(visualTilesGrid, actualTilesGrid);
        }
    }

    // Represents individual tile UI
    public static class TileUi extends StackPane {
        protected Ellipse occupantStone;
        protected Rectangle tileSquare;
        protected Ellipse hoverIndicator; // The clickable region;
        protected Ellipse invalidMoveIndicator;

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

        public void addOccupant(String occupant) {
            FillTransition fadeInAnim = new FillTransition(
                    Duration.millis(25),
                    this.occupantStone,
                    Color.TRANSPARENT,
                    occupant.equals("WHITE") ? Color.WHITE : Color.BLACK
            );
            fadeInAnim.play();
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

    // Represents the listed room UI in room list
    public static class ListedRoomUi extends StackPane {
        protected Rectangle background;
        protected AutoTextLabel roomName;
        protected AutoTextLabel roomId;
        protected AutoTextLabel roomCreator;
        protected AutoTextLabel playersAmount;
        protected AutoButton joinButton;
        protected AutoTextLabel matchStatus;

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

    // Ending screen when match is over
    public static class MatchEndScreen extends StackPane {
        protected Rectangle background;
        protected AutoTextLabel mainMessage;
        protected AutoTextLabel subMessage;
        protected AutoTextLabel notification;
        protected HBox hBox;
        protected AutoButton rematchButton;
        protected AutoButton exitMatch;

        public MatchEndScreen(Region parent) {
            this.background = new Rectangle();
            this.background.setOpacity(0.75);
            bindSizeToParent(this.background, parent, 1, 1);

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
            this.getChildren().addAll(this.background, vBox);
        }
    }


    /// Whole GUIs
    // Main menu
    public static class MainMenuGui extends VBox {
        protected AutoTextLabel titleScreenText;
        protected AutoButton startGameButton;
        protected AutoButton settingsButton;

        public MainMenuGui() {
            this.setAlignment(Pos.CENTER);

            this.titleScreenText = new AutoTextLabel(0.75, 0.25, this);
            this.titleScreenText.text.setText("a go game");
            this.titleScreenText.background.setOpacity(0);

            this.startGameButton = new AutoButton(0.4, 0.1, this);
            this.startGameButton.setText("Start Game");

            this.settingsButton = new AutoButton(0.4, 0.1, this);
            this.settingsButton.setText("Settings");

            this.getChildren().addAll(this.titleScreenText, this.startGameButton, this.settingsButton);
        }
    }

    // Room list
    public static class RoomListGui extends VBox {
        protected AutoTextLabel titleText;
        protected AutoTextLabel descriptionText;
        protected VerticalScrollingContainer roomsContainer;
        protected AutoButton backButton;
        protected AutoButton createRoomButton;

        public RoomListGui() {
            this.setAlignment(Pos.CENTER);

            this.titleText = new AutoTextLabel(0.5, 0.15, this);
            this.titleText.text.setText("Room List");
            this.titleText.background.setOpacity(0);

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
            this.getChildren().addAll(this.titleText, this.descriptionText, this.roomsContainer, hBox);
        }
    }

    // Room creation when you create a room
    public static class RoomCreationGui extends VBox {
        protected AutoTextLabel titleText;
        protected AutoTextLabel roomNameTitleText;
        protected AutoTextField roomNameTextField;
        protected AutoButton createRoomButton;

        public RoomCreationGui() {
            this.setAlignment(Pos.CENTER);

            this.titleText = new AutoTextLabel(1, 0.125, this);
            this.titleText.text.setText("Create Room");
            this.titleText.background.setOpacity(0);

            this.roomNameTitleText = new AutoTextLabel(0.2, 0.075, this);
            this.roomNameTitleText.text.setText("Enter Room Name:");
            this.roomNameTitleText.background.setOpacity(0);

            this.roomNameTextField = new AutoTextField(0.2, 0.0625, this);
            this.roomNameTextField.setText("");

            this.createRoomButton = new AutoButton(0.2, 0.05, this);
            this.createRoomButton.setText("Create");

            this.getChildren().addAll(this.titleText, this.roomNameTitleText, this.roomNameTextField, this.createRoomButton);
        }
    }

    // GUI that shows after a room is created
    public static class RoomSettingsGui extends VBox {
        protected AutoTextLabel titleText;
        protected AutoTextLabel roomName;
        protected AutoTextLabel roomCreator;
        protected AutoTextLabel boardSize;
        protected AutoTextLabel timerPerTurn;
        protected AutoTextLabel connectedPlayers;
        protected VBox settings;
        protected DropdownList boardSizeSettings;
        protected DropdownList timerPerTurnSettings;
        protected AutoTextLabel notification;
        protected AutoTextLabel warning;
        protected HBox hBox;
        protected HBox hBox2;
        protected AutoButton startButton;
        protected AutoButton leaveRoom;

        public RoomSettingsGui() {
            this.setAlignment(Pos.CENTER);

            this.titleText = new AutoTextLabel(0.2, 0.15, this);
            this.titleText.text.setText("Room Settings");
            this.titleText.background.setOpacity(0);

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

            this.connectedPlayers = new AutoTextLabel(1, 0.2, roomDesc);
            this.connectedPlayers.setAlignment(Pos.CENTER_LEFT);
            this.connectedPlayers.background.setOpacity(0);

            roomDesc.getChildren().addAll(this.roomName, this.roomCreator, this.boardSize, this.timerPerTurn, this.connectedPlayers);

            this.settings = new VBox();
            this.settings.setAlignment(Pos.TOP_CENTER);
            bindSizeToParent(this.settings, this.hBox, 0.4, 1);

            this.boardSizeSettings = new DropdownList(1, 0.15, this.settings);
            this.boardSizeSettings.setPromptText("Change Board Size");
            for (int size : Main.BOARD_SIZE_OPTIONS) {
                this.boardSizeSettings.getItems().add(String.format("%dx%d", size, size));
            }

            this.timerPerTurnSettings = new DropdownList(1, 0.15, this.settings);
            this.timerPerTurnSettings.getItems().add("N/A");
            this.timerPerTurnSettings.setPromptText("Change Time per Turn");
            for (double time : Main.TIMER_PER_TURN_OPTIONS) {
                this.timerPerTurnSettings.getItems().add(String.format("%.1fs/turn", time));
            }

            this.settings.getChildren().addAll(
                    new VSpacer(0.2, this.settings),
                    new VSpacer(0.225, this.settings),
                    boardSizeSettings,
                    new VSpacer(0.025, this.settings),
                    timerPerTurnSettings);
            this.hBox.getChildren().addAll(roomDesc, new HSpacer(0.2, this.hBox), settings);

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
                    this.titleText,
                    new HSpacer(0.1, this),
                    this.hBox,
                    new HSpacer(0.1, this),
                    this.notification,
                    new HSpacer(0.025, this),
                    this.hBox2,
                    new HSpacer(0.025, this),
                    this.warning
            );
        }
    }

    // Main game when player is in a match
    public static class GameMatchGui extends StackPane {
        public AutoTextLabel turnTimer;
        public AutoTextLabel upperText;
        public AutoTextLabel bottomText;
        private final VBox vBox;

        public GameMatchGui() {
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER);
            bindSizeToParent(hBox, this, 0.9, 0.35);

            this.turnTimer = new AutoTextLabel(0.25, 1, hBox);
            this.turnTimer.background.setOpacity(0);

            hBox.getChildren().addAll(new HSpacer(0.75, hBox), this.turnTimer);

            vBox = new VBox();
            vBox.setAlignment(Pos.CENTER);
            bindSizeToParent(vBox, this, 1, 1);

            this.upperText = new AutoTextLabel(0.5, 0.0625, this.vBox);
            this.upperText.background.setOpacity(0);

            this.bottomText = new AutoTextLabel(0.5, 0.0625, this.vBox);
            this.bottomText.background.setOpacity(0);

            this.getChildren().addAll(hBox, this.vBox);
        }

        public void addBoardAndInit(BoardUi boardUi) {
            this.vBox.getChildren().addAll(this.upperText, boardUi, this.bottomText);
        }
    }

    /// Helper functions
    // Binds a node to its parent
    private static void bindSizeToParent(Node targetNode, Region parent, double xScale, double yScale) {
        // Switch-case for some nodes listed, not everything is handled yet
        switch (targetNode) {
            case Button button:
                button.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                button.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                button.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                button.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case Label label:
                // Handle Label node
                break;
            case Rectangle rectangle:
                rectangle.widthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                rectangle.heightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case ScrollPane scrollPane:
                scrollPane.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                scrollPane.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                scrollPane.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                scrollPane.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case Ellipse ellipse:
                ellipse.radiusXProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                ellipse.radiusYProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case Circle circle:
                // Handle Circle node
                break;
            case Line line:
                // Handle Line node
                break;
            case Polygon polygon:
                // Handle Polygon node
                break;
            case Polyline polyline:
                // Handle Polyline node
                break;
            case StackPane stackPane:
                stackPane.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                stackPane.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                stackPane.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                stackPane.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case HBox hBox:
                hBox.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                hBox.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                hBox.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                hBox.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case VBox vBox:
                vBox.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                vBox.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                vBox.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                vBox.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case GridPane gridPane:
                gridPane.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                gridPane.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                gridPane.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                gridPane.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case ComboBox<?> comboBox:
                comboBox.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                comboBox.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                comboBox.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                comboBox.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case AnchorPane anchorPane:
                // Handle AnchorPane layout
                break;
            case FlowPane flowPane:
                // Handle FlowPane layout
                break;
            case BorderPane borderPane:
                // Handle BorderPane layout
                break;
            case TilePane tilePane:
                // Handle TilePane layout
                break;
            case ProgressBar progressBar:
                // Handle ProgressBar node
                break;
            case ProgressIndicator progressIndicator:
                // Handle ProgressIndicator node
                break;
            case PasswordField passwordField:
                // Handle PasswordField node
                break;
            case TextField textField:
                textField.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                textField.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                textField.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                textField.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case TextArea textArea:
                // Handle TextArea node
                break;
            case Canvas canvas:
                // Handle Canvas node
                break;
            case MenuBar menuBar:
                // Handle MenuBar node
                break;
            case ToolBar toolBar:
                // Handle ToolBar node
                break;
            case Region region:
                // Handle Region node
                break;
            default:
                // Handle unknown node type
                break;
        }
    }

    // Listens to parent size changes and update the font size numerically
    private static void bindFontSizeToParentSize(Node targetText, Region parent, double fontSize) {
        updateFontSize(targetText, parent, fontSize);

        parent.widthProperty().addListener((observable, oldValue, newValue) -> {
            updateFontSize(targetText, parent, fontSize);
        });

        parent.heightProperty().addListener((observable, oldValue, newValue) -> {
            updateFontSize(targetText, parent, fontSize);
        });
    }

    // Listens to container size changes and update the font size to always fit
    private static void bindFontToAlwaysFit(Node targetText, Region container) {
        updateAutoFitFontSize(targetText, container);

        container.widthProperty().addListener((observable, oldValue, newValue) -> {
            updateAutoFitFontSize(targetText, container);
        });

        container.heightProperty().addListener((observable, oldValue, newValue) -> {
            updateAutoFitFontSize(targetText, container);
        });
    }

    // Updates font size numerically
    private static void updateFontSize(Node targetText, Region parent, double fontSize) {
        double updatedFontSize = getUpdatedFontSize(parent.widthProperty().get(), parent.heightProperty().get(), fontSize);

        Platform.runLater(() -> {
            switch (targetText) {
                case Text text -> text.setFont(Font.font(updatedFontSize));
                case TextField textField -> textField.setFont(Font.font(updatedFontSize));
                case Button textButton -> textButton.setFont(Font.font(updatedFontSize));
                case ComboBox<?> comboBox -> comboBox.setStyle("-fx-font-size: " + updatedFontSize + "px;");
                case null, default -> {
                    assert targetText != null;
                    throw new IllegalArgumentException("Unsupported target text type: " + targetText.getClass().getSimpleName());
                }
            }
        });
    }

    // Updates font size to always fit the parent container
    private static void updateAutoFitFontSize(Node targetText, Region container) {
        double maxWidth = container.widthProperty().get() * 0.65; // 35% margin tolerance
        double maxHeight = container.heightProperty().get() * 0.65;

        Platform.runLater(() -> {
           switch (targetText) {
               case Text text:
                   text.setFont(Font.font(calculateAutoFitFontSize(maxWidth, maxHeight, text.getText())));
                   break;
               case TextField textField:
                   textField.setFont(Font.font(calculateAutoFitFontSize(maxWidth, maxHeight, "")));
                   break;
               case Button button:
                   button.setFont(Font.font(calculateAutoFitFontSize(maxWidth, maxHeight, button.getText())));
                   break;
               case ComboBox<?> comboBox:
                   comboBox.setStyle("-fx-font-size: " + calculateAutoFitFontSize(maxWidth, maxHeight, comboBox.getPromptText()) + "px;");
                   break;
               default:
                   throw new IllegalStateException("Unsupported target text type: " + targetText.getClass().getSimpleName());
           }
        });
    }

    // Gets updated font size depending on current screen size
    private static double getUpdatedFontSize(double width, double height, double DEFAULT_FONT_SIZE) {
        // Ratio works best at 16:9
        return DEFAULT_FONT_SIZE * (16.0 / 25.0 * (width / Main.DEFAULT_WIDTH) + 9.0 / 25.0 * (height / Main.DEFAULT_HEIGHT));
    }

    // Calculates the best font size to auto-fit
    private static double calculateAutoFitFontSize(double maxWidth, double maxHeight, String textContent) {
        double fontSize = 0;
        double step = 0.5;

        Text tempText = new Text();
        tempText.setText(textContent);
        tempText.setFont(Font.font(fontSize));

        while (tempText.getBoundsInLocal().getWidth() <= maxWidth && tempText.getBoundsInLocal().getHeight() <= maxHeight) {
            fontSize += step;
            tempText.setFont(Font.font(fontSize));
        }

        return fontSize - step;
    }
}