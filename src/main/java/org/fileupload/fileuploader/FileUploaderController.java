package org.fileupload.fileuploader;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fileupload.fileuploader.config.LoggerConfig;
import org.fileupload.fileuploader.config.SQLiteConnConfig;
import org.fileupload.fileuploader.services.S3Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class FileUploaderController {

    Logger logger = LoggerConfig.logger;

    private ObservableList<File> selectedFiles = FXCollections.observableArrayList();
    private File backupDirectory;
    private Stage primaryStage;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final StringProperty lastBackup = new SimpleStringProperty("Never");
    private S3Service s3Service;

    // UI Components
    @FXML private ListView<File> selectedFilesList;
    @FXML private Label statusLabel;
    @FXML private ProgressBar uploadProgress;
    @FXML private Label progressLabel;
    @FXML private VBox uploadProgressBox;
    @FXML private VBox dropZone;
    @FXML private Label dropZoneSuccess;

    // Backup Components
    @FXML private Label fileLabel;
    @FXML private Label lastBackupLabel;
    @FXML private ProgressBar backupProgress;
    @FXML private Label backupStatusLabel;
    @FXML private VBox backupProgressBox;

    // Files Table
    @FXML private TableView<FileRecord> filesTable;

    public void setPrimaryStage(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        logger.info("Initializing database connection...");
        SQLiteConnConfig.initializeDatabase();
        initializeUI();
    }

    private void initializeUI() {
        // Initialize the files list view
        selectedFilesList.setItems(selectedFiles);
        selectedFilesList.setCellFactory(lv -> new ListCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.getChildren().addAll(
                            new Label(file.getName()),
                            new Label(formatFileSize(file.length())),
                            new Label(getFileExtension(file.getName()).toUpperCase())
                    );
                    setGraphic(hbox);
                }
            }
        });

        lastBackupLabel.setText("Never");
        lastBackupLabel.textProperty().bind(lastBackup);
        selectedFilesList.setVisible(false);
        uploadProgressBox.setVisible(false);
        backupProgressBox.setVisible(false);

        loadExistingFiles();

    }

    private void loadExistingFiles() {
        List<SQLiteConnConfig.UploadLog> logs = SQLiteConnConfig.getRecentLogs(50);
        filesTable.getItems().clear();
        logger.info("Loading recent upload logs into files table.");

        for (SQLiteConnConfig.UploadLog log : logs) {
            filesTable.getItems().add(new FileRecord(
                    log.getFilename(),
                    formatFileSize(log.getFilesize()),
                    log.getTimestamp(),
                    getFileExtension(log.getFilename()),
                    log.getStatus()
            ));
        }
    }

    @FXML
    private void handleChooseFileButton() {
        logger.info("Opening file chooser dialog...");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Upload");
        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
        logger.info(files == null || files.isEmpty() ? "No files selected." : files.size() + " files selected.");
        updateFileInfo(files);
    }
    private void updateFileInfo(List<File> files) {
        if (files != null && !files.isEmpty()) {
            selectedFiles.addAll(files);
            selectedFilesList.setVisible(true);
            statusLabel.setText("");
        } else {
            selectedFilesList.setVisible(true);
        }
    }

    @FXML
    public void handleRemoveFile(ActionEvent actionEvent) {

        File selectedFile = selectedFilesList.getSelectionModel().getSelectedItem();

        if (selectedFile != null) {

            selectedFilesList.getItems().remove(selectedFile);

            if (selectedFilesList.getItems().isEmpty()) {
                selectedFilesList.setVisible(false);
            }
        }
    }

    @FXML
    private void handleUploadButton() {
        if (selectedFiles.isEmpty()) {
            logger.info("Upload button clicked without any files selected.");
            showStatusMessage("Please select files first.", "error");
            return;
        }
        logger.info("Starting upload of " + selectedFiles.size() + " files.");

        uploadProgressBox.setVisible(true);
        progressLabel.setText("Preparing to upload " + selectedFiles.size() + " files...");
        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll("success", "error");

        Task<Void> uploadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int totalFiles = selectedFiles.size();
                int successfulUploads = 0;

                for (int i = 0; i < totalFiles; i++) {
                    if (isCancelled()) break;

                    File file = selectedFiles.get(i);
                    final int currentFile = i + 1;

                    Platform.runLater(() -> {
                        progressLabel.setText(String.format("Uploading file %d of %d: %s",
                                currentFile, totalFiles, file.getName()));
                    });

                    // Simulate upload progress for this file
                    for (int progress = 0; progress <= 100; progress += 5) {
                        updateProgress(currentFile - 1 + (progress / 100.0), totalFiles);
                        Thread.sleep(50);
                    }

                    logger.info("Uploading file: " + file.getName());

                    // Perform actual upload
                    boolean success = performUpload(file, false);
                    if (success) {
                        successfulUploads++;
                        Platform.runLater(() -> {
                            filesTable.getItems().add(0, new FileRecord(
                                    file.getName(),
                                    formatFileSize(file.length()),
                                    getCurrentDateTime(),
                                    getFileExtension(file.getName()).toUpperCase(),
                                    "Completed"
                            ));
                        });
                        logger.info("Upload successful: " + file.getName());
                    } else {
                        // Retry once if failed
                        success = performUpload(file, true);
                        logger.warning("Upload failed, retrying: " + file.getName());
                        if (success) {
                            successfulUploads++;
                            Platform.runLater(() -> {
                                filesTable.getItems().add(0, new FileRecord(
                                        file.getName(),
                                        formatFileSize(file.length()),
                                        getCurrentDateTime(),
                                        getFileExtension(file.getName()).toUpperCase(),
                                        "Completed (retry)"
                                ));
                            });
                            logger.info("Retry upload successful: " + file.getName());
                        } else {
                            Platform.runLater(() -> {
                                filesTable.getItems().add(0, new FileRecord(
                                        file.getName(),
                                        formatFileSize(file.length()),
                                        getCurrentDateTime(),
                                        getFileExtension(file.getName()).toUpperCase(),
                                        "Failed"
                                ));
                            });
                            logger.warning("Retry upload failed: " + file.getName());
                        }
                    }

                    updateProgress(currentFile, totalFiles);
                }

                final int finalSuccessCount = successfulUploads;
                Platform.runLater(() -> {
                    if (finalSuccessCount == totalFiles) {
                        showStatusMessage("All files uploaded successfully!", "success");
                        uploadProgressBox.setVisible(false);
                    } else if (finalSuccessCount > 0) {
                        showStatusMessage(String.format("%d of %d files uploaded successfully",
                                finalSuccessCount, totalFiles), "success");
                    } else {
                        showStatusMessage("No files were uploaded successfully", "error");
                    }
                    selectedFiles.clear();
                    selectedFilesList.setVisible(false);
                });

                return null;
            }
        };

        uploadTask.setOnFailed(e -> {
            showStatusMessage("Upload Error: " + uploadTask.getException().getMessage(), "error");
            uploadProgressBox.setVisible(false);
        });

        uploadProgress.progressProperty().bind(uploadTask.progressProperty());

        executorService.submit(uploadTask);
    }

    private void showStatusMessage(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success", "error");
        logger.info("Status Message - [" + styleClass.toUpperCase() + "]: " + message);

        if ("success".equals(styleClass)) {
            statusLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else if ("error".equals(styleClass)) {
            statusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("");
        }

        statusLabel.getStyleClass().add(styleClass);
    }

    private boolean performUpload(File file, boolean isRetry) {
        try {
            boolean success = true; // Set based on actual upload result
            logger.info((isRetry ? "Retrying upload for: " : "Uploading: ") + file.getName());
            String status = success ? "SUCCESS" : (isRetry ? "RETRY_FAILED" : "FAILED");
            String message = success ? "Upload successful" :
                    (isRetry ? "Retry failed" : "Upload failed");

            // Log to database
            SQLiteConnConfig.logUpload(
                    file.getName(),
                    file.length(),
                    status,
                    message
            );

            return success;
        } catch (Exception e) {
            SQLiteConnConfig.logUpload(
                    file.getName(),
                    file.length(),
                    "ERROR",
                    "Upload error: " + e.getMessage()
            );
            return false;
        }
    }

    // uploading all the data on the Amazon s3 Server
//    private boolean performUpload(File file, boolean retry) {
//        try {
//            String s3Key = "uploads/" + file.getName(); // or any custom path
//            s3Service.uploadFile(file.toPath(), s3Key);
//            return true;
//        } catch (Exception e) {
//            logger.warning("Upload failed for file: " + file.getName() + ". Error: " + e.getMessage());
//            return false;
//        }
//    }


    // Backup
    @FXML
    private void startManualBackup() {
        if (backupDirectory == null) {
            logger.info("No backup directory set. Prompting user to select one.");
            chooseBackupDirectory();
            if (backupDirectory == null) {
                logger.warning("Backup canceled. No directory selected.");
                return;
            }
        }

        logger.info("Starting manual backup to: " + backupDirectory.getAbsolutePath());

        Task<Void> backupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                Platform.runLater(() -> {
                    backupProgressBox.setVisible(true);
                    backupStatusLabel.textProperty().unbind();
                    backupStatusLabel.setText("Starting backup...");
                });

                // Get files to backup from database
                List<SQLiteConnConfig.UploadLog> filesToBackup = SQLiteConnConfig.getFilesToBackup();
                int totalFiles = filesToBackup.size();
                int processed = 0;

                for (SQLiteConnConfig.UploadLog file : filesToBackup) {
                    if (isCancelled()) break;

                    logger.info("Backing up file: " + file.getFilename());

                    try {
                        backupFile(file);
                        processed++;
                        updateProgress(processed, totalFiles);
                        updateMessage("Backing up " + file.getFilename() +
                                " (" + processed + "/" + totalFiles + ")");
                    } catch (Exception e) {
                        SQLiteConnConfig.logBackup(
                                file.getId(),
                                "",
                                "FAILED",
                                "Backup error: " + e.getMessage()
                        );
                    }
                }

                Platform.runLater(() -> {
                    lastBackupLabel.textProperty().unbind();
                    lastBackupLabel.setText(getCurrentDateTime());
                    if (!isCancelled()) {
                        backupStatusLabel.textProperty().unbind();
                        backupStatusLabel.setText("Backup completed successfully!");
                        backupStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
                        backupProgressBox.setVisible(false);
                        // Add backup record
                        filesTable.getItems().add(0, new FileRecord(
                                "Full Backup",
                                totalFiles + " files",
                                getCurrentDateTime(),
                                "Backup",
                                "Completed"
                        ));
                    }
                });

                logger.info("Backup completed. Files backed up: " + processed + " of " + totalFiles);

                return null;
            }
        };

        backupTask.setOnFailed(e -> {
            backupStatusLabel.textProperty().unbind();
            backupStatusLabel.setText("Backup failed: " + backupTask.getException().getMessage());
            backupStatusLabel.setStyle("-fx-text-fill: #d32f2f;");
        });

        backupProgress.progressProperty().bind(backupTask.progressProperty());
        backupStatusLabel.textProperty().bind(backupTask.messageProperty());

        executorService.submit(backupTask);
    }

    private void backupFile(SQLiteConnConfig.UploadLog file) throws IOException {
        File sourceFile = new File(file.getFilename());
        if (!sourceFile.exists()) {
            SQLiteConnConfig.logBackup(
                    file.getId(),
                    "",
                    "FAILED",
                    "Source file not found"
            );
            return;
        }

        File destFile = new File(backupDirectory, sourceFile.getName());
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        SQLiteConnConfig.logBackup(
                file.getId(),
                destFile.getAbsolutePath(),
                "SUCCESS",
                "Backed up to " + destFile.getParent()
        );
    }

    @FXML
    private void chooseBackupDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backup Directory");
        if (backupDirectory != null) {
            chooser.setInitialDirectory(backupDirectory);
        }
        backupDirectory = chooser.showDialog(primaryStage);
    }

    // Drag and Drop Handlers
    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            dropZone.setStyle("-fx-border-color: #d1d5db; -fx-padding: 20 10 30 15; -fx-background-color: #f9fafb;");
            dropZoneSuccess.setText("Add More...");
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            // Process the dropped files
            List<File> files = db.getFiles();
            selectedFiles.addAll(db.getFiles());
            selectedFilesList.setVisible(!selectedFiles.isEmpty());
                fileLabel.setText(selectedFiles.size() + " files selected");

            success = true;
        }

        event.setDropCompleted(success);
        dropZone.setStyle("-fx-border-color: #d1d5db; -fx-padding: 20 10 30 15; -fx-background-color: #f9fafb;");
        dropZoneSuccess.setText("Add More...");
        event.consume();
    }

    @FXML
    private void handleDragExited(DragEvent event) {
        dropZone.setStyle("-fx-border-color: #d1d5db; -fx-padding: 20 10 30 15; -fx-background-color: #f9fafb;");
        dropZoneSuccess.setText("Add More...");
        event.consume();
    }

    // Utility Methods
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }

    private String getCurrentDateTime() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void setS3Service(S3Service s3Service) {  // I have doubt abut this
        this.s3Service = s3Service;
        // use the following when you upload the file
        // s3Service.uploadFile(Paths.get("path/to/local/file.txt"), "s3-key.txt");
    }


    // Model class for file records
    public static class FileRecord {
        private final String name;
        private final String size;
        private final String date;
        private final String type;
        private String status;

        public FileRecord(String name, String size, String date, String type, String status) {
            this.name = name;
            this.size = size;
            this.date = date;
            this.type = type;
            this.status = status;
        }

        public String getName() { return name; }
        public String getSize() { return size; }
        public String getDate() { return date; }
        public String getType() { return type; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}