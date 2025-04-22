package gomokugame.guis;

import gomokugame.Main;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

// Every ui element is made to scale to their parent node.
// They either inherit an existing element or new one
// Texts can accept fontSize, if no fontSize is explicitly specified then it auto-fits to its parent container
// This file is to store all static methods needed for the ui elements

public class GUI {
    // Binds a node to its parent
    public static void bindSizeToParent(Node targetNode, Region parent, double xScale, double yScale) {
        // Switch-case for some nodes listed, not everything is handled yet
        switch (targetNode) {
            case Button button:
                button.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                button.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                button.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                button.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case Label label:
                // Handle Label node
                break;
            case Rectangle rectangle:
                rectangle.widthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                rectangle.heightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case ScrollPane scrollPane:
                scrollPane.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                scrollPane.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                scrollPane.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                scrollPane.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case Ellipse ellipse:
                ellipse.radiusXProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                ellipse.radiusYProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case Circle circle:
                // Handle Circle node
                break;
            case Line line:
                // Handle Line node
                break;
            case Polygon polygon:
                // Handle Polygon node
                break;
            case Polyline polyline:
                // Handle Polyline node
                break;
            case StackPane stackPane:
                stackPane.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                stackPane.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                stackPane.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                stackPane.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case HBox hBox:
                hBox.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                hBox.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                hBox.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                hBox.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case VBox vBox:
                vBox.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                vBox.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                vBox.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                vBox.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case GridPane gridPane:
                gridPane.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                gridPane.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                gridPane.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                gridPane.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case ComboBox<?> comboBox:
                comboBox.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                comboBox.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                comboBox.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                comboBox.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case AnchorPane anchorPane:
                // Handle AnchorPane layout
                break;
            case FlowPane flowPane:
                // Handle FlowPane layout
                break;
            case BorderPane borderPane:
                // Handle BorderPane layout
                break;
            case TilePane tilePane:
                // Handle TilePane layout
                break;
            case ProgressBar progressBar:
                // Handle ProgressBar node
                break;
            case ProgressIndicator progressIndicator:
                // Handle ProgressIndicator node
                break;
            case PasswordField passwordField:
                // Handle PasswordField node
                break;
            case TextField textField:
                textField.minWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                textField.minHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                textField.maxWidthProperty().bind(Bindings.multiply(parent.widthProperty(), xScale));
                textField.maxHeightProperty().bind(Bindings.multiply(parent.heightProperty(), yScale));
                break;
            case TextArea textArea:
                // Handle TextArea node
                break;
            case Canvas canvas:
                // Handle Canvas node
                break;
            case MenuBar menuBar:
                // Handle MenuBar node
                break;
            case ToolBar toolBar:
                // Handle ToolBar node
                break;
            case Region region:
                // Handle Region node
                break;
            default:
                // Handle unknown node type
                break;
        }
    }

    // Listens to parent size changes and update the font size numerically
    public static void bindFontSizeToParentSize(Node targetText, Region parent, double fontSize) {
        updateFontSize(targetText, parent, fontSize);

        parent.widthProperty().addListener((observable, oldValue, newValue) -> updateFontSize(targetText, parent, fontSize));
        parent.heightProperty().addListener((observable, oldValue, newValue) -> updateFontSize(targetText, parent, fontSize));
    }

    // Listens to container size changes and update the font size to always fit
    public static void bindFontToAlwaysFit(Node targetText, Region container) {
        updateAutoFitFontSize(targetText, container);

        container.widthProperty().addListener((observable, oldValue, newValue) -> updateAutoFitFontSize(targetText, container));
        container.heightProperty().addListener((observable, oldValue, newValue) -> updateAutoFitFontSize(targetText, container));
    }

    // Updates font size numerically
    private static void updateFontSize(Node targetText, Region parent, double fontSize) {
        double updatedFontSize = getUpdatedFontSize(parent.widthProperty().get(), parent.heightProperty().get(), fontSize);

        Platform.runLater(() -> {
            switch (targetText) {
                case Text text -> text.setFont(Font.font(updatedFontSize));
                case TextField textField -> textField.setFont(Font.font(updatedFontSize));
                case Button textButton -> textButton.setFont(Font.font(updatedFontSize));
                case ComboBox<?> comboBox -> comboBox.setStyle("-fx-font-size: " + updatedFontSize + "px;");
                case null, default -> {
                    assert targetText != null;
                    throw new IllegalArgumentException("Unsupported target text type: " + targetText.getClass().getSimpleName());
                }
            }
        });
    }

    // Updates font size to always fit the parent container
    private static void updateAutoFitFontSize(Node targetText, Region container) {
        double maxWidth = container.widthProperty().get() * 0.65; // 35% margin tolerance
        double maxHeight = container.heightProperty().get() * 0.65;

        Platform.runLater(() -> {
           switch (targetText) {
               case Text text:
                   text.setFont(Font.font(calculateAutoFitFontSize(maxWidth, maxHeight, text.getText())));
                   break;
               case TextField textField:
                   textField.setFont(Font.font(calculateAutoFitFontSize(maxWidth, maxHeight, "")));
                   break;
               case Button button:
                   button.setFont(Font.font(calculateAutoFitFontSize(maxWidth, maxHeight, button.getText())));
                   break;
               case ComboBox<?> comboBox:
                   comboBox.setStyle("-fx-font-size: " + calculateAutoFitFontSize(maxWidth, maxHeight, comboBox.getPromptText()) + "px;");
                   break;
               default:
                   throw new IllegalStateException("Unsupported target text type: " + targetText.getClass().getSimpleName());
           }
        });
    }

    // Gets updated font size depending on current screen size
    private static double getUpdatedFontSize(double width, double height, double DEFAULT_FONT_SIZE) {
        // Ratio works best at 16:9
        return DEFAULT_FONT_SIZE * (16.0 / 25.0 * (width / Main.DEFAULT_WIDTH) + 9.0 / 25.0 * (height / Main.DEFAULT_HEIGHT));
    }

    // Calculates the best font size to auto-fit
    private static double calculateAutoFitFontSize(double maxWidth, double maxHeight, String textContent) {
        double fontSize = 0;
        double step = 0.5;

        Text tempText = new Text();
        tempText.setText(textContent);
        tempText.setFont(Font.font(fontSize));

        while (tempText.getBoundsInLocal().getWidth() <= maxWidth && tempText.getBoundsInLocal().getHeight() <= maxHeight) {
            fontSize += step;
            tempText.setFont(Font.font(fontSize));
        }

        return fontSize - step;
    }
}