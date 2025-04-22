package gomokugame.objects;

import java.io.Serializable;

public class SerializedRoom implements Serializable {
    public int roomId;
    public String roomName;
    public String roomCreatorId;
    public long timerPerTurnInMilliseconds;
    public int invisibleModeRevealChances;
    public int boardSize;
    public int connectedPlayersAmount;
    public boolean isCreateRequest;

    // Client usage
    public boolean asPlayer;
    public boolean asSpectator;

    public SerializedRoom(String roomName) {
        this.roomName = roomName;
    }
}