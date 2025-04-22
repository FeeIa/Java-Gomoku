package gomokugame.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

public class Server implements Runnable {
    private ServerSocket server;
    private ExecutorService threadPool;
    protected ArrayList<ClientConnection> connectedClients;
    protected ArrayList<Room> availableRooms;
    protected int globalId = 0; // Used to uniquely identify each player joining in

    // Opens up the Server using constructor
    public Server(int port) {
        try {
            this.server = new ServerSocket(port);
        } catch (IOException e) {
            // Failed to open, so make it null
            this.server = null; // Just to make sure
            System.err.println("Server could not start on port " + port + ". Possibly due to port already in use or invalid port.");
        }
    }

    @Override
    public void run() {
        // If server fails to open then just terminate the process
        if (this.server == null) {
            return;
        }

        // Setup variables
        this.availableRooms = new ArrayList<>();
        this.connectedClients = new ArrayList<>();
        this.threadPool = Executors.newCachedThreadPool();
        System.out.println("Server started on port " + this.server.getLocalPort() + ". Waiting for connections...");

        // Listen for ClientConnection on a separate thread
        this.listenForClientConnections();
    }

    public boolean success() {
        return this.server != null;
    }

    // Listens for ClientConnection
    private void listenForClientConnections() {
        this.threadPool.execute(() -> {
            while (!this.server.isClosed()) {
                try {
                    Socket client = this.server.accept();
                    ClientConnection clientConnection = new ClientConnection(client);
                    this.connectedClients.add(clientConnection);
                    this.threadPool.execute(clientConnection);
                    clientConnection.hostServer = this;
                    System.out.println("Accepted connection from " + client.getInetAddress().getHostAddress() + ":" + client.getPort());
                } catch (IOException e) {
                    System.err.println("Server on port " + this.server.getLocalPort() + " failed to accept a client.");
                }
            }
        });
    }
}