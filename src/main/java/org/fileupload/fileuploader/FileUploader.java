package org.fileupload.fileuploader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fileupload.fileuploader.config.LoggerConfig;

import java.util.logging.Logger;

public class FileUploader extends Application {

    Logger logger = LoggerConfig.logger;

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Application Start...");
        FXMLLoader fxmlLoader = new FXMLLoader(FileUploader.class.getResource("file-uploader.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1050, 1000);

        // Getting the controller and setting the primary stage
        FileUploaderController controller = fxmlLoader.getController();
        controller.setPrimaryStage(primaryStage);

        primaryStage.setTitle("File Upload Example");
        primaryStage.setResizable(true);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}
