package org.fileupload.fileuploader;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.prefs.Preferences;

public class ActivationController {
    @FXML private TextField activationCodeField;
    @FXML private Label errorLabel;

    private static final String SALT = "YourAppSaltValue123!";
    private static final String VALID_HASH = "a1b2c3d4..."; // Replace with your actual hash

    private Preferences prefs = Preferences.userNodeForPackage(ActivationController.class);

    @FXML
    private void handleActivation() {
        String enteredCode = activationCodeField.getText().trim();

        if (validateActivationCode(enteredCode)) {
            // Code is valid
            storeActivation(true);
            errorLabel.setVisible(false);
            // Proceed to main application
            MainApp.showMainApplication();
        } else {
            // Invalid code
            errorLabel.setText("Invalid activation code. Please try again.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    private void handleDemoMode() {
        storeActivation(false);
        MainApp.showMainApplication();
    }

    private boolean validateActivationCode(String code) {
        try {
            String hashed = hashActivationCode(code);
            return hashed.equals(VALID_HASH);
        } catch (Exception e) {
            return false;
        }
    }

    private String hashActivationCode(String code) throws Exception {
        String toHash = SALT + code + SALT;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void storeActivation(boolean activated) {
        prefs.putBoolean("activated", activated);
        prefs.putLong("activation_timestamp", System.currentTimeMillis());
    }

    public static boolean isActivated() {
        Preferences prefs = Preferences.userNodeForPackage(ActivationController.class);
        return prefs.getBoolean("activated", false);
    }
}
