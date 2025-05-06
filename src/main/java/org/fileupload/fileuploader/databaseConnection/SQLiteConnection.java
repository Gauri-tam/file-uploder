package org.fileupload.fileuploader.databaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteConnection {

    private static final String DB_URL = "jdbc:sqlite:fileupload.db";
    private static boolean initialized = false;

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static synchronized void initializeDatabase() {
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

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            initialized = true;
            System.out.println("Database initialized successfully"); // after that we have to see the logs of the file when it is uploaded or having any issue.
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static void logUpload(String filename, String status, String message) {
        logUpload(filename, -1, status, message);
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
            System.err.println("Error logging upload: " + e.getMessage());
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
            System.err.println("Error retrieving logs: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    public static void clearLogs() {
        String sql = "DELETE FROM upload_logs";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("All logs cleared");
        } catch (SQLException e) {
            System.err.println("Error clearing logs: " + e.getMessage());
            e.printStackTrace();
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
