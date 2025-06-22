package uvt.sma.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uvt.sma.helpers.FileCategoryLoader;
import uvt.sma.helpers.MessageTemplate;

import java.util.HashMap;
import java.util.Set;

public class ClassifierManager extends Agent {
    private static final long serialVersionUID = 1L;
    private Set<String> scannedFiles = new java.util.HashSet<>();
    private int maxFiles = 100; // maximum number of files allowed for each worker
    public static final HashMap<String, String> extensionMap = FileCategoryLoader.loadExtensionCategoryMap("src/main/resources/extensions.csv");
    private static final Logger LOGGER = LogManager.getLogger(ClassifierManager.class);
    @Override
    protected void setup() {
        LOGGER.info("Classifier Manager Agent {} is starting up.", getLocalName());

        // add behaviours
        addBehaviour(new RegisterService());
        addBehaviour(new MessageListener());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            LOGGER.warn("Failed to deregister service: {}", e.getMessage());
        }

        LOGGER.info("Classifier Manager Agent {} is shutting down.", getLocalName());
    }

    private class RegisterService extends OneShotBehaviour {
        @Override
        public void action() {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();

            sd.setType("classification-coordinator");
            sd.setName(getLocalName()+ "-classification-coordinator");

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
            ACLMessage msg = receive();

            // CASE 1: get the file list from the monitor
            if (msg != null && msg.getConversationId().equals("file-list")) {
                // process the message
                String content = msg.getContent();
                LOGGER.info("Received scan request from {}: {}", msg.getSender().getLocalName(), content);

                // process the message content and adds the files to scannedFiles set
                processMessageContent(content);

                // confirm back to monitor
                MessageTemplate.sendMessage(
                        myAgent,
                        msg.getSender(),
                        ACLMessage.CONFIRM,
                        "scan-request-processed",
                        "confirm",
                        "Scan request processed. Number of files to classify: " + scannedFiles.size()
                );
                createWorkers();


            } else if (msg != null && msg.getConversationId().equals("worker-finished-notice")) {   // CASE 2: worker finished notice
                String receiverName = msg.getSender().getLocalName();
                String content = msg.getContent();
                LOGGER.info("Received worker finished notice from {}: {}", receiverName, content);

                // TODO: handle not correct termination notice
            } else if(msg != null && msg.getPerformative() == ACLMessage.CONFIRM) {     // CASE 3: confirmation message
                LOGGER.info("Received CONFIRM message from {}: {}", msg.getSender().getLocalName(), msg.getContent());
            } else {// CASE 3: confirmation message
                block(); // Wait for the next message
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

    private void createWorkers() {
        try {
            // TODO make it so it get the Wokers Container!
            ContainerController container = getContainerController(); // Get the container where this agent is running

            int workerCount = (int) Math.ceil((double) scannedFiles.size() / maxFiles); // based on the number of files and maxFiles
            LOGGER.info("Creating {} worker agents to process {} files.", workerCount, scannedFiles.size());

            // iterate through the number of workers and create them
            for(int i = 0; i < workerCount; i++) {
                StringBuilder filesForWorker = new StringBuilder();
                int filesAdded = 0;

                // add each file to the worker's list until maxFiles is reached
                for (String file : scannedFiles) {
                    if (filesAdded < maxFiles) {
                        if (filesForWorker.length() > 0) {
                            filesForWorker.append(",");
                        }
                        filesForWorker.append(file);
                        filesAdded++;
                    } else {
                        break;
                    }
                }
                // remove the files that were assigned to this worker from the scannedFiles set
                scannedFiles.removeAll(Set.of(filesForWorker.toString().split(",")));

                // create the worker agent with the files assigned
                Object[] args = new Object[]{filesForWorker.toString()};
                AgentController workerAgent = container.createNewAgent("worker-" + (i + 1), ClassifierWorkerAgent.class.getName(), args);
                workerAgent.start();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create worker agents: {}", e.getMessage());
        }
    }

    public static String getCategoryForFile(String ext) {
        return extensionMap.getOrDefault(ext.toLowerCase(), "Unknown");
    }

}
