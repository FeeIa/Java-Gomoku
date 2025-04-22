package gomokugame.guis.elements;

import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import static gomokugame.guis.GUI.*;

// Represents a button UI that automatically scales according to their parent
public class AutoButton extends Button {
    public AutoButton(double xScale, double yScale, double fontSize, Region parent) {
        bindSizeToParent(this, parent, xScale, yScale);
        bindFontSizeToParentSize(this, parent, fontSize);
    }

    public AutoButton(double xScale, double yScale, Region parent) {
        bindSizeToParent(this, parent, xScale, yScale);
        bindFontToAlwaysFit(this, this);
    }
}