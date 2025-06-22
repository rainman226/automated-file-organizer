package uvt.sma.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uvt.sma.helpers.StartPlatform;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class MainController {
    private static final Logger LOGGER = LogManager.getLogger(MainController.class);
    @FXML
    private Button sourceSelectButton;
    @FXML
    private Button targetSelectButton;
    @FXML
    private Button deepScanButton;
    @FXML
    private TextField sourceTextField;
    @FXML
    private TextField targetTextField;
    @FXML
    private Text infoText;
    @FXML
    private Text categoriesText;
    @FXML
    private TextArea textArea;

    private String sourcePath;
    private String targetPath;
    private Boolean deepScan = false;

    @FXML
    public void initialize() {
        LOGGER.info("MainController initialized.");
        textArea.setEditable(false);

        // Custom OutputStream to redirect to TextArea
        OutputStream outputStream = new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                buffer.append((char) b);
                // Flush on newline or periodically to avoid missing partial text
                if (b == '\n' || buffer.length() >= 1024) {
                    flushBuffer();
                }
            }

            @Override
            public void write(byte[] b, int off, int len) {
                String str = new String(b, off, len, StandardCharsets.UTF_8);
                buffer.append(str);
                // Flush on newline or large buffer
                if (str.contains("\n") || buffer.length() >= 1024) {
                    flushBuffer();
                }
            }

            private void flushBuffer() {
                String text = buffer.toString();
                if (!text.isEmpty()) {
                    Platform.runLater(() -> textArea.appendText(text));
                    buffer.setLength(0);
                }
            }
        };

        // Create stream
        PrintStream printStream = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
        System.setOut(printStream);
        System.setErr(printStream); // Redirect errors too

        LOGGER.info("Redirected System.out to TextArea");

        System.out.println("Select the source and target folders for the file sorting.");
        System.out.println("Categories: Documents, Images, Videos, Music, Other");
    }

    public void getSourceFolder() {
        // Inside your method or event handler:
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");

        File selectedDirectory = directoryChooser.showDialog(new Stage());

        if (selectedDirectory != null) {
            sourcePath = selectedDirectory.getAbsolutePath();
            sourceTextField.setText(selectedDirectory.getAbsolutePath());
        } else {
            System.out.println("No folder selected.");
        }
    }

    public void getTargetFolder() {
        // Inside your method or event handler:
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder");

        File selectedDirectory = directoryChooser.showDialog(new Stage());

        if (selectedDirectory != null) {
            targetPath = selectedDirectory.getAbsolutePath();
            targetTextField.setText(selectedDirectory.getAbsolutePath());
        } else {
            System.out.println("No folder selected.");
        }
    }

    public void toggleDeepScan() {
        if(!deepScan) {
            deepScan = true;
            deepScanButton.setText("Deep Scan: ON");
            System.out.println("Deep scan enabled.");
        } else {
            deepScan = false;
            deepScanButton.setText("Deep Scan: OFF");
            System.out.println("Deep scan disabled.");
        }
    }

    public void startSorting() {
        if (sourcePath == null || targetPath == null) {
            infoText.setText("Please select both source and target folders.");
            return;
        }

            StartPlatform platform = new StartPlatform(sourcePath, targetPath, deepScan);
            platform.startPlatform();

    }
}
