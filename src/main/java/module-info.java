module gomokugame {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.xml.dom;
    requires java.desktop;
    requires jdk.compiler;

    exports gomokugame.server;
    opens gomokugame.server to javafx.fxml;
    exports gomokugame.objects;
    opens gomokugame.objects to javafx.fxml;
    exports gomokugame.client;
    opens gomokugame.client to javafx.fxml;
    exports gomokugame;
    opens gomokugame to javafx.fxml;
    exports gomokugame.guis;
    opens gomokugame.guis to javafx.fxml;
    exports gomokugame.guis.elements;
    opens gomokugame.guis.elements to javafx.fxml;
    exports gomokugame.guis.layouts;
    opens gomokugame.guis.layouts to javafx.fxml;
}