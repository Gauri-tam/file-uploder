module org.fileupload.fileuploader {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;


    opens org.fileupload.fileuploader to javafx.fxml;
    exports org.fileupload.fileuploader;
}