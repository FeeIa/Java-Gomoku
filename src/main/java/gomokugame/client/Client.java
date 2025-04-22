package gomokugame.client;

import gomokugame.guis.elements.*;
import gomokugame.guis.layouts.*;
import gomokugame.objects.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import gomokugame.Main;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Client implements Runnable {
    public Socket clientSocket;
    private Stage stage;
    private Board board;
    private String color;
    private ArrayList<SerializedRoom> availableRooms;
    private Move lastMove;
    private boolean turnTimerIsRunning;
    private boolean invisibleModeIsOn = false;
    private int invisibleModeRevealChances;
    private boolean revealModeIsOn;
    private int invalidMovesCount;
    private Scene mainMenuScene;
    private Scene roomListScene;
    private Scene roomCreationScene;
    private Scene roomSettingsScene;
    private MainMenuGui mainMenuGui;
    private RoomListGui roomListGui;
    private RoomCreationGui roomCreationGui;
    private RoomSettingsGui roomSettingsGui;
    private GameMatchGui gameMatchGui;
    private MatchEndScreen matchEndScreen;
    private final Object oisThreadLock = new Object();
    private final Object matchEndThreadLock = new Object();
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private ExecutorService threadPool;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public Client(int port, Stage stage) {
        try {
            this.clientSocket = new Socket("localhost", port);
            this.stage = stage;
            System.out.println("Client connected to port " + this.clientSocket.getPort());
        } catch (IOException e){
            // Failed to open, so client must be null
            this.clientSocket = null; // Just to ensure
            System.err.println("Could not connect to Server on port " + port + ". Possibly due to no server online or invalid port.");
        }
    }

    @Override
    public void run() {
        // If client fails to open then terminate process
        if (this.clientSocket == null) {
            return;
        }

        this.threadPool = Executors.newCachedThreadPool();
        this.availableRooms = new ArrayList<>();

        // IO streams
        try {
            this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
            this.oos.flush();
            this.ois = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.err.println("Failed to establish Client IO stream.");
        }

        // Initializing GUIs
        this.mainMenuGui = new MainMenuGui();
        this.mainMenuScene = new Scene(this.mainMenuGui, Main.DEFAULT_WIDTH, Main.DEFAULT_HEIGHT);
        this.stage.setScene(this.mainMenuScene);

        // Sets up a MouseClicked event upon clicking Start Game to open roomListGui
        this.mainMenuGui.startGameButton.setOnMouseClicked(e1 -> {
            // If roomListGui is NULL then create and initialize new one
            if (this.roomListGui == null) {
                this.roomListGui = new RoomListGui();

                // Show room creation gui upon clicking create room
                this.roomListGui.createRoomButton.setOnMouseClicked(e2 -> {
                    // If roomCreationGui is NULL then initialize new one
                    if (this.roomCreationGui == null) {
                        this.roomCreationGui = new RoomCreationGui();

                        // Process room creation
                        this.roomCreationGui.createRoomButton.setOnMouseClicked(e3 -> {
                            String roomName = this.roomCreationGui.roomNameTextField.getText();

                            if (!roomName.isEmpty()) {
                                if (this.roomSettingsGui == null) {
                                    this.initializeRoomSettingsGui();
                                }

                                if (!this.roomSettingsGui.hBox2.getChildren().contains(this.roomSettingsGui.startButton)) {
                                    this.roomSettingsGui.hBox2.getChildren().add(this.roomSettingsGui.startButton);
                                }
                                if (!this.roomSettingsGui.hBox.getChildren().contains(this.roomSettingsGui.settings)) {
                                    this.roomSettingsGui.hBox.getChildren().add(this.roomSettingsGui.settings);
                                }

                                this.sendCreateRoomRequest(roomName);
                                this.roomSettingsGui.setDefaultSettingsValue();
                                this.showRoomSettings();
                            }
                        });
                    }

                    this.roomCreationGui.roomNameTextField.setText("");
                    this.showRoomCreation(); // Show the room creation gui
                });

                // Back button click
                this.roomListGui.backButton.setOnMouseClicked(e -> this.showMainMenu());
            }

            this.showRoomList(); // Show the room list
            this.getRoomListFromServer(); // Get available rooms from server
        });

        this.mainMenuGui.exitGameButton.setOnMouseClicked(e -> this.stage.fireEvent(new WindowEvent(this.stage, WindowEvent.WINDOW_CLOSE_REQUEST)));

        this.listenToServerMessage(); // Listen to server message in a separate thread
    }

    public void close() {
        try {
            if (!this.clientSocket.isClosed()) {
                this.clientSocket.close();
                this.oos.close();
                this.ois.close();
                this.threadPool.shutdown();
                this.scheduler.shutdown();
            }
        } catch (IOException e) {
            // Handle
        }
    }

    /// IN-MATCH METHODS
    private void startMatch() {
        this.threadPool.execute(() -> {
            this.gameMatchGui = new GameMatchGui();
            this.getColorFromServer();
            this.generateBoard();
            this.showGameMatchGui();
            this.getInvisibleModeRevealChancesFromServer();
            this.sendMessageToServer("FINISHED_INITIALIZING");
        });
    }

    private void generateBoard() {
        this.getBoardFromServer();

        System.out.println("Generating board...");
        BoardUi boardUi = new BoardUi(this.board, 0.45, 0.8, this.gameMatchGui);
        System.out.println("SUCCESS");

        Platform.runLater(() -> {
            this.gameMatchGui.addBoardAndInit(boardUi);

            if (color.equals("SPECTATOR")) {
                this.gameMatchGui.upperText.text.setText("YOU ARE SPECTATING");
            }
            else {
                this.gameMatchGui.upperText.text.setText("YOU ARE PLAYING AS " + color.toUpperCase());
            }
        });
    }

    private void handleMoveRequest() {
        Platform.runLater(() -> this.gameMatchGui.bottomText.text.setText("YOUR TURN"));

        // Sets a MouseClick connection for each tile
        for (Tile[] tiles : this.board.boardArray) {
            for (Tile tile : tiles) {
                tile.ui.tileSquare.setOnMouseClicked(e -> {
                    Move move = new Move(tile.row, tile.col);
                    this.lastMove = move;
                    this.sendMessageToServer(move);
                });

                tile.ui.tileSquare.setOnMouseEntered(e -> tile.ui.hoverIndicator.setFill(Color.CYAN));

                tile.ui.tileSquare.setOnMouseExited(e -> tile.ui.hoverIndicator.setFill(Color.TRANSPARENT));
            }
        }

        if (this.invisibleModeIsOn) this.invalidMovesCount = 0;
    }

    // Just a helper function for three other functions
    private void afterMove() {
        for (Tile[] cTiles : this.board.boardArray) {
            for (Tile cTile : cTiles) {
                cTile.ui.tileSquare.setOnMouseClicked(null);
                cTile.ui.tileSquare.setOnMouseEntered(null);
                cTile.ui.tileSquare.setOnMouseExited(null);
                cTile.ui.hoverIndicator.setFill(Color.TRANSPARENT);
            }
        }

        this.turnTimerIsRunning = false;
        this.gameMatchGui.hideRevealStonesButton();
        if (this.invisibleModeIsOn && this.revealModeIsOn) {
            this.hideAllStones();
            this.revealModeIsOn = false;
        }
    }

    private void handleValidMove() {
        Tile tile = this.board.boardArray[this.lastMove.targetRow][this.lastMove.targetCol];
        tile.ui.hoverIndicator.setFill(Color.TRANSPARENT);

        Platform.runLater(() -> this.gameMatchGui.bottomText.text.setText(""));

        this.afterMove();
    }

    private void handleInvalidMove() {
        this.board.boardArray[this.lastMove.targetRow][this.lastMove.targetCol].ui.indicateInvalidMove();

        if (this.invisibleModeIsOn && !this.revealModeIsOn) {
            this.invalidMovesCount++;

            if (this.invalidMovesCount == 3) {
                this.handleInvalidMovesPenalty();
                return;
            }
        }

        String toDisplay;
        if (this.invisibleModeIsOn && !this.revealModeIsOn) {
            toDisplay = "INVALID MOVE! YOU ONLY CAN MAKE " + (3 - this.invalidMovesCount) + " INVALID MOVE(S) LEFT!";
        }
        else {
            toDisplay = "INVALID MOVE!";
        }
        this.gameMatchGui.bottomText.text.setText(toDisplay);

        this.scheduler.schedule(() -> Platform.runLater(() -> {
            if (this.gameMatchGui.bottomText.text.getText().equals(toDisplay)) {
                this.gameMatchGui.bottomText.text.setText("YOUR TURN");
            }
        }), 1000, TimeUnit.MILLISECONDS);
    }

    private void handleMoveTimeout() {
        Platform.runLater(() -> {
            this.gameMatchGui.bottomText.text.setText("RAN OUT OF TIME TO MAKE A MOVE");

            this.scheduler.schedule(() -> Platform.runLater(() -> {
                if (this.gameMatchGui.bottomText.text.getText().equals("RAN OUT OF TIME TO MAKE A MOVE")) {
                    this.gameMatchGui.bottomText.text.setText("");
                }
            }), 1000, TimeUnit.MILLISECONDS);
        });

        this.afterMove();
    }

    private void handleInvalidMovesPenalty() {
        Platform.runLater(() -> {
            this.gameMatchGui.bottomText.text.setText("YOU HAVE MADE 3 INVALID MOVES! SKIPPING YOUR TURN...");

            this.scheduler.schedule(() -> Platform.runLater(() -> {
                if (this.gameMatchGui.bottomText.text.getText().equals("YOU HAVE MADE 3 INVALID MOVES! SKIPPING YOUR TURN...")) {
                    this.gameMatchGui.bottomText.text.setText("");
                }
            }), 1000, TimeUnit.MILLISECONDS);
        });

        this.afterMove();
        this.sendMessageToServer("INVALID_MOVE_PENALTY");
    }

    private void endMatch(MatchEndResult endResult) {
        Platform.runLater(() -> {
            this.showHiddenStones();
            this.matchEndScreen = new MatchEndScreen(this.gameMatchGui);

            if (endResult.spectator) {
                this.matchEndScreen.hBox.getChildren().clear();
                this.matchEndScreen.hBox.getChildren().add(this.matchEndScreen.exitMatch);
                this.matchEndScreen.mainMessage.text.setText("MATCH ENDED");

                if (endResult.wasAnAbort) {
                    this.matchEndScreen.subMessage.text.setText((endResult.colorThatWon.equals("WHITE") ? "White" : "Black") + " won by aborted match");
                }
                else {
                    this.matchEndScreen.subMessage.text.setText((endResult.colorThatWon.equals("WHITE") ? "White" : "Black") + " won the match");
                }
            }
            else if (endResult.winner || endResult.loser){
                if (endResult.winner) {
                    this.matchEndScreen.mainMessage.text.setText("DAMN UR GOOD");
                }
                else {
                    this.matchEndScreen.mainMessage.text.setText("DAMN U SUCK SO BAD");
                }

                if (endResult.wasAnAbort) {
                    this.matchEndScreen.subMessage.text.setText("Your opponent aborted the match. Please exit");
                    this.matchEndScreen.hBox.getChildren().clear();
                    this.matchEndScreen.hBox.getChildren().add(this.matchEndScreen.exitMatch);
                }
                else {
                    this.matchEndScreen.subMessage.text.setText("You can choose to request a rematch or exit");
                    this.matchEndScreen.rematchButton.setOnMouseClicked(e -> this.sendMessageToServer("REMATCH_REQUEST"));
                }
            }
            else {
                this.matchEndScreen.hBox.getChildren().clear();
                this.matchEndScreen.hBox.getChildren().add(this.matchEndScreen.exitMatch);
                this.matchEndScreen.mainMessage.text.setText("NOBODY WON");
                this.matchEndScreen.subMessage.text.setText("Somehow nobody had won the match???");
            }

            this.matchEndScreen.exitMatch.setOnMouseClicked(e -> {
                if (this.roomSettingsGui != null) {
                    this.showRoomSettings();
                    this.sendMessageToServer("EXIT_MATCH");
                }
                else {
                    this.sendLeaveRoomRequest();
                    this.showRoomList();
                }
            });

            this.turnTimerIsRunning = false;
            this.invisibleModeIsOn = false;
            this.gameMatchGui.bottomText.text.setText("");
            this.gameMatchGui.getChildren().add(this.matchEndScreen);

            synchronized (this.matchEndThreadLock) {
                this.matchEndThreadLock.notifyAll();
            }
        });
    }

    /// COMMUNICATION METHODS
    private void listenToServerMessage() {
        this.threadPool.execute(() -> {
            while (!this.clientSocket.isClosed()) {
                Object message;

                try {
                    message = this.ois.readObject();

                    // Synchronized with oisThreadLock to ensure wanted response after sending a request
                    synchronized (this.oisThreadLock) {
                        if (message instanceof String) {
                            System.out.println("Received server request: " + message);

                            switch ((String) message) {
                                case "NOT_ENOUGH_PLAYERS_TO_START":
                                    if (this.roomSettingsGui != null) {
                                        Platform.runLater(() -> {
                                            this.roomSettingsGui.warning.text.setText("Not enough player to start the match!");

                                            this.scheduler.schedule(() -> Platform.runLater(() -> {
                                                if (this.roomSettingsGui.warning.text.getText().equals("Not enough player to start the match!")) {
                                                    this.roomSettingsGui.warning.text.setText("");
                                                }
                                            }), 1000, TimeUnit.MILLISECONDS);
                                        });
                                    }
                                    break;
                                case "START_REQUEST":
                                    this.startMatch();
                                    break;
                                case "WHITE":
                                    this.color = "WHITE";
                                    break;
                                case "BLACK":
                                    this.color = "BLACK";
                                    break;
                                case "SPECTATOR":
                                    this.color = "SPECTATOR";
                                case "JOIN_SUCCESS":
                                    System.out.println("Successfully joined the room.");
                                    break;
                                case "LEAVE_ROOM":
                                    this.showRoomList();
                                    break;
                                case "INVALID_MOVE":
                                    this.handleInvalidMove();
                                    break;
                                case "VALID_MOVE":
                                    this.handleValidMove();
                                    break;
                                case "MOVE_TIMEOUT":
                                    this.handleMoveTimeout();
                                    break;
                                case "REQUEST_REMATCH":
                                    this.matchEndScreen.notification.text.setText("Your opponent requested a rematch");
                                    break;
                                case "REMATCH_IMPOSSIBLE":
                                    this.matchEndScreen.notification.text.setText("Your opponent left the match. Cannot request a rematch");
                                    break;
                                case "HOST_LEFT_IN_MATCH":
                                    threadPool.execute(() -> {
                                        synchronized (this.matchEndThreadLock) {
                                            try {
                                                this.matchEndThreadLock.wait();
                                                final int[] timer = {5};

                                                Platform.runLater(() -> {
                                                    this.matchEndScreen.hBox.getChildren().clear();
                                                    this.matchEndScreen.notification.text.setText("The room host left the match. You will automatically exit in 5 seconds");

                                                    Timeline timeline = new Timeline();
                                                    timeline.getKeyFrames().add(
                                                            new KeyFrame(Duration.seconds(1), event -> {
                                                                timer[0]--;
                                                                this.matchEndScreen.notification.text.setText("The room host left the match. You will automatically exit in " + timer[0] + " second(s)");

                                                                if (timer[0] <= 0) {
                                                                    this.matchEndScreen.notification.text.setText("");
                                                                    timeline.stop();
                                                                }
                                                            })
                                                    );

                                                    timeline.setCycleCount(Timeline.INDEFINITE);
                                                    timeline.play();
                                                });
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    });
                            }
                        }
                        else if (message instanceof ArrayList<?> arrayList) {
                            System.out.println("Received room list.");
                            this.availableRooms = (ArrayList<SerializedRoom>) arrayList;
                            this.updateRoomList();
                        }
                        else if (message instanceof Move move) {
                            System.out.println("Received move");

                            if (this.board != null) {
                                Tile[][] boardArray = this.board.boardArray;
                                boardArray[move.targetRow][move.targetCol].occupant = move.moveMaker;

                                if (!this.invisibleModeIsOn) {
                                    boardArray[move.targetRow][move.targetCol].ui.showOccupant(move.moveMaker);
                                }
                            }
                        }
                        else if (message instanceof Board passedBoard) {
                            System.out.println("Received board");
                            this.board = passedBoard;
                        }
                        else if (message instanceof MatchEndResult endResult) {
                            System.out.println("Received end result");
                            this.endMatch(endResult);
                        }
                        else if (message instanceof SerializedRoom serializableRoom) {
                            System.out.println("Received serialized room");
                            this.updateRoomSettings(serializableRoom);
                        }
                        else if (message instanceof MoveRequest moveRequest) {
                            this.handleMoveRequest();

                            // Show "reveal move" in case of an invisible mode being on
                            if (this.invisibleModeIsOn && this.invisibleModeRevealChances > 0) {
                                this.gameMatchGui.revealAmount.text.setText("You have " + this.invisibleModeRevealChances + " reveal(s) left");
                                this.gameMatchGui.showRevealStonesButton();
                                this.gameMatchGui.revealStones.setOnMouseClicked(e -> {
                                    this.showHiddenStones();
                                    this.gameMatchGui.revealStones.setOnMouseClicked(null);
                                    this.gameMatchGui.hideRevealStonesButton();
                                    this.revealModeIsOn = true;
                                    this.updateInvisibleModeRevealChancesToServer();
                                });
                            }

                            if (moveRequest.timerPerTurnInMilliseconds > 0) {
                                this.gameMatchGui.turnTimer.text.setText("Make a move in: " + String.format("%.1f", moveRequest.timerPerTurnInMilliseconds / 1000.0) + "s");

                                Timeline timeline = new Timeline();
                                timeline.getKeyFrames().add(
                                        new KeyFrame(Duration.millis(100), event -> {
                                            moveRequest.timerPerTurnInMilliseconds -= 100;
                                            this.gameMatchGui.turnTimer.text.setText("Make a move in: " + String.format("%.1f", moveRequest.timerPerTurnInMilliseconds / 1000.0) + "s");

                                            if (moveRequest.timerPerTurnInMilliseconds <= 0 || !this.turnTimerIsRunning) {
                                                this.gameMatchGui.turnTimer.text.setText("");
                                                timeline.stop();
                                            }
                                        })
                                );

                                timeline.setCycleCount(Timeline.INDEFINITE);
                                timeline.play();

                                this.turnTimerIsRunning = true;
                            }
                        }
                        else if (message instanceof Integer turns) {
                            this.invisibleModeRevealChances = turns;
                            if (!this.invisibleModeIsOn) {
                                if (this.invisibleModeRevealChances > 0) {
                                    this.invisibleModeIsOn = true;
                                }
                            }
                        }

                        this.oisThreadLock.notifyAll();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Failed to read object from server.");
                    this.close();
                    break;
                }
            }
        });
    }

    private void sendLeaveRoomRequest() {
        System.out.println("Leaving room");
        this.sendMessageToServer("LEAVE_ROOM_REQUEST");
    }

    private void sendStartMatchRequest() {
        System.out.println("Sending match start request");
        this.sendRequestToServer("START_MATCH_REQUEST");
    }

    private void sendCreateRoomRequest(String roomName) {
        System.out.println("Creating room request");

        SerializedRoom roomToCreate = new SerializedRoom(roomName);
        roomToCreate.isCreateRequest = true;
        this.sendRequestToServer(roomToCreate);
    }

    private void sendJoinRoomRequest(SerializedRoom roomToJoin) {
        System.out.println("Joining room");
        this.sendRequestToServer(roomToJoin);
    }

    // Request expects a callback (response) from the server
    private void sendRequestToServer(Object request) {
        synchronized (this.oisThreadLock) {
            try {
                System.out.println("Sending: " + request);
                this.oos.writeObject(request);
                this.oos.flush();
                this.oisThreadLock.wait();
            } catch (IOException e) {
                System.err.println("Failed to send " + request + " to Server on port " + clientSocket.getPort());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Message does not expect a response from the server
    private void sendMessageToServer(Object message) {
        try {
            System.out.println("Sending: " + message);
            this.oos.writeObject(message);
            this.oos.flush();
        } catch (IOException e) {
            System.err.println("Failed to send " + message + " to Server on port " + clientSocket.getPort());
        }
    }

    private void getBoardFromServer() {
        System.out.println("Getting board from port " + clientSocket.getPort());
        this.sendRequestToServer("GET_BOARD_REQUEST");
    }

    private void getRoomListFromServer() {
        System.out.println("Getting room list from port " + clientSocket.getPort());
        this.sendRequestToServer("GET_ROOM_LIST");
    }

    private void getColorFromServer() {
        System.out.println("Getting color from port " + clientSocket.getPort());
        this.sendRequestToServer("GET_COLOR_REQUEST");
    }

    private void getInvisibleModeRevealChancesFromServer() {
        System.out.println("Getting invisible mode " + clientSocket.getPort());
        this.sendRequestToServer("GET_INVISIBLE_MODE_REVEAL_CHANCES");
    }

    private void updateInvisibleModeRevealChancesToServer() {
        System.out.println("Updating invisible mode " + clientSocket.getPort());
        this.sendRequestToServer("UPDATE_INVISIBLE_MODE_REVEAL_CHANCES");
    }

    /// GUI RELATED METHODS
    private double getCurrentWidth () {
        return this.stage.sceneProperty().get().getWidth();
    }

    private double getCurrentHeight () {
        return this.stage.sceneProperty().get().getHeight();
    }

    private void showMainMenu() {
        double currentWidth = getCurrentWidth();
        double currentHeight = getCurrentHeight();

        Platform.runLater(() -> {
            if (this.mainMenuScene != null) {
                this.mainMenuScene.setRoot(new Pane());
            }

            this.mainMenuScene = new Scene(this.mainMenuGui, currentWidth, currentHeight);
            this.stage.setScene(this.mainMenuScene);
        });
    }

    private void showRoomList() {
        double currentWidth = getCurrentWidth();
        double currentHeight = getCurrentHeight();

        Platform.runLater(() -> {
            if (this.roomListScene != null) {
                this.roomListScene.setRoot(new Pane());
            }

            this.roomListScene = new Scene(this.roomListGui, currentWidth, currentHeight);
            this.stage.setScene(this.roomListScene);
            this.updateRoomList();
        });
    }

    private void showRoomCreation() {
        double currentWidth = getCurrentWidth();
        double currentHeight = getCurrentHeight();

        Platform.runLater(() -> {
            if (this.roomCreationScene != null) {
                this.roomCreationScene.setRoot(new Pane());
            }

            this.roomCreationScene = new Scene(this.roomCreationGui, currentWidth, currentHeight);
            this.stage.setScene(this.roomCreationScene);
        });
    }

    private void showRoomSettings() {
        double currentWidth = getCurrentWidth();
        double currentHeight = getCurrentHeight();

        Platform.runLater(() -> {
            if (this.roomSettingsScene != null) {
                this.roomSettingsScene.setRoot(new Pane());
            }

            this.roomSettingsScene = new Scene(this.roomSettingsGui, currentWidth, currentHeight);
            this.stage.setScene(this.roomSettingsScene);
        });
    }

    private void showGameMatchGui() {
        double currentWidth = getCurrentWidth();
        double currentHeight = getCurrentHeight();

        Platform.runLater(() -> this.stage.setScene(new Scene(this.gameMatchGui, currentWidth, currentHeight)));
    }

    private void updateRoomList() {
        if (this.roomListGui == null) {
            return;
        }

        // Only update if the current scene is the roomListScene
        if (this.stage.sceneProperty().get().getRoot() == this.roomListGui) {
            Platform.runLater(() -> {
                VerticalScrollingContainer container = this.roomListGui.roomsContainer;
                container.removeAllElements();

                if (availableRooms.isEmpty()) {
                    this.roomListGui.descriptionText.text.setText("No rooms available. Go create one!");
                }
                else {
                    this.roomListGui.descriptionText.text.setText(this.availableRooms.size() + " room(s) available. Go join or create one!");

                    for (SerializedRoom room : this.availableRooms) {
                        ListedRoomUi roomUi = new ListedRoomUi(0.5, 0.175, this.roomListGui);
                        roomUi.roomName.text.setText("Room Name: " + room.roomName);
                        roomUi.roomCreator.text.setText("Room Creator Id: " + room.roomCreatorId);
                        roomUi.roomId.text.setText("Room Id: " + room.roomId);
                        roomUi.playersAmount.text.setText("Connected players: " + room.connectedPlayersAmount + " player(s)");

                        if (room.connectedPlayersAmount < 2) {
                            roomUi.matchStatus.text.setText("Join as Player");
                        }
                        else {
                            roomUi.matchStatus.text.setText("Join as Spectator");
                        }

                        roomUi.joinButton.setOnMouseClicked(e -> {
                            if (this.roomSettingsGui == null) {
                                this.initializeRoomSettingsGui();
                            }

                            this.roomSettingsGui.hBox2.getChildren().remove(this.roomSettingsGui.startButton);
                            this.roomSettingsGui.hBox.getChildren().remove(this.roomSettingsGui.settings);

                            if (room.asPlayer) {
                                this.roomSettingsGui.notification.text.setText("You are playing. Waiting for host to start...");
                            }
                            else if (room.asSpectator) {
                                this.roomSettingsGui.notification.text.setText("You are spectating. Waiting for host to start...");
                            }

                            this.sendJoinRoomRequest(room);
                            this.showRoomSettings();
                        });

                        container.addElement(roomUi);
                    }
                }
            });
        }
    }

    private void initializeRoomSettingsGui() {
        this.roomSettingsGui = new RoomSettingsGui();

        this.roomSettingsGui.leaveRoom.setOnMouseClicked(e -> {
            this.sendLeaveRoomRequest();
            this.showRoomList();
        });

        this.roomSettingsGui.startButton.setOnMouseClicked(e -> this.sendStartMatchRequest());

        this.roomSettingsGui.boardSizeSettings.setOnAction(e -> {
            String selectedItem = this.roomSettingsGui.boardSizeSettings.getSelectionModel().getSelectedItem();

            if (selectedItem != null) {
                System.out.println(selectedItem);

                BoardSizeOption boardSizeOption = new BoardSizeOption(Integer.parseInt(selectedItem.split("x")[0]));
                this.sendMessageToServer(boardSizeOption);
            }
        });

        this.roomSettingsGui.timerPerTurnSettings.setOnAction(e -> {
            String selectedItem = this.roomSettingsGui.timerPerTurnSettings.getSelectionModel().getSelectedItem();

            if (selectedItem != null) {
                System.out.println(selectedItem);
                TimerOption timerOption;

                if (selectedItem.equals("N/A")) {
                    timerOption = new TimerOption(0);
                } else {
                    timerOption = new TimerOption((int) (Double.parseDouble(selectedItem.split("s")[0]) * 1000));
                }

                this.sendMessageToServer(timerOption);
            }
        });

        this.roomSettingsGui.invisibleModeRevealChancesSettings.setOnAction(e -> {
            String selectedItem = this.roomSettingsGui.invisibleModeRevealChancesSettings.getSelectionModel().getSelectedItem();

            if (selectedItem != null) {
                System.out.println(selectedItem);
                InvisibleModeOption invisibleModeOption;

                if (selectedItem.equals("N/A")) {
                    invisibleModeOption = new InvisibleModeOption(-1);
                } else {
                    invisibleModeOption = new InvisibleModeOption(Integer.parseInt(selectedItem.split(" ")[0]));
                }

                this.sendMessageToServer(invisibleModeOption);
            }
        });
    }

    private void updateRoomSettings(SerializedRoom room) {
        if (this.roomSettingsGui == null) {
            return;
        }

        Platform.runLater(() -> {
            this.roomSettingsGui.roomName.text.setText("Room Name: " + room.roomName);
            this.roomSettingsGui.roomCreator.text.setText("Room Creator Id: " + room.roomCreatorId);
            this.roomSettingsGui.boardSize.text.setText("Board Size: " + room.boardSize + "x" + room.boardSize);
            this.roomSettingsGui.connectedPlayers.text.setText("Connected players: " + room.connectedPlayersAmount + " player(s)");

            if (room.asPlayer) {
                this.roomSettingsGui.notification.text.setText("You are playing. Wait for host to start!");
            }
            else if (room.asSpectator) {
                this.roomSettingsGui.notification.text.setText("You are spectating. Waiting for host to start.");
            }

            if (room.timerPerTurnInMilliseconds > 0) {
                this.roomSettingsGui.timerPerTurn.text.setText("Timer: " + String.format("%.1f", room.timerPerTurnInMilliseconds / 1000.0) + "s/turn");
            }
            else {
                this.roomSettingsGui.timerPerTurn.text.setText("Timer: N/A");
            }

            if (room.invisibleModeRevealChances > 0) {
                this.roomSettingsGui.invisibleModeRevealChances.text.setText("Invisible Mode: On (" + room.invisibleModeRevealChances + " reveal chance(s))");
            }
            else {
                this.roomSettingsGui.invisibleModeRevealChances.text.setText("Invisible Mode: Off");
            }
        });
    }

    private void showHiddenStones() {
        for (Tile[] tiles : this.board.boardArray) {
            for (Tile tile : tiles) {
                if (tile.occupant != null) {
                    tile.ui.showOccupant(tile.occupant);
                }
            }
        }
    }

    private void hideAllStones() {
        for (Tile[] tiles : this.board.boardArray) {
            for (Tile tile : tiles) {
                if (tile.occupant != null) {
                    tile.ui.hideOccupant();
                }
            }
        }
    }
}