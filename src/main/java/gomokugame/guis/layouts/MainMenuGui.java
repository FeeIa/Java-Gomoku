package gomokugame.guis.layouts;

import gomokugame.guis.elements.*;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

public class MainMenuGui extends VBox {
    public AutoButton startGameButton;
    public AutoButton exitGameButton;

    public MainMenuGui() {
        this.setAlignment(Pos.CENTER);

        AutoTextLabel titleScreenText = new AutoTextLabel(0.75, 0.25, this);
        titleScreenText.text.setText("a go game");
        titleScreenText.background.setOpacity(0);

        this.startGameButton = new AutoButton(0.4, 0.1, this);
        this.startGameButton.setText("Start Game");

        this.exitGameButton = new AutoButton(0.4, 0.1, this);
        this.exitGameButton.setText("Exit");

        this.getChildren().addAll(titleScreenText, this.startGameButton, this.exitGameButton);
    }
}