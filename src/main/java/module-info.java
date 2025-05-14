module org.fileupload.fileuploader {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires java.logging;
    requires jdk.unsupported;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.s3;

    opens org.fileupload.fileuploader to javafx.fxml;
    exports org.fileupload.fileuploader;
}