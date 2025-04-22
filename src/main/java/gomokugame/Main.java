package org.example.gomokugame;

import gomokugame.server.Server;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {
    /// VARIABLES & INITIALIZERS
    protected static final int DEFAULT_WIDTH = 960;
    protected static final int DEFAULT_HEIGHT = 540;
    private static final double ASPECT_RATIO = (double) DEFAULT_WIDTH / (double) DEFAULT_HEIGHT;
    private static final int GLOBAL_PORT = 9090; // The port to connect to
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    protected static final int[] BOARD_SIZE_OPTIONS = {9, 11, 13, 15, 19, 20, 30};
    protected static final double[] TIMER_PER_TURN_OPTIONS = {15, 30, 45, 60};

    @Override
    public void start(Stage stage) {
        // Open a gomokugame.server socket
        Server server = new Server(GLOBAL_PORT); // Will not open if not the first instance of Client to join
        threadPool.execute(server);

        // Open a gomokugame.client connection
        Client client = new Client(GLOBAL_PORT, stage);
        client.run();

        // Stage setup
        stage.setTitle("Gomoku Game");
        stage.show();
        stage.setOnCloseRequest(_ -> {
            if (client.clientSocket != null) {
                client.close();
                System.out.println("Client closed.");

                if (!server.success()) {
                    Platform.exit();
                    System.exit(0);
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}