package org.example.gomokugame;
import gomokugame.objects.GameObject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Client implements Runnable {
    /// VARIABLES & INITIALIZERS
    protected Socket clientSocket;
    private Stage stage;
    private GameObject.Board board;
    private String color;
    private ArrayList<GameObject.SerializableRoom> availableRooms;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private GUI.GameMatchGui gameMatchGui;
    private Scene mainMenuScene;
    private Scene roomListScene;
    private Scene roomCreationScene;
    private Scene roomSettingsScene;
    private GUI.MainMenuGui mainMenuGui;
    private GUI.RoomListGui roomListGui;
    private GUI.RoomCreationGui roomCreationGui;
    private GUI.RoomSettingsGui roomSettingsGui;
    private GUI.MatchEndScreen matchEndScreen;
    private final Object oisThreadLock = new Object();
    private final Object matchEndThreadLock = new Object();
    private ExecutorService threadPool;
    private GameObject.Move lastMove;
    private boolean turnTimerIsRunning;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public Client(int port, Stage stage) {
        try {
            this.clientSocket = new Socket("localhost", port);
            this.stage = stage;
            System.out.println("Client connected to port " + this.clientSocket.getPort());
        } catch (IOException e){
            // Failed to open, so gomokugame.client must be null
            this.clientSocket = null; // Just to ensure
            System.err.println("Could not connect to gomokugame.server on port " + port + ". Possibly due to no gomokugame.server online or invalid port.");
        }
    }

    @Override
    public void run() {
        // If gomokugame.client failed to open then terminate process
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
        this.mainMenuGui = new GUI.MainMenuGui();
        this.mainMenuScene = new Scene(this.mainMenuGui, Main.DEFAULT_WIDTH, Main.DEFAULT_HEIGHT);
        this.stage.setScene(this.mainMenuScene);

        // Sets up a MouseClicked event upon clicking Start Game to open roomListGui
        this.mainMenuGui.startGameButton.setOnMouseClicked(_ -> {
            // If roomListGui is NULL then create initialize new one
            if (this.roomListGui == null) {
                this.roomListGui = new GUI.RoomListGui();

                // Create Room button click
                this.roomListGui.createRoomButton.setOnMouseClicked(_ -> {
                    // If roomCreationGui is NULL then initialize new one
                    if (this.roomCreationGui == null) {
                        this.roomCreationGui = new GUI.RoomCreationGui();

                        this.roomCreationGui.createRoomButton.setOnMouseClicked(_ -> {
                            String roomName = this.roomCreationGui.roomNameTextField.getText();

                            if (!roomName.isEmpty()) {
                                if (this.roomSettingsGui == null) {
                                    this.roomSettingsGui = new GUI.RoomSettingsGui();
                                }

                                if (!this.roomSettingsGui.hBox2.getChildren().contains(this.roomSettingsGui.startButton)) {
                                    this.roomSettingsGui.hBox2.getChildren().add(this.roomSettingsGui.startButton);
                                }
                                if (!this.roomSettingsGui.hBox.getChildren().contains(this.roomSettingsGui.settings)) {
                                    this.roomSettingsGui.hBox.getChildren().add(this.roomSettingsGui.settings);
                                }

                                this.roomSettingsGui.boardSizeSettings.setOnAction(e -> {
                                    String selectedItem = this.roomSettingsGui.boardSizeSettings.getSelectionModel().getSelectedItem();

                                    if (selectedItem != null) {
                                        System.out.println(selectedItem);

                                        GameObject.BoardSizeOption boardSizeOption = new GameObject.BoardSizeOption(Integer.parseInt(selectedItem.split("x")[0]));
                                        this.sendMessageToServer(boardSizeOption);
                                    }
                                });

                                this.roomSettingsGui.timerPerTurnSettings.setOnAction(e -> {
                                   String selectedItem = this.roomSettingsGui.timerPerTurnSettings.getSelectionModel().getSelectedItem();

                                   if (selectedItem != null) {
                                       System.out.println(selectedItem);
                                       GameObject.TimerOption timerOption;

                                       if (selectedItem.equals("N/A")) {
                                           timerOption = new GameObject.TimerOption(0);
                                       }
                                       else {
                                           timerOption = new GameObject.TimerOption((int) (Double.parseDouble(selectedItem.split("s")[0]) * 1000));
                                       }

                                       this.sendMessageToServer(timerOption);
                                   }
                                });

                                this.roomSettingsGui.leaveRoom.setOnMouseClicked(_ -> {
                                    this.sendLeaveRoomRequest();
                                    this.showRoomList();
                                });

                                this.roomSettingsGui.startButton.setOnMouseClicked(_ -> {
                                    this.sendStartMatchRequest();
                                });

                                this.sendCreateRoomRequest(roomName);
                                this.showRoomSettings();
                            }
                        });
                    }
                    this.showRoomCreation(); // Show the room creation gui
                });

                // Back button click
                this.roomListGui.backButton.setOnMouseClicked(_ -> {
                    this.showMainMenu();
                });
            }

            this.showRoomList(); // Show the room list
            this.getRoomListByIdFromServer(); // Get available rooms from gomokugame.server
        });

        this.listenToServerMessage(); // Listen to gomokugame.server message in a separate thread
    }

    public void close() {
        try {
            if (!this.clientSocket.isClosed()) {
                this.clientSocket.close();
                this.oos.close();;
                this.ois.close();
                this.scheduler.shutdown();
            }
        } catch (IOException e) {
            // Handle
        }
    }

    /// IN-MATCH METHODS
    public void startMatch() {
        this.threadPool.execute(() -> {
            this.gameMatchGui = new GUI.GameMatchGui();
            this.getColorFromServer();
            this.generateBoard();
            this.showGameMatchGui();
            this.sendMessageToServer("FINISHED_INITIALIZING");
        });
    }

    public void generateBoard() {
        this.getBoardFromServer();

        System.out.println("Generating board...");
        GUI.BoardUi boardUi = new GUI.BoardUi(this.board, 0.45, 0.8, this.gameMatchGui);
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

    public void handleMoveRequest() {
        Platform.runLater(() -> {
            this.gameMatchGui.bottomText.text.setText("YOUR TURN");
        });

        // Sets a MouseClick connection for each tile
        for (GameObject.Tile[] tiles : this.board.boardArray) {
            for (GameObject.Tile tile : tiles) {
                tile.ui.tileSquare.setOnMouseClicked(e -> {
                    GameObject.Move move = new GameObject.Move(tile.row, tile.col);
                    this.lastMove = move;
                    this.sendMessageToServer(move);
                });

                tile.ui.tileSquare.setOnMouseEntered(e -> {
                    tile.ui.hoverIndicator.setFill(Color.CYAN);
                });

                tile.ui.tileSquare.setOnMouseExited(e -> {
                    tile.ui.hoverIndicator.setFill(Color.TRANSPARENT);
                });
            }
        }
    }

    public void handleMoveTimeout() {
        Platform.runLater(() -> {
            this.gameMatchGui.bottomText.text.setText("RAN OUT OF TIME TO MAKE A MOVE");
            this.gameMatchGui.turnTimer.text.setText("");

            ScheduledFuture<?> timerTask = this.scheduler.schedule(() -> {
                Platform.runLater(() -> {
                    if (this.gameMatchGui.bottomText.text.getText().equals("RAN OUT OF TIME TO MAKE A MOVE")) {
                        this.gameMatchGui.bottomText.text.setText("");
                    }
                });
            }, 1000, TimeUnit.MILLISECONDS);
        });

        for (GameObject.Tile[] tiles : this.board.boardArray) {
            for (GameObject.Tile tile : tiles) {
                tile.ui.hoverIndicator.setFill(Color.TRANSPARENT);
                tile.ui.tileSquare.setOnMouseClicked(null);
                tile.ui.tileSquare.setOnMouseEntered(null);
                tile.ui.tileSquare.setOnMouseExited(null);
            }
        }

        this.turnTimerIsRunning = false;
    }

    public void endMatch(GameObject.MatchEndResult endResult) {
        Platform.runLater(() -> {
            this.matchEndScreen = new GUI.MatchEndScreen(this.gameMatchGui);

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
                    this.matchEndScreen.subMessage.text.setText("Your opponent aborted the game. Please exit");
                    this.matchEndScreen.hBox.getChildren().clear();
                    this.matchEndScreen.hBox.getChildren().add(this.matchEndScreen.exitMatch);
                }
                else {
                    this.matchEndScreen.subMessage.text.setText("You can choose to request a rematch or exit");
                    this.matchEndScreen.rematchButton.setOnMouseClicked(e -> {
                        this.sendMessageToServer("REMATCH_REQUEST");
                    });
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
                            System.out.println("Received gomokugame.server request: " + message);

                            switch ((String) message) {
                                case "NOT_ENOUGH_PLAYERS_TO_START":
                                    if (this.roomSettingsGui != null) {
                                        Platform.runLater(() -> {
                                            this.roomSettingsGui.warning.text.setText("Not enough player to start the match!");

                                            ScheduledFuture<?> timerTask = this.scheduler.schedule(() -> {
                                                Platform.runLater(() -> {
                                                    if (this.roomSettingsGui.warning.text.getText().equals("Not enough player to start the match!")) {
                                                        this.roomSettingsGui.warning.text.setText("");
                                                    }
                                                });
                                            }, 1000, TimeUnit.MILLISECONDS);
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
                                    this.board.boardArray[this.lastMove.targetRow][this.lastMove.targetCol].ui.indicateInvalidMove();
                                    this.gameMatchGui.bottomText.text.setText("INVALID MOVE");

                                    ScheduledFuture<?> timerTask = this.scheduler.schedule(() -> {
                                        Platform.runLater(() -> {
                                            if (this.gameMatchGui.bottomText.text.getText().equals("INVALID MOVE")) {
                                                this.gameMatchGui.bottomText.text.setText("YOUR TURN");
                                            }
                                        });
                                    }, 1000, TimeUnit.MILLISECONDS);
                                    break;
                                case "VALID_MOVE":
                                    GameObject.Tile tile = this.board.boardArray[this.lastMove.targetRow][this.lastMove.targetCol];
                                    tile.ui.hoverIndicator.setFill(Color.TRANSPARENT);

                                    for (GameObject.Tile[] cTiles : this.board.boardArray) {
                                        for (GameObject.Tile cTile : cTiles) {
                                            cTile.ui.tileSquare.setOnMouseClicked(null);
                                            cTile.ui.tileSquare.setOnMouseEntered(null);
                                            cTile.ui.tileSquare.setOnMouseExited(null);
                                        }
                                    }

                                    Platform.runLater(() -> {
                                        this.gameMatchGui.bottomText.text.setText("");
                                    });

                                    this.turnTimerIsRunning = false;
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
                                                    this.matchEndScreen.notification.text.setText("The room host left the match. You will be automatically redirected in 5 seconds");

                                                    Timeline timeline = new Timeline();
                                                    timeline.getKeyFrames().add(
                                                            new KeyFrame(Duration.seconds(1), event -> {
                                                                timer[0]--;
                                                                this.matchEndScreen.notification.text.setText("The room host left the match. You will be automatically exit in " + timer[0] + " second(s)");

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
                            this.availableRooms = (ArrayList<GameObject.SerializableRoom>) message;
                            this.updateRoomList();
                        }
                        else if (message instanceof GameObject.Move move) {
                            System.out.println("Received move");

                            if (this.board != null) {
                                GameObject.Tile[][] boardArray = this.board.boardArray;
                                boardArray[move.targetRow][move.targetCol].occupant = move.moveMaker;
                                boardArray[move.targetRow][move.targetCol].ui.addOccupant(move.moveMaker);
                            }
                        }
                        else if (message instanceof GameObject.Board passedBoard) {
                            System.out.println("Received board");
                            this.board = passedBoard;
                        }
                        else if (message instanceof GameObject.MatchEndResult endResult) {
                            System.out.println("Received end result");
                            this.endMatch(endResult);
                        }
                        else if (message instanceof GameObject.SerializableRoom serializableRoom) {
                            System.out.println("Received serializable room");
                            this.updateRoomSettings(serializableRoom);
                        }
                        else if (message instanceof GameObject.MoveRequest moveRequest) {
                            this.handleMoveRequest();

                            if (moveRequest.timerPerTurnInMilliseconds > 0) {
                                this.gameMatchGui.turnTimer.text.setText("Make a move in: " + String.format("%.1f", moveRequest.timerPerTurnInMilliseconds / 1000.0) + "s");

                                Timeline timeline = new Timeline();
                                timeline.getKeyFrames().add(
                                        new KeyFrame(Duration.millis(50), event -> {
                                            moveRequest.timerPerTurnInMilliseconds -= 50;
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

                        this.oisThreadLock.notifyAll();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Failed to read object from gomokugame.server on port: " + this.clientSocket.getPort());
                    this.close();
                    break;
                }
            }
        });
    }

    private void getBoardFromServer() {
        System.out.println("Getting board from port " + clientSocket.getPort());
        this.sendRequestToServer("GET_BOARD_REQUEST");
    }

    private void getRoomListByIdFromServer() {
        System.out.println("Getting room list from port " + clientSocket.getPort());
        this.sendRequestToServer("GET_ROOM_LIST");
    }

    private void getColorFromServer() {
        System.out.println("Getting color from port " + clientSocket.getPort());
        this.sendRequestToServer("GET_COLOR_REQUEST");
    }

    private void sendCreateRoomRequest(String roomName) {
        System.out.println("Creating room request");

        GameObject.SerializableRoom roomToCreate = new GameObject.SerializableRoom(roomName);
        roomToCreate.isCreateRequest = true;
        this.sendRequestToServer(roomToCreate);
    }

    private void sendJoinRoomRequest(GameObject.SerializableRoom roomToJoin) {
        System.out.println("Joining room");
        this.sendRequestToServer(roomToJoin);
    }

    private void sendLeaveRoomRequest() {
        System.out.println("Leaving room");
        this.sendMessageToServer("LEAVE_ROOM_REQUEST");
    }

    private void sendStartMatchRequest() {
        System.out.println("Sending match start request");
        this.sendRequestToServer("START_MATCH_REQUEST");
    }

    // Request expects a callback (response) from the gomokugame.server
    private void sendRequestToServer(Object request) {
        synchronized (this.oisThreadLock) {
            try {
                System.out.println("Sending: " + request);
                this.oos.writeObject(request);
                this.oos.flush();
                this.oisThreadLock.wait();
            } catch (IOException e) {
                System.err.println("Failed to send " + request + " to gomokugame.server on port " + clientSocket.getPort());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Message does not expect a response from the gomokugame.server
    private void sendMessageToServer(Object message) {
        try {
            System.out.println("Sending: " + message);
            this.oos.writeObject(message);
            this.oos.flush();
        } catch (IOException e) {
            System.err.println("Failed to send " + message + " to gomokugame.server on port " + clientSocket.getPort());
        }
    }

    /// GUI RELATEd METHODS
    public double getCurrentWidth () {
        return this.stage.sceneProperty().get().getWidth();
    }

    public double getCurrentHeight () {
        return this.stage.sceneProperty().get().getHeight();
    }

    public void showMainMenu() {
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

    public void showRoomList() {
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

    public void showRoomCreation() {
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

    public void showRoomSettings() {
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

    public void showGameMatchGui() {
        double currentWidth = getCurrentWidth();
        double currentHeight = getCurrentHeight();

        Platform.runLater(() -> {
            this.stage.setScene(new Scene(this.gameMatchGui, currentWidth, currentHeight));
        });
    }

    public void updateRoomList() {
        if (this.roomListGui == null) {
            return;
        }

        // Only update if the current scene is the roomListScene
        if (this.stage.sceneProperty().get().getRoot() == this.roomListGui) {
            Platform.runLater(() -> {
                GUI.VerticalScrollingContainer container = this.roomListGui.roomsContainer;
                container.removeAllElements();

                if (availableRooms.isEmpty()) {
                    this.roomListGui.descriptionText.text.setText("No rooms available. Go create one!");
                }
                else {
                    this.roomListGui.descriptionText.text.setText(this.availableRooms.size() + " room(s) available. Go join or create one!");

                    for (GameObject.SerializableRoom room : this.availableRooms) {
                        GUI.ListedRoomUi roomUi = new GUI.ListedRoomUi(0.5, 0.175, this.roomListGui);
                        roomUi.roomName.text.setText("Room Name: " + room.roomName);
                        roomUi.roomCreator.text.setText("Room Creator Id: " + room.roomCreatorId);
                        roomUi.roomId.text.setText("Room Id: " + Integer.toString(room.roomId));
                        roomUi.playersAmount.text.setText("Connected players: " + room.connectedPlayersAmount + " player(s)");

                        if (room.connectedPlayersAmount < 2) {
                            roomUi.matchStatus.text.setText("Join as Player");
                        }
                        else {
                            roomUi.matchStatus.text.setText("Join as Spectator");
                        }

                        roomUi.joinButton.setOnMouseClicked(_ -> {
                            if (this.roomSettingsGui == null) {
                                this.roomSettingsGui = new GUI.RoomSettingsGui();
                            }

                            this.roomSettingsGui.hBox2.getChildren().remove(this.roomSettingsGui.startButton);
                            this.roomSettingsGui.hBox.getChildren().remove(this.roomSettingsGui.settings);

                            if (room.asPlayer) {
                                this.roomSettingsGui.notification.text.setText("You are playing. Waiting for host to start...");
                            }
                            else if (room.asSpectator) {
                                this.roomSettingsGui.notification.text.setText("You are spectating. Waiting for host to start...");
                            }

                            this.roomSettingsGui.leaveRoom.setOnMouseClicked(_ -> {
                                this.sendLeaveRoomRequest();
                                this.showRoomList();
                            });

                            this.sendJoinRoomRequest(room);
                            this.showRoomSettings();
                        });

                        container.addElement(roomUi);
                    }
                }
            });
        }
    }

    public void updateRoomSettings(GameObject.SerializableRoom room) {
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
        });
    }
}