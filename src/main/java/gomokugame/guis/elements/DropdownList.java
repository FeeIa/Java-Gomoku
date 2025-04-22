package gomokugame.guis.elements;

import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;

import static gomokugame.guis.GUI.*;

// Represents a dropdown list that scales to its parent
public class DropdownList extends ComboBox<String> {
    public DropdownList(double xScale, double yScale, Region parent) {
        bindSizeToParent(this, parent, xScale, yScale);
        bindFontToAlwaysFit(this, this);
    }
}