package gomokugame;

import gomokugame.client.Client;
import gomokugame.server.Server;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {
    public static final int[] BOARD_SIZE_OPTIONS = {9, 11, 13, 15, 19, 20, 30};
    public static final double[] TIMER_PER_TURN_OPTIONS = {15, 30, 45, 60};
    public static final int[] INVISIBLE_MODE_REVEAL_CHANCES = {1, 3, 5, 7, 10, 15};
    public static final int DEFAULT_WIDTH = 960;
    public static final int DEFAULT_HEIGHT = 540;
    private static final int GLOBAL_PORT = 9090; // The port to connect to
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    @Override
    public void start(Stage stage) {
        // Opens up a server socket
        Server server = new Server(GLOBAL_PORT); // Will not open if not the first instance of Client to join
        threadPool.execute(server); // Executes the server thread. If failed to open, it'll stop execution by return

        // Opens up a client socket
        Client client = new Client(GLOBAL_PORT, stage);
        client.run();

        // Stage setup
        stage.setTitle("Gomoku Game");
        stage.show();
        stage.setOnCloseRequest(e -> {
            if (client.clientSocket != null) {
                client.close();
                System.out.println("Client closed.");

                // If the current instance was not the first Client to join, then terminate completely, otherwise retain
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