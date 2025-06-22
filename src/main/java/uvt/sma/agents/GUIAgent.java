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
import uvt.sma.helpers.MessageTemplate;

public class GUIAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private String sourceFolder;
    private String targetFolder;

    private Boolean deepScan = false; // TODO make this configurable from UI

    private DFAgentDescription[] sorters; // list of services
    private DFAgentDescription[] monitors; // list of services

    private static final Logger LOGGER = LogManager.getLogger(GUIAgent.class);

    @Override
    protected void setup() {
        LOGGER.info("GUI Agent {} is starting up.", getLocalName());

        // Initialize source and target folders
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            sourceFolder = (String) args[0];
            targetFolder = (String) args[1];
            LOGGER.info("Source folder: {}", sourceFolder);
            LOGGER.info("Target folder: {}", targetFolder);
        } else {
            LOGGER.error("No source or target folder provided. Please provide both as arguments.");
        }

        // add behaviours
        addBehaviour(new RegisterService());    // register service
        addBehaviour(new MessageListener());    // listen for messages
        addBehaviour(new ProvideFolders());
    }

    @Override
    protected void takeDown() {
        LOGGER.info("GUI Agent {} is shutting down.", getLocalName());
    }

    private class RegisterService extends OneShotBehaviour {
        @Override
        public void action() {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();

            sd.setType("gui-boss");
            sd.setName(getLocalName()+ "-gui-boss");

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
            LOGGER.info("GUI Agent {} is ready to receive messages.", getLocalName());
            ACLMessage msg = myAgent.receive();

            if(msg != null && msg.getConversationId().equals("confirm"))  {
                LOGGER.info("Received confirmation message from {}: {}", msg.getSender().getLocalName(), msg.getContent());
            } else {
                block();
            }
        }
    }
    private class ProvideFolders extends OneShotBehaviour {
        @Override
        public void action() {
            LOGGER.info("Providing source folder: {} and target folder: {}", sourceFolder, targetFolder);

            searchForServices();

            // 1st: send to sorter:
            MessageTemplate.sendMessage(
                    myAgent,
                    sorters[0].getName(),
                    ACLMessage.INFORM,
                    "set-folder",
                    "set-folder",
                    targetFolder
            );
            LOGGER.info("Sent target folder to sorter: {}", sorters[0].getName());
            // 2nd: send to monitor:
            MessageTemplate.sendMessage(
                    myAgent,
                    monitors[0].getName(),
                    ACLMessage.INFORM,
                    "set-folder",
                    "set-folder",
                    sourceFolder + "," + deepScan.toString()
            );
        }
    }

    private void searchForServices() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("sorting");
            dfd.addServices(sd);
            sorters = DFService.search(this, dfd);

            sd.setType("monitor");
            dfd.addServices(sd);
            monitors = DFService.search(this, dfd);

            LOGGER.info("Found {} sorting agents and {} monitor agents.", sorters.length, monitors.length);
        } catch (FIPAException e) {
            LOGGER.error("Failed to search for services: {}", e.getMessage());
        }
    }

}
