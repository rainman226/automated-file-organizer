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

public class SortingAgent extends Agent {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LogManager.getLogger(SortingAgent.class);
    @Override
    protected void setup() {
        LOGGER.info("Sorting Agent {} is starting up.", getLocalName());

        // add behaviours
        addBehaviour(new RegisterService());
        addBehaviour(new MessageListener());
    }

    @Override
    protected void takeDown() {
        LOGGER.info("Sorting Agent {} is shutting down.", getLocalName());
    }

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
            if (msg != null && msg.getConversationId().equals("file-sorting-request")) {
                LOGGER.info("Received message from {} : {}" , msg.getSender().getLocalName() ,msg.getContent());
                //TODO add logic
            } else {
                block(); // Block until a new message arrives
            }
            //,,,
        }
    }
}
