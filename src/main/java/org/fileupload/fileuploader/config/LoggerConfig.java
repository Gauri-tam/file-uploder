package org.fileupload.fileuploader.config;

import java.util.logging.*;

public class LoggerConfig {

    public static final Logger logger = Logger.getLogger("FileUploaderLogger");
    static {
        try{
            FileHandler fileHandler = new FileHandler("file-uploader.log", false);

            // Custom formatter to print only the message
            fileHandler.setFormatter(new SimpleFormatter() {
                private static final String format = "%s%n";

                @Override
                public synchronized String format(LogRecord record) {
                    return String.format(format, record.getMessage());
                }
            });

            fileHandler.setFilter(record -> !record.getSourceMethodName().equals("loadExistingFiles"));
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);

            // Optional: disable default console logging
            Logger rootLogger = Logger.getLogger("");
            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
