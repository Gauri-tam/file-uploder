package org.fileupload.fileuploader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class FileUploader extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(FileUploader.class.getResource("file-uploader.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 900);

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
