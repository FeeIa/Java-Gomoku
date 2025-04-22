package gomokugame.guis.elements;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

import static gomokugame.guis.GUI.*;

// Represents a fill-able text field that scales to its parent
public class AutoTextField extends TextField {
    public AutoTextField(double xScale, double yScale, double fontSize, Region parent) {
        bindSizeToParent(this, parent, xScale, yScale);

        this.setEditable(true);
        this.setAlignment(Pos.CENTER);

        bindFontSizeToParentSize(this, parent, fontSize);
    }

    public AutoTextField(double xScale, double yScale, Region parent) {
        bindSizeToParent(this, parent, xScale, yScale);

        this.setEditable(true);
        this.setAlignment(Pos.CENTER);

        bindFontToAlwaysFit(this, this);
    }
}