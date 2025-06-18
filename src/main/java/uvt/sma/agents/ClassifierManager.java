package uvt.sma.agents;

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

import java.util.Set;

public class ClassifierManager extends Agent {
    private static final long serialVersionUID = 1L;
    private Set<String> scannedFiles = new java.util.HashSet<>();
    private static final Logger LOGGER = LogManager.getLogger(ClassifierManager.class);
    @Override
    protected void setup() {
        LOGGER.info("Classifier Manager Agent {} is starting up.", getLocalName());

        // add behaviours
        addBehaviour(new RegisterService());
        addBehaviour(new AnswerScanRequest());
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

    private class AnswerScanRequest extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();

            if (msg != null && msg.getConversationId().equals("file-list")) {
                // process the message
                String content = msg.getContent();
                LOGGER.info("Received scan request from {}: {}", msg.getSender().getLocalName(), content);

                // process the message content and adds the files to scannedFiles set
                processMessageContent(content);

            } else {
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

}
