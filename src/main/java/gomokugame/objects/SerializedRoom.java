package gomokugame.objects;

import java.io.Serializable;

public class SerializableRoom implements Serializable {
    protected int roomId;
    protected String roomName;
    protected String roomCreatorId;
    protected long timerPerTurnInMilliseconds;
    protected int boardSize;
    protected int connectedPlayersAmount;
    protected boolean isCreateRequest;

    // Client usage
    protected boolean asPlayer;
    protected boolean asSpectator;

    public SerializableRoom(String roomName) {
        this.roomName = roomName;
    }
}