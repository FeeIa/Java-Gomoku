package gomokugame.guis.elements;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import static gomokugame.guis.GUI.*;

public class AutoTextLabel extends StackPane {
    public Rectangle background;
    public Text text;

    public AutoTextLabel(double xScale, double yScale, double fontSize, Region parent) {
        this.background = new Rectangle();
        bindSizeToParent(this.background, parent, xScale, yScale);

        this.text = new Text();
        bindFontSizeToParentSize(this.text, parent, fontSize);

        this.getChildren().addAll(this.background, this.text);
    }

    public AutoTextLabel(double xScale, double yScale, Region parent) {
        this.background = new Rectangle();
        bindSizeToParent(this.background, parent, xScale, yScale);

        this.text = new Text();
        bindFontToAlwaysFit(this.text, this);

        this.getChildren().addAll(this.background, this.text);
    }
}