package org.fileupload.fileuploader;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import org.fileupload.fileuploader.databaseConnection.SQLiteConnection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileUploaderController {

    private ObservableList<File> selectedFiles = FXCollections.observableArrayList();
    private File backupDirectory;
    private Stage primaryStage;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    // UI Components
    @FXML private ListView<File> selectedFilesList;
    @FXML private Label statusLabel;
    @FXML private ProgressBar uploadProgress;
    @FXML private Label progressLabel;
    @FXML private VBox uploadProgressBox;
    @FXML private VBox dropZone;

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
        SQLiteConnection.initializeDatabase();
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

        selectedFilesList.setVisible(false);
        uploadProgressBox.setVisible(false);
        backupProgressBox.setVisible(false);
        lastBackupLabel.setText("Never");
        loadExistingFiles();
        
    }

    private void loadExistingFiles() {
        List<SQLiteConnection.UploadLog> logs = SQLiteConnection.getRecentLogs(50);
        filesTable.getItems().clear();

        for (SQLiteConnection.UploadLog log : logs) {
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
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Upload");
        List<File> files = fileChooser.showOpenMultipleDialog(primaryStage);
        updateFileInfo(files);
    }
    private void updateFileInfo(List<File> files) {
        if (files != null && !files.isEmpty()) {
            selectedFiles.addAll(files);
            selectedFilesList.setVisible(true);
            statusLabel.setText("");
        } else {
            selectedFilesList.setVisible(false);
        }
    }

    @FXML
    private void handleUploadButton() {
        if (selectedFiles.isEmpty()) {
            showStatusMessage("Please select files first.", "error");
            return;
        }

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
                    } else {
                        // Retry once if failed
                        success = performUpload(file, true);
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
                        }
                    }

                    updateProgress(currentFile, totalFiles);
                }

                final int finalSuccessCount = successfulUploads;
                Platform.runLater(() -> {
                    if (finalSuccessCount == totalFiles) {
                        showStatusMessage("All files uploaded successfully!", "success");
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
        statusLabel.getStyleClass().add(styleClass);
    }

    private boolean performUpload(File file, boolean isRetry) {
        try {
            boolean success = true; // Set based on actual upload result
            String status = success ? "SUCCESS" : (isRetry ? "RETRY_FAILED" : "FAILED");
            String message = success ? "Upload successful" :
                    (isRetry ? "Retry failed" : "Upload failed");

            // Log to database
            SQLiteConnection.logUpload(
                    file.getName(),
                    file.length(),
                    status,
                    message
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

    // Backup
    @FXML
    private void startManualBackup() {
        if (backupDirectory == null) {
            chooseBackupDirectory();
            if (backupDirectory == null) return;
        }

        Task<Void> backupTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                Platform.runLater(() -> {
                    backupProgressBox.setVisible(true);
                    backupStatusLabel.setText("Starting backup...");
                });

                // Get files to backup from database
                List<SQLiteConnection.UploadLog> filesToBackup = SQLiteConnection.getFilesToBackup();
                int totalFiles = filesToBackup.size();
                int processed = 0;

                for (SQLiteConnection.UploadLog file : filesToBackup) {
                    if (isCancelled()) break;

                    try {
                        backupFile(file);
                        processed++;
                        updateProgress(processed, totalFiles);
                        updateMessage("Backing up " + file.getFilename() +
                                " (" + processed + "/" + totalFiles + ")");
                    } catch (Exception e) {
                        SQLiteConnection.logBackup(
                                file.getId(),
                                "",
                                "FAILED",
                                "Backup error: " + e.getMessage()
                        );
                    }
                }

                Platform.runLater(() -> {
                    lastBackupLabel.setText(getCurrentDateTime());
                    if (!isCancelled()) {
                        backupStatusLabel.setText("Backup completed successfully!");
                        backupStatusLabel.setStyle("-fx-text-fill: #2e7d32;");

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

                return null;
            }
        };

        backupTask.setOnFailed(e -> {
            backupStatusLabel.setText("Backup failed: " + backupTask.getException().getMessage());
            backupStatusLabel.setStyle("-fx-text-fill: #d32f2f;");
        });

        backupProgress.progressProperty().bind(backupTask.progressProperty());
        backupStatusLabel.textProperty().bind(backupTask.messageProperty());

        executorService.submit(backupTask);
    }

    private void backupFile(SQLiteConnection.UploadLog file) throws IOException {
        File sourceFile = new File(file.getFilename());
        if (!sourceFile.exists()) {
            SQLiteConnection.logBackup(
                    file.getId(),
                    "",
                    "FAILED",
                    "Source file not found"
            );
            return;
        }

        File destFile = new File(backupDirectory, sourceFile.getName());
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        SQLiteConnection.logBackup(
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
            event.acceptTransferModes(TransferMode.COPY);
            dropZone.setStyle("-fx-border-color: #4a6baf; -fx-background-color: #e6e9f2;");
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            selectedFiles.addAll(db.getFiles());
            selectedFilesList.setVisible(!selectedFiles.isEmpty());
            fileLabel.setText(selectedFiles.size() + " files selected");
            success = true;
        }

        dropZone.setStyle("-fx-border-color: #d1d5db; -fx-background-color: #f9fafb;");
        event.setDropCompleted(success);
        event.consume();
    }


    @FXML
    private void handleDragExited(DragEvent event) {
        dropZone.setStyle("-fx-border-color: #d1d5db; -fx-background-color: #f9fafb;");
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