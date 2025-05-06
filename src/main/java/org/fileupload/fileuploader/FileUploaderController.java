package org.fileupload.fileuploader;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fileupload.fileuploader.databaseConnection.SQLiteConnection;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileUploaderController {

    private File selectedFile;
    private Stage primaryStage;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @FXML private Label fileLabel;
    @FXML private GridPane fileInfoPane;
    @FXML private Label fileNameLabel;
    @FXML private Label fileSizeLabel;
    @FXML private Label fileTypeLabel;
    @FXML private Label statusLabel;
    @FXML private TextArea logTextArea;
    @FXML private ProgressBar uploadProgress;
    @FXML private Label progressLabel;
    @FXML private VBox progressBox;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        SQLiteConnection.initializeDatabase();
//        refreshLogs();
    }

    @FXML
    private void handleChooseFileButton() {
        FileChooser fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            fileLabel.setText("Selected: " + selectedFile.getName());
            statusLabel.setText("");

            // Show file info
            fileInfoPane.setVisible(true);
            fileNameLabel.setText(selectedFile.getName());
            fileSizeLabel.setText(formatFileSize(selectedFile.length()));
            fileTypeLabel.setText(getFileExtension(selectedFile.getName()));
        } else {
            fileLabel.setText("No file selected");
            fileInfoPane.setVisible(false);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "Unknown" : filename.substring(dotIndex + 1).toUpperCase();
    }

    @FXML
    private void handleUploadButton() {
        if (selectedFile == null) {
            statusLabel.setText("Please choose a file first.");
            statusLabel.getStyleClass().add("error");
            return;
        }

        // Show progress UI
        progressBox.setVisible(true);
        uploadProgress.setProgress(0);
        progressLabel.setText("Preparing upload...");
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll("success", "error");

        // Run upload in background thread
        executorService.submit(() -> {
            try {
                // Simulate upload progress
                for (int i = 0; i <= 100; i += 5) {
                    final int progress = i;
                    Platform.runLater(() -> {
                        uploadProgress.setProgress(progress / 100.0);
                        progressLabel.setText("Uploading... " + progress + "%");
                    });
                    Thread.sleep(100); // Simulate work
                }

                // Perform actual upload
                boolean success = performUpload(selectedFile, false);

                Platform.runLater(() -> {
                    if (success) {
                        statusLabel.setText("Upload Successful");
                        statusLabel.getStyleClass().add("success");
                    } else {
                        // Retry logic
                        boolean retrySuccess = performUpload(selectedFile, true);
                        if (retrySuccess) {
                            statusLabel.setText("Upload Successful (after retry)");
                            statusLabel.getStyleClass().add("success");
                        } else {
                            statusLabel.setText("Upload Failed");
                            statusLabel.getStyleClass().add("error");
                        }
                    }
                    progressBox.setVisible(false);
//                    refreshLogs();
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Upload Interrupted");
                    statusLabel.getStyleClass().add("error");
                    progressBox.setVisible(false);
                });
                Thread.currentThread().interrupt();
            }
        });
    }

    private boolean performUpload(File file, boolean isRetry) {
        try {
            // Simulate potential failure (20% chance)
            boolean success = Math.random() > 0.2;

            // Log the upload attempt
            SQLiteConnection.logUpload(
                    file.getName(),
                    file.length(),
                    success ? "SUCCESS" : (isRetry ? "RETRY_FAILED" : "FAILED"),
                    success ? "File uploaded successfully" :
                            (isRetry ? "Retry upload failed" : "Upload failed")
            );

            return success;
        } catch (Exception e) {
            SQLiteConnection.logUpload(
                    file.getName(),
                    file.length(),
                    "ERROR",
                    "Upload error: " + e.getMessage()
            );
            return false;
        }
    }

//    private void refreshLogs() {
//        List<SQLiteConnection.UploadLog> logs = SQLiteConnection.getRecentLogs(10); // Get last 10 logs
//
//        StringBuilder logText = new StringBuilder("=== Upload History ===\n\n");
//        for (SQLiteConnection.UploadLog log : logs) {
//            logText.append(String.format(
//                    "[%s] %s - %s (%s)\n%s\n\n",
//                    log.getTimestamp(),
//                    log.getFilename(),
//                    log.getStatus(),
//                    log.getFilesize() > 0 ? formatFileSize(log.getFilesize()) : "N/A",
//                    log.getMessage()
//            ));
//        }
//
//        Platform.runLater(() -> {
//            logTextArea.setText(logText.toString());
//        });
//    }

    private boolean simulateUpload(File file) {
        // Simulate success   if no use of the file than why do we get here.
        return true;
    }
}
