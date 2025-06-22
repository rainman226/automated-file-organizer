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

    private Boolean deepScan;

    private DFAgentDescription[] sorters; // list of services
    private DFAgentDescription[] monitors; // list of services

    private static final Logger LOGGER = LogManager.getLogger(GUIAgent.class);

    @Override
    protected void setup() {
        LOGGER.info("GUI Agent {} is starting up.", getLocalName());

        // Initialize source and target folders
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            sourceFolder = (String) args[0];
            targetFolder = (String) args[1];
            deepScan = Boolean.parseBoolean((String) args[2]);
            LOGGER.info("Source folder: {}", sourceFolder);
            LOGGER.info("Target folder: {}", targetFolder);
            LOGGER.info("Deep scan option: {}", deepScan);
        } else {
            LOGGER.error("No source or target folder provided. Please provide both as arguments.");
        }

        // add behaviours
        addBehaviour(new RegisterService());    // register service
        addBehaviour(new MessageListener());    // listen for messages
        addBehaviour(new ProvideFolders());     // provide folders to sorters and monitors
    }

    @Override
    protected void takeDown() {
        LOGGER.info("GUI Agent {} is shutting down.", getLocalName());
    }

    /*
        * Registers the GUI service with the Directory Facilitator (DF).
        * The service type is "gui-boss" and the name is based on the agent's local name.
        * This allows other agents to discover this GUI service.
     */
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
            ACLMessage msg = myAgent.receive();

            if(msg != null && msg.getPerformative() == ACLMessage.CONFIRM) {    // CASE 1: confirmation message
                LOGGER.info("Received confirmation message from {}: {}", msg.getSender().getLocalName(), msg.getContent());
                System.out.println("Received confirmation message from " + msg.getSender().getLocalName() + ": " + msg.getContent());
            } else if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {   // CASE 2: inform message (empty folder)
                LOGGER.info("Received inform message from {}: {}", msg.getSender().getLocalName(), msg.getContent());
                System.out.println("Received inform message from " + msg.getSender().getLocalName() + ": " + msg.getContent());
            } else
                block();
            }
    }

    /*
        * Provides the source and target folders to the sorting and monitoring agents.
        * It sends the target folder to the first sorter and the source folder (with deep scan option) to the first monitor.
        * This is done after searching for available services in the Directory Facilitator (DF).
     */
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

    /*
        * Searches for sorting and monitoring services in the Directory Facilitator (DF).
        * It looks for services of type "sorting" and "monitor" and stores them in the respective arrays.
        * This allows the GUI agent to interact with available sorting and monitoring agents.
     */
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
