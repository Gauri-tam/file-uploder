package org.fileupload.fileuploader.config;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SQLiteConnConfig {

    private static Logger logger = LoggerConfig.logger;

    private static final String DB_URL = "jdbc:sqlite:fileupload.db";
    private static boolean initialized = false;

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static synchronized void initializeDatabase() throws Exception {
        if (initialized) {
            return;
        }

        String createTable = """
                CREATE TABLE IF NOT EXISTS upload_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    filename TEXT NOT NULL,
                    filesize LONG,
                    status TEXT NOT NULL,
                    message TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createBackupLogs =
                """
                CREATE TABLE IF NOT EXISTS backup_logs (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    upload_id   INTEGER NOT NULL,
                    backup_path TEXT    NOT NULL,
                    status      TEXT    NOT NULL,
                    message     TEXT,
                    timestamp   DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (upload_id) REFERENCES upload_logs(id)
                );
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            stmt.execute(createBackupLogs);
            initialized = true;
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static void logUpload(String filename, String status, String message) {
        logUpload(filename, -1, status, message);
    }

    public static List<UploadLog> getFilesToBackup() {
        List<UploadLog> filesToBackup = new ArrayList<>();
        String sql = "SELECT id, filename, filesize, status, message, timestamp " +
                "FROM upload_logs " +  // Changed from upload_log to upload_logs
                "WHERE status = 'SUCCESS' " +
                "ORDER BY timestamp DESC";  // Changed from upload_time to timestamp

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UploadLog log = new UploadLog(
                        rs.getInt("id"),
                        rs.getString("filename"),
                        rs.getLong("filesize"),
                        rs.getString("status"),
                        rs.getString("message"),
                        rs.getString("timestamp")
                );
                filesToBackup.add(log);
            }
        } catch (SQLException e) {
            logger.info("Error getting files to backup: " + e.getMessage());
        }

        return filesToBackup;
    }


    public static void logUpload(String filename, long filesize, String status, String message) {
        String sql = "INSERT INTO upload_logs (filename, filesize, status, message) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, filename);
            pstmt.setLong(2, filesize);
            pstmt.setString(3, status);
            pstmt.setString(4, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.info("Error logging upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<UploadLog> getRecentLogs(int limit) {
        String sql = "SELECT id, filename, filesize, status, message, timestamp FROM upload_logs ORDER BY timestamp DESC LIMIT ?";
        List<UploadLog> logs = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UploadLog log = new UploadLog(
                        rs.getInt("id"),
                        rs.getString("filename"),
                        rs.getLong("filesize"),
                        rs.getString("status"),
                        rs.getString("message"),
                        rs.getString("timestamp")
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            logger.info("Error retrieving logs: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    public static void clearLogs() {
        String sql = "DELETE FROM upload_logs";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("All logs cleared");
        } catch (SQLException e) {
            logger.info("Error clearing logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void logBackup(int id, String absolutePath, String status, String message) {
        String sql = "INSERT INTO backup_logs (upload_id, backup_path, status, message) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setString(2, absolutePath);
            pstmt.setString(3, status);
            pstmt.setString(4, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.info("Error logging backup: " + e.getMessage());
        }
    }

    // Helper class to represent log entries
    public static class UploadLog {
        private final int id;
        private final String filename;
        private final long filesize;
        private final String status;
        private final String message;
        private final String timestamp;

        public UploadLog(int id, String filename, long filesize, String status, String message, String timestamp) {
            this.id = id;
            this.filename = filename;
            this.filesize = filesize;
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }

        public int getId() { return id; }
        public String getFilename() { return filename; }
        public long getFilesize() { return filesize; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            return String.format("[%s] %s (%s): %s - %s",
                    timestamp, filename, status, message, filesize > 0 ? formatFileSize(filesize) : "");
        }

        private String formatFileSize(long bytes) {
            final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
            int unitIndex = 0;
            double size = bytes;

            while (size > 1024 && unitIndex < units.length - 1) {
                size /= 1024;
                unitIndex++;
            }

            return String.format("%.2f %s", size, units[unitIndex]);
        }
    }
}
