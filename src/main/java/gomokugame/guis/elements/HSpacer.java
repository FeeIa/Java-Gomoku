package gomokugame.guis.elements;

import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;

// Horizontal spacer (empty) element for spacing purposes
public class HSpacer extends Region {
    public HSpacer(double xScale, Region parent) {
        this.minWidthProperty().bind(Bindings.multiply(parent.minWidthProperty(), xScale));
        this.maxWidthProperty().bind(Bindings.multiply(parent.maxWidthProperty(), xScale));
        this.setMouseTransparent(true);
    }
}