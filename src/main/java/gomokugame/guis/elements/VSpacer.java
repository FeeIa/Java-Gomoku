package gomokugame.guis.elements;

import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;

// Vertical spacer (empty) element for spacing purposes
public class VSpacer extends Region {
    public VSpacer(double yScale, Region parent) {
        this.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
        this.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
        this.setMouseTransparent(true);
    }
}