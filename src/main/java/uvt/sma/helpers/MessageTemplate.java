package uvt.sma.helpers;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class MessageTemplate {
    public static void sendMessage(Agent sender, AID receiver, int performative, String ontology, String conversationId, String content) {
        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(receiver);
        msg.setOntology(ontology);
        msg.setConversationId(conversationId);
        msg.setContent(content);
        sender.send(msg);
    }
}
