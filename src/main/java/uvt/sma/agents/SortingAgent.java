package uvt.sma.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uvt.sma.helpers.MessageTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class SortingAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private String targetFolder;
    private DFAgentDescription[] guiBosses; // list of GUI services

    private static final Logger LOGGER = LogManager.getLogger(SortingAgent.class);
    @Override
    protected void setup() {
        LOGGER.info("Sorting Agent {} is starting up.", getLocalName());

        // add behaviours
        addBehaviour(new RegisterService());    // register service
        addBehaviour(new MessageListener());    // listen for messages
    }

    @Override
    protected void takeDown() {
        LOGGER.info("Sorting Agent {} is shutting down.", getLocalName());
    }

    /*
        * Registers the sorting service with the Directory Facilitator (DF).
        * The service type is "sorting" and the name is based on the agent's local name.
        * This allows other agents to discover this sorting service.
     */
    private class RegisterService extends OneShotBehaviour {
        @Override
        public void action() {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();

            sd.setType("sorting");
            sd.setName(getLocalName()+ "-sorting-agent");

            dfd.addServices(sd);
            LOGGER.info("Registering service: {}", sd.getName());
            try{
                DFService.register(myAgent, dfd);
                LOGGER.info("Service registered successfully.");
            } catch (FIPAException e) {
                LOGGER.error("Failed to register service: {}", e.getMessage());
            }
        }
    }

    private class MessageListener extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();

            // CASE 1: Set target folder
            if(msg != null && msg.getConversationId().equals("set-folder")) {
                String content = msg.getContent();
                if (content != null && !content.isEmpty()) {
                    targetFolder = content;
                    LOGGER.info("Target folder set to: {}", targetFolder);

                    MessageTemplate.sendMessage(
                            myAgent,
                            msg.getSender(),
                            ACLMessage.CONFIRM,
                            "folder-set",
                            "confirm",
                            "Directory set to: " + targetFolder
                    );

                } else {
                    LOGGER.warn("Received empty folder path.");
                }
            }

            // CASE 2: File sorting request
            if (msg != null && msg.getConversationId().equals("file-sorting-request")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String content = msg.getContent();

                    Map<String, List<String>> fileMap = objectMapper.readValue(
                            content,
                            new TypeReference<Map<String, List<String>>>() {}
                    );

                    LOGGER.info("Received file sorting request with {} categories.", fileMap.size());

                    sortFiles(fileMap);

                    searchForGUIService();

                    MessageTemplate.sendMessage(
                            myAgent,
                            guiBosses[0].getName(),
                            ACLMessage.CONFIRM,
                            "files-sorted",
                            "confirm",
                            "Sucesfully sorted files into categories: " + fileMap.keySet().toString()
                    );


                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block(); // Block until a new message arrives
            }

        }

        private void searchForGUIService() {
            ServiceDescription sd = new ServiceDescription();
            sd.setType("gui-boss");

            DFAgentDescription dfd = new DFAgentDescription();
            dfd.addServices(sd);

            try {
                guiBosses = DFService.search(myAgent, dfd);

                if (guiBosses.length > 0) {
                    LOGGER.info("Found {} GUI services.", guiBosses.length);

                } else {
                    LOGGER.warn("No GUI services found.");
                }
            } catch (FIPAException e) {
                LOGGER.error("Failed to search for GUI services: {}", e.getMessage());
            }
        }
    }

    /*
        * Sorts files into categories based on the provided file map.
        * Each category corresponds to a folder in the target directory.
        * @param fileMap A map where keys are category names and values are lists of file paths.
     */
    private void sortFiles(Map<String, List<String>> fileMap) {
        LOGGER.info("Sorting files into categories...");
        Path targetPath = Paths.get(targetFolder);

        for (Map.Entry<String, List<String>> entry : fileMap.entrySet()) {
            String category = entry.getKey();
            List<String> files = entry.getValue();

            // sanitize category name before using it in a path
            String safeCategoryName = sanitizeCategoryName(category);
            Path categoryFolder;

            try {
                categoryFolder = targetPath.resolve(safeCategoryName);

                // create the category directory if it doesn't exist
                if (!Files.exists(categoryFolder)) {
                    Files.createDirectories(categoryFolder);
                }
            } catch (Exception e) {
                LOGGER.error("Error creating directory for category '{}', sanitized as '{}'. Exception: {}", category, safeCategoryName, e.toString());
                continue;
            }

            // move each file into the category folder
            for (String filePathStr : files) {
                Path sourcePath = Paths.get(filePathStr);
                Path targetFilePath = categoryFolder.resolve(sourcePath.getFileName());

                try {
                    Files.move(sourcePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Moved file '{}' to category '{}'.", sourcePath, category);
                } catch (IOException e) {
                    LOGGER.error("Failed to move file '{}' to '{}': {}", sourcePath, targetFilePath, e.toString());
                }
            }
        }
            //...
    }

    /*
        * Sanitizes the category name by replacing invalid characters with underscores.
        * This ensures that the category name can be used as a directory name.
        * @param category The original category name.
        * @return A sanitized version of the category name.
     */
    private String sanitizeCategoryName(String category) {
        return category.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

}

