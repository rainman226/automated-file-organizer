package uvt.sma.agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ClassifierWorkerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private Set<String> scannedFiles = new java.util.HashSet<>();
    private DFAgentDescription[] sorters; // list of sorting services
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
        notifyManger();
    }

    private class SearchForSorters extends OneShotBehaviour {
        @Override
        public void action() {
            ServiceDescription sd = new ServiceDescription();
            sd.setType("sorting");

            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            try {
                sorters = DFService.search(myAgent, dfd);

                if(sorters.length > 0) {
                    LOGGER.info("Found {} sorting services.", sorters.length);

                } else {
                    LOGGER.warn("No sorting services found.");
                }
            } catch (FIPAException e) {
                LOGGER.error("Failed to search for sorting services: {}", e.getMessage());
            }
        }
    }


    private class ClassifyFiles extends OneShotBehaviour {
        @Override
        public void action() {
            Map<String, List<String>> fileCategories = new HashMap<>();

            for(String filePath : scannedFiles) {
                String extension = filePath.substring(filePath.lastIndexOf('.')).toLowerCase();
                String category = ClassifierManager.extensionMap.get(extension);

                // Initialize the category list if not present
                fileCategories.computeIfAbsent(category, k -> new ArrayList<>());

                // Add the file to the appropriate category
                fileCategories.get(category).add(filePath);
            }
            LOGGER.info("Classified {} files into {} categories.", scannedFiles.size(), fileCategories.size());
            for(Map.Entry<String, List<String>> entry : fileCategories.entrySet()) {
                String category = entry.getKey();
                List<String> filesInCategory = entry.getValue();

                LOGGER.info("Category: {}, Files: {}", category, filesInCategory.size());
            }
            myAgent.doDelete(); // shut down the agent as we don't need it anymore
        }
    }

    private void notifyManger() {
        try {
            // Notify the manager that this worker has finished processing
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new jade.core.AID("manager", jade.core.AID.ISLOCALNAME));
            msg.setConversationId("worker-finished-notice");
            msg.setSender(getAID());
            msg.setContent("Worker " + getLocalName() + " has finished processing files.");
            send(msg);
            LOGGER.info("Notification sent to manager about completion of work.");
        } catch (Exception e) {
            LOGGER.error("Failed to notify manager: {}", e.getMessage());
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
