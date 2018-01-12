package com.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class MainController {

    @FXML
    public TextArea logWindow;

    public void addLog(String text) {
        Platform.runLater(() -> logWindow.appendText(text));
    }
}
