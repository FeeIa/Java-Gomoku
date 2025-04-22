package gomokugame.guis.elements;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import static gomokugame.guis.GUI.*;

// Represents a vertical scrolling container that scales to its parent
public class VerticalScrollingContainer extends ScrollPane {
    private final VBox content;

    public VerticalScrollingContainer(double xScale, double yScale, Region parent) {
        bindSizeToParent(this, parent, xScale, yScale);

        this.setHbarPolicy(ScrollBarPolicy.NEVER);

        this.content = new VBox();
        this.setContent(this.content);
    }

    public void addElement(Node element) {
        this.content.getChildren().add(element);
    }

    public void removeAllElements() {
        this.content.getChildren().clear();
    }
}