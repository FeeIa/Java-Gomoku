package gomokugame.server;

import gomokugame.objects.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

public class ClientConnection implements Runnable {
    private final Socket clientSocket;
    private Room connectedRoom;
    private ExecutorService threadPool;
    protected ObjectOutputStream oos;
    protected ObjectInputStream ois;
    protected Server hostServer;
    protected String id;

    public ClientConnection(Socket client) {
        this.clientSocket = client;
    }

    @Override
    public void run() {
        try {
            // Variables setup
            this.id = String.format("%010d", this.hostServer.globalId++); // 10 digits id
            this.oos = new ObjectOutputStream(this.clientSocket.getOutputStream());
            this.ois = new ObjectInputStream(this.clientSocket.getInputStream());
            this.threadPool = Executors.newCachedThreadPool();
            System.out.println("Client " + this.id + " connected.");

            // Listens to client instance message in a separate thread
            this.listenToClientMessage();
        } catch (IOException e){
            System.err.println("Failed to initialize Client " + this.id + ".");
            this.close();
        }
    }

    // Closes the ClientConnection
    private void close() {
        if (!this.clientSocket.isClosed()) {
            try {
                this.hostServer.connectedClients.remove(this);
                this.handleLeaveRoomRequest();
                this.clientSocket.close();
                this.oos.close();
                this.ois.close();
                this.threadPool.shutdown();

                System.out.println("Client " + this.id + " disconnected.");
            } catch (IOException e) {
                System.err.println("Failed to close Client " + this.id + ".");
            }
        }
    }

