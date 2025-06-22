package uvt.sma.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.ControllerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uvt.sma.helpers.MessageTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClassifierWorkerAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private Set<String> scannedFiles = new java.util.HashSet<>();
    private DFAgentDescription[] sorters; // list of sorting services
    private static final Logger LOGGER = LogManager.getLogger(ClassifierWorkerAgent.class);
    @Override
    protected void setup() {

        try {
            LOGGER.info("Classifier Worker Agent {} is starting up in container {}.", getLocalName(), getContainerController().getContainerName());
        } catch (ControllerException e) {
            throw new RuntimeException(e);
        }
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            String files = (String) args[0];
            processMessageContent(files);

            LOGGER.info("Sucesfully processed scan request with {} files.", scannedFiles.size());

            addBehaviour(new SearchForSorters());
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

    /*
        * Searches for sorting services in the Directory Facilitator (DF).
        * This is a one-shot behaviour that runs once when the agent starts.
        * It looks for services of type "sorting" and stores them in the sorters array.
     */
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


    /*
        * Classifies the scanned files based on their extensions.
        * This is a one-shot behaviour that runs once after the files are scanned.
        * It categorizes files into different types and sends them to the sorter service.
     */
    private class ClassifyFiles extends OneShotBehaviour {
        @Override
        public void action() {
            Map<String, List<String>> fileCategories = new HashMap<>();

            for (String filePath : scannedFiles) {
                Path path = Paths.get(filePath);

                // Skip directories (just in case one slipped in)
                if (Files.isDirectory(path)) {
                    LOGGER.warn("Skipping directory: {}", filePath);
                    continue;
                }

                // Extract extension safely
                String extension;
                int dotIndex = filePath.lastIndexOf('.');
                int sepIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

                if (dotIndex == -1 || dotIndex < sepIndex || dotIndex == filePath.length() - 1) {
                    extension = ""; // No extension
                } else {
                    extension = filePath.substring(dotIndex).toLowerCase(); // includes the dot
                }

                // Get category or default to "Unknown"
                String category = ClassifierManager.getCategoryForFile(extension);
                if (category == null) {
                    category = "Unknown";
                }

                // Add file to the category list
                fileCategories.computeIfAbsent(category, k -> new ArrayList<>()).add(filePath);
            }

            LOGGER.info("Classified {} files into {} categories.", scannedFiles.size(), fileCategories.size());
            System.out.println("Categories:" + fileCategories.keySet());

            searchForSpecializedClassifiers(fileCategories);
            sendClassifiedFilesToSorter(fileCategories);
            myAgent.doDelete(); // shut down the agent as we don't need it anymore
        }

        private void sendClassifiedFilesToSorter(Map<String, List<String>> fileCategories) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonPayload = objectMapper.writeValueAsString(fileCategories);

                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(sorters[0].getName()); // Send to the first sorter found
                msg.setLanguage("JSON");
                msg.setOntology("FileClassificationOntology");
                msg.setConversationId("file-sorting-request");
                msg.setContent(jsonPayload);
                send(msg);

            } catch (Exception e) {
                LOGGER.warn("Failed to send classified files to sorter: {}", e.getMessage());
            }
        }

        /*
            * Searches for specialized classifiers in the Directory Facilitator (DF) based on file categories.
            * For each category, it looks for classifiers of type "classifier-category" and sends the file list to them.
            * This allows for specialized processing of files based on their category.
         */
        private void searchForSpecializedClassifiers(Map<String, List<String>> fileCategories) {
            DFAgentDescription[] specializedClassifiers;

            for(String category : fileCategories.keySet()) {
                ServiceDescription sd = new ServiceDescription();
                sd.setType("classifier-" + category.toLowerCase());
                sd.setName(category);

                DFAgentDescription dfd = new DFAgentDescription();
                dfd.addServices(sd);

                try {
                    specializedClassifiers = DFService.search(myAgent, dfd);
                    if(specializedClassifiers.length > 0) {
                        LOGGER.info("Found {} specialized classifiers for category: {}", specializedClassifiers.length, category);
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            String jsonPayload = objectMapper.writeValueAsString(fileCategories.get(category));

                            MessageTemplate.sendMessage(
                                    myAgent,
                                    specializedClassifiers[0].getName(),
                                    ACLMessage.REQUEST,
                                    "file-list",
                                    "file-list",
                                    jsonPayload
                            );

                            fileCategories.remove(category); // Remove category after sending to specialized classifier
                        } catch (Exception e) {
                            LOGGER.error("Failed to convert file list to JSON: {}", e.getMessage());
                        }

                    } else {
                        LOGGER.warn("No specialized classifiers found for category: {}", category);
                    }
                } catch (FIPAException e) {
                    LOGGER.error("Failed to search for specialized classifiers: {}", e.getMessage());
                }
            }

        }
    }

    /*
        * Notifies the manager agent that this worker has finished processing files.
        * This is called in the takeDown method to inform the manager about the completion of work.
        * It sends an INFORM message to the manager agent with the worker's name and status.
     */
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

    /*
        * Processes the content of the message received from the Manager.
     */
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
