package uvt.sma.agents;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import uvt.sma.helpers.MessageTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

public class MonitorAgent extends Agent {
    private Set<String> scannedFiles = new java.util.HashSet<>();
    private String directory;
    private Boolean deepScan;   // true for recursive scan, false for top-level only
    private DFAgentDescription[] result;    // list of services
    private static final Logger LOGGER = LogManager.getLogger(MonitorAgent.class);

    @Override
    protected void setup() {
        LOGGER.info("Monitor Agent " + getLocalName() + " is starting up.");

        // behaviour list
        addBehaviour(new RegisterService());  // register service
        addBehaviour(new MessageListener());
        //addBehaviour(new ScanFolder());     // MAKE TRIGGER
        //addBehaviour(new SendFileList());   // MAKE TRIGGER
    }

    @Override
    protected void takeDown() {
        System.out.println("Monitor Agent " + getLocalName() + " is shutting down.");
        // Cleanup code can be added here
    }

    private class RegisterService extends OneShotBehaviour {
        @Override
        public void action() {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();

            sd.setType("monitor");
            sd.setName(getLocalName()+ "-monitor-agent");

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

            // CASE 1: get the folder to monitor
            if (msg != null && msg.getConversationId().equals("set-folder")) {
                String content = msg.getContent();
                LOGGER.info("Received folder setting request: {}", content);

                // Process the message content
                String[] parts = content.split(",");
                if (parts.length >= 1) {
                    directory = parts[0];
                    deepScan = parts.length > 1 && Boolean.parseBoolean(parts[1]);
                    LOGGER.info("Directory set to: {}", directory);
                    LOGGER.info("Deep scan enabled: {}", deepScan);

                    MessageTemplate.sendMessage(
                            myAgent,
                            msg.getSender(),
                            ACLMessage.CONFIRM,
                            "folder-set",
                            "confirm",
                            "Directory set to: " + directory + ", Deep scan: " + deepScan
                    );

                    addBehaviour(new ScanFolder()); // Start scanning folder
                    addBehaviour(new SendFileList()); // Send file list after scanning
                } else {
                    LOGGER.warn("Invalid folder setting request format.");
                }
            } else if (msg != null && msg.getPerformative() == ACLMessage.CONFIRM) {    // CASE 2: confirmation message
                LOGGER.info("Received CONFIRM message from {}: {}", msg.getSender().getLocalName(), msg.getContent());
            } else {
                block(); // No message received, block until next message
            }
        }

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
                                String scannedFilePath = filePath.toString();
                                if (!scannedFiles.contains(scannedFilePath)) {
                                    LOGGER.info("[Deep scan] File detected: {}", scannedFilePath);
                                    scannedFiles.add(scannedFilePath);
                                }
                            });
                } else {
                    // Top-level only
                    Files.list(Paths.get(directory))
                            .filter(Files::isRegularFile)
                            .forEach(filePath -> {
                                String scannedFilePath = filePath.toString();
                                if (!scannedFiles.contains(scannedFilePath)) {
                                    LOGGER.info("[Shallow scan] File detected: {}", scannedFilePath);
                                    scannedFiles.add(scannedFilePath);
                                }
                            });
                }
            } catch (IOException e) {
                LOGGER.warn("Error scanning directory {}: {}", directory, e.getMessage());
            }
        }

    }

    private class SendFileList extends OneShotBehaviour {
        @Override
        public void action() {
            myAgent.doWait(2000); // Wait for 2 seconds before sending the file list

            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("classification-coordinator");
            dfd.addServices(sd);

            // TODO handle case when no service is available

            try {
                result = DFService.search(myAgent, dfd);

                if(result.length > 0) {
                    LOGGER.info("Found {} classification coordinator(s). Sending file list.", result.length);

//                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
//                    msg.setConversationId("file-list");
//                    msg.setContent(String.join(",", scannedFiles));
//                    msg.addReceiver(result[0].getName());
//
//                    myAgent.send(msg);

                    MessageTemplate.sendMessage(
                            myAgent,
                            result[0].getName(),
                            ACLMessage.INFORM,
                            "file-list",
                            "file-list",
                            String.join(",", scannedFiles)
                    );

                    LOGGER.info("File list sent to: {}", result[0].getName().getLocalName());
                } else {
                    LOGGER.warn("No classification coordinator found.");
                }
            } catch (FIPAException e) {
                LOGGER.error("Failed to send file list: {}", e.getMessage());
            }

        }
    }
}
