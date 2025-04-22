package org.example.gomokugame;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Server implements Runnable {
    /// VARIABLES & INITIALIZERS
    private ServerSocket server;
    private ArrayList<ClientConnection> connectedClients;
    private ExecutorService threadPool;
    private ArrayList<Room> availableRooms;
    private int globalId = 0; // Used to uniquely identify each player joining in

    // Opens up the gomokugame.server using constructor
    public Server(int port) {
        try {
            this.server = new ServerSocket(port);
        } catch (IOException e) {
            // Failed to open, so this.gomokugame.server is NULL
            this.server = null; // Just to make sure
            System.err.println("Server could not start on port " + port + ". Possibly due to port already in use or invalid port.");
        }
    }

    @Override
    public void run() {
        // If gomokugame.server failed to open then just terminate the process
        if (this.server == null) {
            return;
        }

        // Setup variables
        this.availableRooms = new ArrayList<>();
        this.connectedClients = new ArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
        System.out.println("Server started on port " + this.server.getLocalPort() + ". Waiting for connections...");

        // Listen for gomokugame.client connection on a separate thread
        this.listenForClientConnections();
    }

    public boolean success() {
        return this.server != null;
    }

    /// METHODS
    // Listens for gomokugame.client connection to the gomokugame.server
    private void listenForClientConnections() {
        this.threadPool.execute(() -> {
            while (!this.server.isClosed()) {
                try {
                    Socket client = this.server.accept();
                    ClientConnection clientConnection = new ClientConnection(client);
                    this.connectedClients.add(clientConnection);
                    this.threadPool.execute(clientConnection);
                    System.out.println("Accepted connection from " + client.getInetAddress().getHostAddress() + ":" + client.getPort());
                } catch (IOException e) {
                    System.err.println("Server on port " + this.server.getLocalPort() + " failed to accept a gomokugame.client.");
                }
            }
        });
    }

    // Updates the available room list for all connected clients
    private void updateAllClientRoomList() {
        for (ClientConnection c : this.connectedClients) {
            try {
                if (c.oos != null) {
                    c.oos.writeObject(this.getSerializableRoomList());
                    c.oos.flush();
                }
            } catch (IOException e) {
                System.err.println("Server on port " + this.server.getLocalPort() + " failed to send room list to gomokugame.client " + c.id);
            }
        }
    }

    // Helper functions for updateAllClientRoomList, gets the SerializableRoom object list to be sent to gomokugame.client
    private ArrayList<GameObject.SerializableRoom> getSerializableRoomList() {
        ArrayList<GameObject.SerializableRoom> serializableRooms = new ArrayList<>();

        for (Room room : availableRooms) {
            serializableRooms.add(this.serializeRoom(room));
        }

        return serializableRooms;
    }

    private GameObject.SerializableRoom serializeRoom(Room room) {
        GameObject.SerializableRoom serializedRoom = new GameObject.SerializableRoom(room.roomName);

        serializedRoom.roomId = room.roomId;
        serializedRoom.roomCreatorId = room.roomCreator.id;
        serializedRoom.boardSize = room.boardSize;
        serializedRoom.timerPerTurnInMilliseconds = room.timerPerTurnInMilliseconds;
        serializedRoom.connectedPlayersAmount = room.connectedClients.size();

        return serializedRoom;
    }

    /// INNER CLASSES
    // Represents the interactable connected gomokugame.client
    public class ClientConnection implements Runnable {
        private final Socket clientSocket;
        protected ObjectOutputStream oos;
        protected ObjectInputStream ois;
        protected Room connectedRoom;
        protected String id;
        protected boolean isCurrentlyInMatch;

        public ClientConnection(Socket client) {
            this.clientSocket = client;
        }

        @Override
        public void run() {
            try {
                // Variables setup
                this.id = String.format("%010d", globalId++); // 10 digits id
                this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
                this.ois = new ObjectInputStream(clientSocket.getInputStream());
                System.out.println("Client " + this.id + " connected.");

                // Listens to gomokugame.client message in a separate thread
                this.listenToClientMessage();
            } catch (IOException e){
                System.err.println("Failed to initialize gomokugame.client " + this.id + ".");
                this.close();
            }
        }

        // Closes the gomokugame.client connection
        private void close() {
            if (!this.clientSocket.isClosed()) {
                try {
                    connectedClients.remove(this);
                    this.handleLeaveRoomRequest();
                    this.clientSocket.close();
                    this.oos.close();
                    this.ois.close();

                    System.out.println("Client " + this.id + " disconnected.");
                } catch (IOException e) {
                    System.err.println("Failed to close gomokugame.client " + this.id + " on port " + server.getLocalPort());
                }
            }
        }

        /// METHODS
        // Listens for gomokugame.client request (inputs)
        private void listenToClientMessage() {
            threadPool.execute(() -> {
                while (true) {
                    // Get the message
                    Object message;

                    try {
                        message = this.ois.readObject();
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("Failed to read object from gomokugame.client " + this.id + " on gomokugame.server " + server.getLocalPort());
                        this.close();
                        break;
                    }

                    // Process the message
                    if (message instanceof String) {
                        System.out.println("Received " + message + " from gomokugame.client " + this.id + " on gomokugame.server " + server.getLocalPort());

                        switch ((String) message) {
                            case "GET_BOARD_REQUEST":
                                try {
                                    System.out.println("SENDING BOARD>..");
                                    System.out.println(this.connectedRoom.board);
                                    this.oos.writeObject(this.connectedRoom.board);
                                    this.oos.flush();
                                    System.out.println("SENT BOARD>..");
                                } catch (IOException e) {
                                    System.err.println("Failed to send board object to gomokugame.client.");
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
                                    System.err.println("Failed to send color to gomokugame.client.");
                                }

                                break;
                            case "GET_ROOM_LIST":
                                try {
                                    this.oos.writeObject(getSerializableRoomList());
                                    this.oos.flush();
                                } catch (IOException e) {
                                    System.err.println("Failed to send room list to gomokugame.client.");
                                }

                                break;
                            case "LEAVE_ROOM_REQUEST":
                                this.handleLeaveRoomRequest();
                                break;
                            case "START_MATCH_REQUEST":
                                // If no match is in progress and connected gomokugame.client is now 2 --> start the match
                                if (this.connectedRoom.roomCreator == this) {
                                    if (!this.connectedRoom.matchInProgress && this.connectedRoom.connectedClients.size() >= 2) {
                                        updateAllClientRoomList();
                                        this.connectedRoom.startMatch(); // Starts the match in a different thread
                                    }
                                    else {
                                        try {
                                            this.oos.writeObject("NOT_ENOUGH_PLAYERS_TO_START");
                                            this.oos.flush();
                                        } catch (IOException _) {
                                            System.err.println("Failed to send NOT_ENOUGH_PLAYERS_TO_START to gomokugame.client.");
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
                                            System.err.println("Failed to send REQUEST_REMATCH to gomokugame.client.");
                                        }
                                    }
                                    else {
                                        try {
                                            this.oos.writeObject("REMATCH_IMPOSSIBLE");
                                            this.oos.flush();
                                        } catch (IOException e) {
                                            System.err.println("Failed to send REMATCH_IMPOSSIBLE to gomokugame.client.");
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
                                            System.err.println("Failed to send REQUEST_REMATCH to gomokugame.client.");
                                        }
                                    }
                                    else {
                                        try {
                                            this.oos.writeObject("REMATCH_IMPOSSIBLE");
                                            this.oos.flush();
                                        } catch (IOException e) {
                                            System.err.println("Failed to send REMATCH_IMPOSSIBLE to gomokugame.client.");
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
                                            System.err.println("Failed to send REMATCH_IMPOSSIBLE to gomokugame.client.");
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
                                            System.err.println("Failed to send REMATCH_IMPOSSIBLE to gomokugame.client.");
                                        }
                                    }
                                }
                        }
                    }
                    else if (message instanceof GameObject.Move moveMade) {
                        System.out.println("Received move from gomokugame.client " + this.id + " on gomokugame.server " + server.getLocalPort());

                        if (this == this.connectedRoom.currentPlayerInTurn) {
                            GameObject.Tile[][] boardArray = this.connectedRoom.board.boardArray;
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
                                    System.err.println("Failed to VALID_MOVE to gomokugame.client.");
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
                                    System.err.println("Failed to send INVALID_MOVE to gomokugame.client.");
                                }
                            }
                        }
                        else {
                            System.out.println("Illegal move detected. A non-playing player sent a move request.");
                        }
                    }
                    else if (message instanceof GameObject.SerializableRoom receivedRoom) {
                        System.out.println("Received serializable room from gomokugame.client " + this.id + " on gomokugame.server " + server.getLocalPort());

                        if (this.connectedRoom == null) {
                            if (receivedRoom.isCreateRequest) {
                                System.out.println("Creating room on gomokugame.server...");

                                Room room = new Room((int) (Math.random() * 10000), this);
                                room.roomName = receivedRoom.roomName;

                                availableRooms.add(room);
                                this.connectedRoom = room;
                                threadPool.execute(room);

                                updateAllClientRoomList();
                                updateAllClientRoomSettings();
                            }
                            else {
                                System.out.println("Joining room on gomokugame.server...");

                                for (Room r : availableRooms) {
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
                                System.err.println("Failed to send ALREADY_IN_ROOM to gomokugame.client.");
                            }
                        }
                    }
                    else if (message instanceof GameObject.BoardSizeOption boardSizeOption) {
                        if (this.connectedRoom.roomCreator == this) {
                            this.connectedRoom.boardSize = boardSizeOption.boardSize;
                        }

                        updateAllClientRoomSettings();
                    }
                    else if (message instanceof GameObject.TimerOption timerOption) {
                        if (this.connectedRoom.roomCreator == this) {
                            this.connectedRoom.timerPerTurnInMilliseconds = timerOption.timerPerTurnInMilliseconds;
                        }

                        updateAllClientRoomSettings();
                    }
                }
            });
        }

        // Updates all gomokugame.client board in the current connected room after a move is made
        private void updateAllClientBoardInRoom(GameObject.Move moveMade) {
            for (ClientConnection c : this.connectedRoom.connectedClients) {
                try {
                    System.out.println(moveMade.targetCol + ":" + moveMade.targetRow);
                    c.oos.writeObject(moveMade);
                    c.oos.flush();
                } catch (IOException e) {
                    System.out.println("Failed to send updated move to gomokugame.client " + c.id + " on port " + server.getLocalPort());
                }
            }
        }

        // Updates the current room settings screen if its on display
        private void updateAllClientRoomSettings() {
            for (ClientConnection c : this.connectedRoom.connectedClients) {
                try {
                    if (c.oos != null) {
                        GameObject.SerializableRoom serializedRoom = serializeRoom(c.connectedRoom);
                        serializedRoom.asPlayer = ((!c.connectedRoom.connectedClients.isEmpty() && c.connectedRoom.connectedClients.getFirst() == c) || (c.connectedRoom.connectedClients.size() >= 2 && c.connectedRoom.connectedClients.get(1) == c));
                        serializedRoom.asSpectator = !serializedRoom.asPlayer;

                        c.oos.writeObject(serializedRoom);
                        c.oos.flush();
                    }
                } catch (IOException e) {
                    System.err.println("Server on port " + server.getLocalPort() + " failed to send room to gomokugame.client " + c.id);
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
                    availableRooms.remove(this.connectedRoom);
                    updateAllClientRoomList();

                    for (ClientConnection c : this.connectedRoom.connectedClients) {
                        c.connectedRoom = null;

                        if (wasInMatch) {
                            try {
                                c.oos.writeObject("HOST_LEFT_IN_MATCH");
                                c.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send HOST_LEFT_IN_MATCH to gomokugame.client.");
                            }

                            final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

                            ScheduledFuture<?> timerTask = scheduler.schedule(() -> {
                                try {
                                    c.oos.writeObject("LEAVE_ROOM");
                                    c.oos.flush();
                                } catch (IOException e) {
                                    System.err.println("Failed to send leave room to gomokugame.client.");
                                }
                            }, 5000, TimeUnit.MILLISECONDS);

                            scheduler.shutdown();
                        }
                        else {
                            try {
                                c.oos.writeObject("LEAVE_ROOM");
                                c.oos.flush();
                            } catch (IOException e) {
                                System.err.println("Failed to send leave room to gomokugame.client.");
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
}