# Gomoku Game - CSC1004 Project 2

There are three types of features implemented:
- **Basic**: Features required by the course project details.
- **Advanced**: Features that are implemented for extra score.
- **Additional**: Features that are implemented for self-interest.

## Features
### Basic Features
- **Default Board Size**: 20x20 board.
- **Turn Based**: Two players in a match (black plays first) take turns to place a stone on an empty intersection. Players will not be able to place a stone in a non-empty intersection (an indicator will pop up as a warning).
- **Winning Condition**: First player to get FIVE unbroken rows of stones wins.
- **Board Display**: The board is visually displayed.
- **Current Turn**: The program tells which player is in turn.
- **Game End**: The program tells who wins and who loses after the ongoing match ends.
- **Game Settings**: Users (room host) can change the game settings.
- **Game Reset**: You can request for a rematch to restart the game.
- **Direct Mouse Control**: GUI elements are interactable by mouse controls.

### Advanced Features
- **Invisible Mode**: You can choose to play invisible mode, where all placed pieces on the board are not shown by default. Use *reveal chances* to reveal hidden pieces for a turn. There is a penalty of a turn skip after 3 invalid moves are made.
- **Time Limit:** You can choose to play with a time limit per turn (15s, 30s, 45s, 60s).
- **User-Friendly Interface**: Made some animations & additional GUI related stuff.

### Additional Features
- **Multiplayer**: Multiple instances can play together in a match.
- **Room-based Match**: Players can create and join different rooms with customizable settings to start a multiplayer match.
- **Scaled UI**: UI elements are scaled to the current window size. The expected aspect ratio is the default screen size upon running the program. UI elements may not look good in different aspect ratios.

## How To Run The Program
You can skip the compilation step if you are using an IDE. Directly run the Main file. You do not need to separately open the Client and Server. The program will automatically open the server upon first run.
### Running the Main File:
1. **Compile the Code**: You need to download MAVEN if you want to compile this manually. It is recommended to just use an IDE. Run the following command inside the GomokuGame folder path:
```
mvn clean compile
```

2. **Run the Main File**:
```
mvn javafx:run
```

## Tutorials
### Creating a room
You can create a room by clicking the "Create Room" button. You will be prompted to enter a name for the room. Afterwards, you can customize the match settings in that room:
- **Board Size**: defaults to 20x20
- **Time Limit**: defaults to N/A
- **Invisible Mode**: defaults to OFF

### Joining a room
You can directly join any available rooms in the room list. If you are the second player to join that room, you will automatically play against the room creator; otherwise, as a spectator.

### Gameplay
1. Place a stone when it is your turn. The game will tell you when it is your turn.
2. If there is a time limit, then there will be a countdown to tell you. If it reaches zero, your turn will be automatically skipped.
3. If invisible mode is on, then you cannot see any pieces on the board unless you use your reveal chance for one turn. Mistakenly placing a stone on an unavailable tile three times leads to your turn being skipped.
4. The first player to reach 5 consecutive stones placed on the board wins.
5. You can request to rematch or exit the room.
