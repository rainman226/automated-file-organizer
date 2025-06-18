package uvt.sma.agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class ClassifierWorkerAgent extends Agent {
    private static final long serialVersionUID = 1L;

    private Set<String> scannedFiles = new java.util.HashSet<>();

    private static final Logger LOGGER = LogManager.getLogger(ClassifierWorkerAgent.class);
    @Override
    protected void setup() {
        // Initialization code for the Classifier Worker Agent
        LOGGER.info("Classifier Worker Agent {} is starting up.", getLocalName());
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String files = (String) args[0];
            processMessageContent(files);

            LOGGER.info("Sucesfully processed scan request with {} files.", scannedFiles.size());

            addBehaviour(new ClassifyFiles());
        } else {
            LOGGER.info("No arguments provided to the Classifier Worker Agent.");
        }
        // Add behaviors or other initialization logic here
    }

    @Override
    protected void takeDown() {
        LOGGER.info("Classifier Worker Agent {} is shutting down.", getLocalName());
        // Cleanup code can be added here

    }

    private class ClassifyFiles extends OneShotBehaviour {
        @Override
        public void action() {
            // Logic for classifying files goes here
            LOGGER.info("We are in the ClassifyFiles behaviour of ClassifierWorkerAgent.");
            // For now, just log the files
            scannedFiles.forEach(file -> LOGGER.info("File to classify: {}", file));

            myAgent.doDelete(); // shut down the agent as we don't need it anymore
        }
    }
    private void processMessageContent(String content) {
        // check if content is null or empty
        if (content == null || content.isEmpty()) {
            return;
        }

        String[] files = content.split(",");

        // add each file to scannedFiles set
        for(String file : files) {
            if(!file.trim().isEmpty()) {
                scannedFiles.add(file.trim());
            }
        }
        LOGGER.info("Processed scan request. Current scanned files: {}", scannedFiles.size());
    }

}
