package com.server;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

public class LoginController {
    public TextField portEdit;

    public void createServer(MouseEvent event) {
        Server server = null;
        try {
            server = new Server(Integer.valueOf(portEdit.getText()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (server != null) {
            server.start();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
            try {
                Parent root = (Parent) loader.load();
                MainController controller = loader.getController();
                server.getDispatcher().setController(controller);
                stage.setScene(new Scene(root));
                stage.show();
                stage.setOnCloseRequest((event1) -> {
                    System.exit(0);
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
