package gomokugame.server;

import gomokugame.objects.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Room implements Runnable {
    private final ExecutorService threadPool;
    private ClientConnection winner;
    protected ClientConnection black;
    protected ClientConnection white;
    protected ClientConnection currentPlayerInTurn;
    protected int timerPerTurnInMilliseconds;
    protected final int roomId;
    protected String roomName;
    protected ClientConnection roomCreator;
    protected int boardSize = 20; // Defaults to 20
    protected Board board;
    protected int invisibleModeRevealChances;
    protected int blackInvisibleModeRevealChances;
    protected int whiteInvisibleModeRevealChances;
    protected ArrayList<Move> movesDone;
    protected ArrayList<ClientConnection> connectedClients; // Connected clients (player + spectator)
    protected boolean matchInProgress = false;
    protected boolean blackRequestedRematch = false;
    protected boolean whiteRequestedRematch = false;
    protected final Object initLock = new Object();
    protected final Object moveLock = new Object();
    private static final int[][] checkingDirections = { // Lists the absolute direction for straight-five checking
            {0, 1}, // Horizontal
            {1, 0}, // Vertical
            {-1, 1}, // Diagonal up
            {1, 1} // Diagonal down
    };

    public Room(int roomId, ClientConnection creator) {
        this.roomId = roomId;
        this.roomCreator = creator;

        this.threadPool = Executors.newCachedThreadPool();
        this.connectedClients = new ArrayList<>();
        this.movesDone = new ArrayList<>();
        this.addClient(this.roomCreator);
    }

    @Override
    public void run() {
        System.out.println("Starting room " + this.roomId);
    }

    /// METHODS
    protected void addClient(ClientConnection client) {
        if (!this.connectedClients.contains(client)) {
            System.out.println("ADDING: " + client.id);

            this.connectedClients.add(client);
        }
    }

    protected void startSpectate(ClientConnection client) {
        this.threadPool.execute(() -> this.sendClientStartRequest(client));
    }

    protected void checkForRematchRequest() {
        if (this.whiteRequestedRematch && this.blackRequestedRematch) {
            this.startMatch();
        }
    }

    // Starts the match
    protected void startMatch() {
        System.out.println("Starting match in room " + this.roomId);
        this.matchInProgress = true;
        this.blackInvisibleModeRevealChances = this.invisibleModeRevealChances;
        this.whiteInvisibleModeRevealChances = this.invisibleModeRevealChances;

        this.threadPool.execute(() -> {
            this.initializeBoard();

            this.black = (Math.random() > 0.5) ? connectedClients.get(0) : connectedClients.get(1);
            this.white = (this.black == connectedClients.get(0)) ? connectedClients.get(1) : connectedClients.get(0);
            this.currentPlayerInTurn = this.black; // Black plays first

            this.sendClientStartRequest(this.black);
            this.sendClientStartRequest(this.white);
            for (ClientConnection c : this.connectedClients) {
                if (c != this.white && c != this.black) {
                    this.sendClientStartRequest(c);
                }
            }

            final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            while (this.matchInProgress) {
                boolean matchEnded = this.checkForWinningCondition();

                if (matchEnded) {
                    break;
                }

                synchronized (this.moveLock) {
                    try {
                        this.currentPlayerInTurn.oos.writeObject(new MoveRequest(this.timerPerTurnInMilliseconds));
                        this.currentPlayerInTurn.oos.flush();

                        // Schedules a move skip if ran out of time
                        ScheduledFuture<?> timerTask = null;

                        if (this.timerPerTurnInMilliseconds > 0) {
                            timerTask = scheduler.schedule(() -> {
                                synchronized (this.moveLock) {
                                    System.err.println(String.format("%.1f", this.timerPerTurnInMilliseconds / 1000.0) + " seconds HAS PASSED!!!!!");

                                    try {
                                        this.currentPlayerInTurn.oos.writeObject("MOVE_TIMEOUT");
                                        this.currentPlayerInTurn.oos.flush();
                                    } catch (IOException e) {
                                        System.err.println("Error sending MOVE_TIMEOUT to client");
                                    }

                                    this.moveLock.notifyAll();
                                }
                            }, this.timerPerTurnInMilliseconds, TimeUnit.MILLISECONDS);
                        }

                        this.moveLock.wait();

                        // Cancels the scheduled timerTask if a move was successfully received
                        if (this.timerPerTurnInMilliseconds > 0 && timerTask != null) {
                            timerTask.cancel(false);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to send MoveRequest to client " + this.currentPlayerInTurn.id);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    this.currentPlayerInTurn = (this.currentPlayerInTurn == this.white) ? this.black : this.white;
                }
            }

            scheduler.shutdown();
        });
    }

    // Ends the match
    protected void endMatch() {
        System.out.println("ENDING MATCH...");

        // Reset everything
        this.matchInProgress = false;
        this.whiteRequestedRematch = false;
        this.blackRequestedRematch = false;

        synchronized (this.moveLock) {
            this.moveLock.notifyAll();
        }

        boolean wasAnAbort;
        if (this.white != null && this.black != null) {
            System.out.println((this.winner == this.white ? "WHITE" : "BLACK") + " WON!!!");
            wasAnAbort = false;
        }
        else {
            wasAnAbort = true;
            this.winner = (this.white == null) ? this.black : this.white; // Grants winner to the player that remains
            System.out.println("Match aborted because a player left. " + (this.winner == this.white ? "White" : "Black" + " won by default."));
        }

        for (ClientConnection client : this.connectedClients) {
            this.sendClientEndRequest(client, wasAnAbort);
        }
    }

    // Initializes the Board object
    private void initializeBoard() {
        this.board = new Board(this.boardSize);
        int rows = this.board.boardArray.length;
        int cols = this.board.boardArray[0].length;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.board.boardArray[row][col] = new Tile(row, col);
            }
        }
    }

    // Sends a message to specified client for initialization
    private void sendClientStartRequest(ClientConnection client) {
        try {
            client.oos.writeObject("START_REQUEST");
            client.oos.flush();

            synchronized (this.initLock) {
                this.initLock.wait();
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to send START_REQUEST to Client " + client.id);
        }
    }

    // Sends a message to specified client to tell that the match ended
    private void sendClientEndRequest(ClientConnection client, boolean wasAnAbort) {
        try {
            MatchEndResult result = new MatchEndResult();

            if (this.winner != null) {
                result.wasAnAbort = wasAnAbort;

                if (client == this.winner) {
                    result.winner = true;
                }
                else if (client == this.white || client == this.black) {
                    result.loser = true;
                }
                else {
                    result.spectator = true;
                }

                result.colorThatWon = (this.winner == this.white) ? "WHITE" : "BLACK";
            }

            client.oos.writeObject(result);
            client.oos.flush();
        } catch (IOException e) {
            System.err.println("Failed to send end request to Client " + client.id);
        }
    }

    // Checks for winning condition (straight-five)
    private boolean checkForWinningCondition() {
        if (!this.movesDone.isEmpty()) {
            System.out.println("Checking for winning conditions...");
            Move lastMoveMade = this.movesDone.getLast();

            for (int[] dir : checkingDirections) {
                int consecutiveCount = this.checkConsecutivePieces(lastMoveMade, dir);
                System.out.println("Found " + consecutiveCount + " consecutive pieces.");

                if (consecutiveCount >= 5) {
                    this.winner = lastMoveMade.moveMaker.equalsIgnoreCase("WHITE") ? white : black;
                    this.endMatch();

                    return true;
                }
            }
        }

        return false;
    }

    // Helper method of checkForWinningCondition() to check for consecutive pieces
    private int checkConsecutivePieces(Move lastMove, int[] dir) {
        int count = 1; // Including the starting cell
        Tile[][] boardArray = this.board.boardArray;

        // Positive direction
        int currentRow = lastMove.targetRow + dir[0];
        int currentCol = lastMove.targetCol + dir[1];
        while (withinBound(currentRow, currentCol) && boardArray[currentRow][currentCol].occupant != null && boardArray[currentRow][currentCol].occupant.equals(lastMove.moveMaker)) {
            count++;
            currentRow += dir[0];
            currentCol += dir[1];
        }

        // Negative direction
        currentRow = lastMove.targetRow - dir[0];
        currentCol = lastMove.targetCol - dir[1];
        while (withinBound(currentRow, currentCol) && boardArray[currentRow][currentCol].occupant != null && boardArray[currentRow][currentCol].occupant.equals(lastMove.moveMaker)) {
            count++;
            currentRow -= dir[0];
            currentCol -= dir[1];
        }

        return count;
    }

    private boolean withinBound(int row, int col) {
        return row >= 0 && col >= 0 && row < this.board.boardArray.length && col < this.board.boardArray[0].length;
    }
}