    /// METHODS
    // Listens for client instance request (inputs)
    private void listenToClientMessage() {
        this.threadPool.execute(() -> {
            while (true) {
                // Get the message
                Object message;

                try {
                    message = this.ois.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Failed to read object from Client " + this.id + ".");
                    this.close();
                    break;
                }

                // Process the message
                if (message instanceof String) {
                    System.out.println("Received " + message + " from Client " + this.id + ".");

                    switch ((String) message) {
                        case "GET_BOARD_REQUEST":
                            try {
                                System.out.println(this.connectedRoom.board);
                                this.oos.writeObject(this.connectedRoom.board);
                                this.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send board object to client.");
                            }

                            break;
                        case "GET_COLOR_REQUEST":
                            try {
                                if (this.connectedRoom.white == this) {
                                    this.oos.writeObject("WHITE");
                                }
                                else if (this.connectedRoom.black == this) {
                                    this.oos.writeObject("BLACK");
                                }
                                else {
                                    this.oos.writeObject("SPECTATOR");
                                }

                                this.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send color to client.");
                            }

                            break;
                        case "GET_INVISIBLE_MODE_REVEAL_CHANCES":
                            try {
                                if (this == this.connectedRoom.white) {
                                    this.oos.writeObject(this.connectedRoom.whiteInvisibleModeRevealChances);
                                }
                                else if (this == this.connectedRoom.black) {
                                    this.oos.writeObject(this.connectedRoom.blackInvisibleModeRevealChances);
                                }
                                else {
                                    this.oos.writeObject(-1);
                                }
                                this.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send invisible mode turns to client.");
                            }

                            break;
                        case "UPDATE_INVISIBLE_MODE_REVEAL_CHANCES":
                            try {
                                if (this == this.connectedRoom.white) {
                                    this.connectedRoom.whiteInvisibleModeRevealChances--;
                                    this.oos.writeObject(this.connectedRoom.whiteInvisibleModeRevealChances);
                                }
                                else if (this == this.connectedRoom.black) {
                                    this.connectedRoom.blackInvisibleModeRevealChances--;
                                    this.oos.writeObject(this.connectedRoom.blackInvisibleModeRevealChances);
                                }
                                this.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send invisible mode turns to client after updating.");
                            }

                            break;
                        case "INVALID_MOVE_PENALTY":
                            synchronized (this.connectedRoom.moveLock) {
                                this.connectedRoom.moveLock.notifyAll();
                            }

                            break;
                        case "GET_ROOM_LIST":
                            try {
                                this.oos.writeObject(this.getSerializedRoomList());
                                this.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send room list to client.");
                            }

                            break;
                        case "LEAVE_ROOM_REQUEST":
                            this.handleLeaveRoomRequest();
                            break;
                        case "START_MATCH_REQUEST":
                            // If no match is in progress and connected client is now 2 --> start the match
                            if (this.connectedRoom.roomCreator == this) {
                                if (!this.connectedRoom.matchInProgress && this.connectedRoom.connectedClients.size() >= 2) {
                                    this.updateAllClientRoomList();
                                    this.connectedRoom.startMatch(); // Starts the match in a different thread
                                }
                                else {
                                    try {
                                        this.oos.writeObject("NOT_ENOUGH_PLAYERS_TO_START");
                                        this.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Failed to send NOT_ENOUGH_PLAYERS_TO_START to client.");
                                    }
                                }
                            }
                            else {
                                System.err.println("Client " + this.id + " tried to start a room without being a room creator.");
                            }

                            break;
                        case "REMATCH_REQUEST":
                            if (this.connectedRoom.white == this) {
                                this.connectedRoom.whiteRequestedRematch = true;

                                if (this.connectedRoom.black != null) {
                                    try {
                                        this.connectedRoom.black.oos.writeObject("REQUEST_REMATCH");
                                        this.connectedRoom.black.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Failed to send REQUEST_REMATCH to client.");
                                    }
                                }
                                else {
                                    try {
                                        this.oos.writeObject("REMATCH_IMPOSSIBLE");
                                        this.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Failed to send REMATCH_IMPOSSIBLE to client.");
                                    }
                                }
                            }
                            else if (this.connectedRoom.black == this) {
                                this.connectedRoom.blackRequestedRematch = true;

                                if (this.connectedRoom.white != null) {
                                    try {
                                        this.connectedRoom.white.oos.writeObject("REQUEST_REMATCH");
                                        this.connectedRoom.white.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Failed to send REQUEST_REMATCH to client.");
                                    }
                                }
                                else {
                                    try {
                                        this.oos.writeObject("REMATCH_IMPOSSIBLE");
                                        this.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Failed to send REMATCH_IMPOSSIBLE to client.");
                                    }
                                }
                            }

                            this.connectedRoom.checkForRematchRequest();

                            break;
                        case "FINISHED_INITIALIZING":
                            synchronized (this.connectedRoom.initLock) {
                                this.connectedRoom.initLock.notifyAll();
                            }

                            break;
                        case "EXIT_MATCH":
                            if (this.connectedRoom.white == this) {
                                this.connectedRoom.white = null;

                                if (this.connectedRoom.black != null) {
                                    try {
                                        this.connectedRoom.black.oos.writeObject("REMATCH_IMPOSSIBLE");
                                        this.connectedRoom.black.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Failed to send REMATCH_IMPOSSIBLE to client.");
                                    }
                                }
                            }
                            else if (this.connectedRoom.black == this) {
                                this.connectedRoom.black = null;

                                if (this.connectedRoom.white != null) {
                                    try {
                                        this.connectedRoom.white.oos.writeObject("REMATCH_IMPOSSIBLE");
                                        this.connectedRoom.white.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Failed to send REMATCH_IMPOSSIBLE to client.");
                                    }
                                }
                            }
                    }
                }
                else if (message instanceof Move moveMade) {
                    System.out.println("Received move from Client " + this.id + ".");

                    if (this == this.connectedRoom.currentPlayerInTurn) {
                        Tile[][] boardArray = this.connectedRoom.board.boardArray;
                        int targetRow = moveMade.targetRow;
                        int targetCol = moveMade.targetCol;

                        if (boardArray[targetRow][targetCol].occupant == null) {
                            moveMade.moveMaker = (this.connectedRoom.white == this) ? "WHITE" : "BLACK";
                            boardArray[targetRow][targetCol].occupant = moveMade.moveMaker;
                            this.connectedRoom.movesDone.add(moveMade);

                            try {
                                this.oos.writeObject("VALID_MOVE");
                                this.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to VALID_MOVE to client.");
                            }

                            updateAllClientBoardInRoom(moveMade);

                            synchronized (this.connectedRoom.moveLock) {
                                this.connectedRoom.moveLock.notifyAll();
                            }
                        }
                        else {
                            System.out.println("Move on row " + targetRow + " and column " + targetCol + " is invalid.");

                            try {
                                this.oos.writeObject("INVALID_MOVE");
                                this.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send INVALID_MOVE to client.");
                            }
                        }
                    }
                    else {
                        System.out.println("Illegal move detected. A non-playing player sent a move request.");
                    }
                }
                else if (message instanceof SerializedRoom receivedRoom) {
                    System.out.println("Received serializable room from client " + this.id + ".");

                    if (this.connectedRoom == null) {
                        if (receivedRoom.isCreateRequest) {
                            System.out.println("Creating room on server...");

                            Room room = new Room((int) (Math.random() * 10000), this);
                            room.roomName = receivedRoom.roomName;

                            this.hostServer.availableRooms.add(room);
                            this.connectedRoom = room;
                            threadPool.execute(room);

                            this.updateAllClientRoomList();
                            updateAllClientRoomSettings();
                        }
                        else {
                            System.out.println("Joining room on server...");

                            for (Room r : this.hostServer.availableRooms) {
                                if (r.roomId == receivedRoom.roomId) {
                                    r.addClient(this);
                                    this.connectedRoom = r;

                                    updateAllClientRoomList();
                                    updateAllClientRoomSettings();

                                    // If match is in progress --> immediately send start request as a spectator
                                    if (r.matchInProgress) {
                                        r.startSpectate(this);
                                    }

                                    break;
                                }
                            }
                        }
                    }
                    else {
                        System.out.println("Client " + this.id + " is already in a room. Cannot join/create.");

                        try {
                            this.oos.writeObject("ALREADY_IN_ROOM");
                            this.oos.flush();
                        } catch (IOException e) {
                            System.err.println("Failed to send ALREADY_IN_ROOM to client.");
                        }
                    }
                }
                else if (message instanceof BoardSizeOption boardSizeOption) {
                    if (this.connectedRoom.roomCreator == this) {
                        this.connectedRoom.boardSize = boardSizeOption.boardSize;
                    }

                    this.updateAllClientRoomSettings();
                }
                else if (message instanceof TimerOption timerOption) {
                    if (this.connectedRoom.roomCreator == this) {
                        this.connectedRoom.timerPerTurnInMilliseconds = timerOption.timerPerTurnInMilliseconds;
                    }

                    this.updateAllClientRoomSettings();
                }
                else if (message instanceof InvisibleModeOption invisibleModeOption) {
                    if (this.connectedRoom.roomCreator == this) {
                        this.connectedRoom.invisibleModeRevealChances = invisibleModeOption.invisibleModeRevealChances;
                    }

                    this.updateAllClientRoomSettings();
                }
            }
        });
    }

