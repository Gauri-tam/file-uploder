package org.fileupload.fileuploader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.fileupload.fileuploader.config.LoggerConfig;

import java.util.logging.Logger;

public class FileUploader extends Application {

    Logger logger = LoggerConfig.logger;

//    @Override
//    public void start(Stage primaryStage) throws Exception {
//        logger.info("Application Start...");
//        FXMLLoader fxmlLoader = new FXMLLoader(FileUploader.class.getResource("file-uploader.fxml"));
//        Scene scene = new Scene(fxmlLoader.load(), 1050, 1000);
//
//        // Step 1: Hardcoded AWS credentials  // we can create a configuration for this
//        String accessKey = "YOUR_ACCESS_KEY";
//        String secretKey = "YOUR_SECRET_KEY";
//        String bucketName = "your-bucket-name";
//        String region = "us-east-1";
//
//        // Step 2: Initialize S3Service
//        S3Service s3Service = new S3Service(accessKey, secretKey, bucketName, region);
//
//        // Step 3: Inject S3Service into the controller
//        FileUploaderController controller = fxmlLoader.getController();
//        controller.setPrimaryStage(primaryStage);
//        controller.setS3Service(s3Service);
//
//        // Step 4: Setup stage
//        primaryStage.setTitle("File Upload Example");
//        primaryStage.setResizable(true);
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Application Start...");
        FXMLLoader fxmlLoader = new FXMLLoader(FileUploader.class.getResource("file-uploader.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1050, 1000);

        // Get the controller and set the primary stage
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
