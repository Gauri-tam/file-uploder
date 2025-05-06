package org.fileupload.fileuploader;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Check activation status
        if (ActivationController.isActivated() || isFirstRun()) {
            showMainApplication(primaryStage);
        } else {
            showActivationScreen(primaryStage); // Error 2 // Error 4
        }
    }

    private boolean isFirstRun() {
        // Implement logic to check if this is the first run
        // Could check a preferences value or file existence
        return false;
    }

    public static void showMainApplication() {
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                Parent root = FXMLLoader.load(MainApp.class.getResource("main.fxml")); // Error 1
                stage.setScene(new Scene(root));
                stage.setTitle("Your Application");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showMainApplication(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Your Application");
        primaryStage.show();
    }

    private void showActivationScreen(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("activation.fxml"));  //  Error 3
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Activate Your Software");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