    // Updates the available room list for all connected clients
    private void updateAllClientRoomList() {
        for (ClientConnection c : this.hostServer.connectedClients) {
            try {
                if (c.oos != null) {
                    c.oos.writeObject(this.getSerializedRoomList());
                    c.oos.flush();
                }
            } catch (IOException e) {
                System.err.println("Failed to send updated room list to Client " + c.id);
            }
        }
    }

    private ArrayList<SerializedRoom> getSerializedRoomList() {
        ArrayList<SerializedRoom> rooms = new ArrayList<>();

        for (Room room : this.hostServer.availableRooms) {
            rooms.add(this.serializeRoom(room));
        }

        return rooms;
    }

    private SerializedRoom serializeRoom(Room room) {
        SerializedRoom serializedRoom = new SerializedRoom(room.roomName);

        serializedRoom.roomId = room.roomId;
        serializedRoom.roomCreatorId = room.roomCreator.id;
        serializedRoom.boardSize = room.boardSize;
        serializedRoom.timerPerTurnInMilliseconds = room.timerPerTurnInMilliseconds;
        serializedRoom.invisibleModeRevealChances = room.invisibleModeRevealChances;
        serializedRoom.connectedPlayersAmount = room.connectedClients.size();

        return serializedRoom;
    }

    // Updates all client board in the current connected room after a move is made
    private void updateAllClientBoardInRoom(Move moveMade) {
        for (ClientConnection c : this.connectedRoom.connectedClients) {
            try {
                System.out.println(moveMade.targetCol + ":" + moveMade.targetRow);
                c.oos.writeObject(moveMade);
                c.oos.flush();
            } catch (IOException e) {
                System.out.println("Failed to send updated move to Client " + c.id + ".");
            }
        }
    }

