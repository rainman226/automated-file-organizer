package uvt.sma.agents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

public class MonitorAgent extends Agent {
    private Set<String> scannedFiles = new java.util.HashSet<>();
    private String directory;
    private Boolean deepScan;
    private static final Logger LOGGER = LogManager.getLogger(MonitorAgent.class);

    @Override
    protected void setup() {
        LOGGER.info("Monitor Agent " + getLocalName() + " is starting up.");

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            directory = (String) args[0];
            if (args.length > 1) {
                deepScan = Boolean.parseBoolean((String) args[1]);
            }
        }
        LOGGER.info("Monitoring directory: {}", directory);
        LOGGER.info("Deep scan enabled: {}", deepScan);

        addBehaviour(new ScanFolder());
    }

    @Override
    protected void takeDown() {
        System.out.println("Monitor Agent " + getLocalName() + " is shutting down.");
        // Cleanup code can be added here
    }

    private class ScanFolder extends OneShotBehaviour {
        @Override
        public void action() {

            try {
                if (deepScan) {
                    // Recursive scan
                    Files.walk(Paths.get(directory))
                            .filter(Files::isRegularFile)
                            .forEach(filePath -> {
                                String fileName = filePath.getFileName().toString();
                                if (!scannedFiles.contains(fileName)) {
                                    LOGGER.info("[Deep scan] File detected: {}", fileName);
                                    scannedFiles.add(fileName);
                                }
                            });
                } else {
                    // Top-level only
                    Files.list(Paths.get(directory))
                            .filter(Files::isRegularFile)
                            .forEach(filePath -> {
                                String fileName = filePath.getFileName().toString();
                                if (!scannedFiles.contains(fileName)) {
                                    LOGGER.info("[Shallow scan] File detected: {}", fileName);
                                    scannedFiles.add(fileName);
                                }
                            });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