    // Updates the current room settings screen if its on display
    private void updateAllClientRoomSettings() {
        for (ClientConnection c : this.connectedRoom.connectedClients) {
            try {
                if (c.oos != null) {
                    SerializedRoom serializedRoom = this.serializeRoom(c.connectedRoom);
                    serializedRoom.asPlayer = ((!c.connectedRoom.connectedClients.isEmpty() && c.connectedRoom.connectedClients.getFirst() == c) || (c.connectedRoom.connectedClients.size() >= 2 && c.connectedRoom.connectedClients.get(1) == c));
                    serializedRoom.asSpectator = !serializedRoom.asPlayer;

                    c.oos.writeObject(serializedRoom);
                    c.oos.flush();
                }
            } catch (IOException e) {
                System.err.println("Failed to send room to Client " + c.id);
            }
        }
    }

    // Handles the leave room request
    private void handleLeaveRoomRequest() {
        // Remove from current connected clients in the room
        if (this.connectedRoom != null) {
            boolean wasInMatch = false;
            this.connectedRoom.connectedClients.remove(this);

            if (this.connectedRoom.white == this || this.connectedRoom.black == this) {
                if (this.connectedRoom.white == this) {
                    this.connectedRoom.white = null;
                }
                else {
                    this.connectedRoom.black = null;
                }

                if (this.connectedRoom.matchInProgress) {
                    wasInMatch = true;
                    this.connectedRoom.endMatch();
                }
            }

            updateAllClientRoomSettings();

            // Closes the room if the creator is them
            if (this.connectedRoom.roomCreator == this) {
                this.hostServer.availableRooms.remove(this.connectedRoom);
                updateAllClientRoomList();

                for (ClientConnection c : this.connectedRoom.connectedClients) {
                    c.connectedRoom = null;

                    if (wasInMatch) {
                        try {
                            c.oos.writeObject("HOST_LEFT_IN_MATCH");
                            c.oos.flush();
                        } catch (IOException e) {
                            System.err.println("Failed to send HOST_LEFT_IN_MATCH to client.");
                        }

                        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                        scheduler.schedule(() -> {
                            try {
                                c.oos.writeObject("LEAVE_ROOM");
                                c.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send leave room to client.");
                            }
                        }, 5000, TimeUnit.MILLISECONDS);

                        scheduler.shutdown();
                    }
                    else {
                        try {
                            c.oos.writeObject("LEAVE_ROOM");
                            c.oos.flush();
                        } catch (IOException e) {
                            System.err.println("Failed to send leave room to client.");
                        }
                    }
                }

                this.connectedRoom.connectedClients.clear();
            }
            else {
                updateAllClientRoomList();
            }

            this.connectedRoom = null;
        }
    }
}