package uvt.sma.controllers;

import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uvt.sma.helpers.StartPlatform;

import java.awt.*;

public class MainController {
    private static final Logger LOGGER = LogManager.getLogger(MainController.class);
    @FXML
    private Button sourceSelectButton;
    @FXML
    private Button targetSelectButton;
    @FXML
    private TextField sourceTextField;
    @FXML
    private TextField targetTextField;
    @FXML
    private Text infoText;
    @FXML
    private Text categoriesText;



//    @FXML
//    public void initialize() {
//        LOGGER.info("MainController initialized.");
//        // Additional initialization logic can be added here
//        //
//        jade.core.Runtime rt = jade.core.Runtime.instance();
//
//        // === Main Container ===
//        Profile pMain = new ProfileImpl();
//        AgentContainer mainContainer = rt.createMainContainer(pMain);
//
//        // === Monitor Container ===
//        Profile pMonitor = new ProfileImpl();
//        pMonitor.setParameter(Profile.CONTAINER_NAME, "MonitorContainer");
//        pMonitor.setParameter(Profile.MAIN_HOST, "localhost");
//        AgentContainer monitorContainer = rt.createAgentContainer(pMonitor);
//
//        // === Classifier Container ===
//        Profile pClassifier = new ProfileImpl();
//        pClassifier.setParameter(Profile.CONTAINER_NAME, "ClassifierContainer");
//        pClassifier.setParameter(Profile.MAIN_HOST, "localhost");
//        AgentContainer classifierContainer = rt.createAgentContainer(pClassifier);
//
//        // === Sorter Container ===
//        Profile pSorter = new ProfileImpl();
//        pSorter.setParameter(Profile.CONTAINER_NAME, "SorterContainer");
//        pSorter.setParameter(Profile.MAIN_HOST, "localhost");
//        AgentContainer sorterContainer = rt.createAgentContainer(pSorter);
//        // === Deploy Agents ===
//        try {
//            // Tools
//            mainContainer.createNewAgent("rma", "jade.tools.rma.rma", null).start();
//            mainContainer.createNewAgent("sniffer", "jade.tools.sniffer.Sniffer", null).start();
//
//            // GUI + ClassifierManager
//            mainContainer.createNewAgent("gui", "uvt.sma.agents.GUIAgent", null).start();
//            mainContainer.createNewAgent("manager", "uvt.sma.agents.ClassifierManager", null).start();
//
//            // MonitorAgent
//            Object[] argsMonitor = new Object[]{"C:\\Users\\Asus\\Desktop\\test", "false"};
//            monitorContainer.createNewAgent("monitor", "uvt.sma.agents.MonitorAgent", argsMonitor).start();
//
//            // SorterAgent
//            sorterContainer.createNewAgent("sorter1", "uvt.sma.agents.SortingAgent", null).start();
//
//
//        } catch (StaleProxyException e) {
//            e.printStackTrace();
//        }
//    }

        @FXML
        public void initialize() {
            // not the best way but it works so who cares
            StartPlatform platform = new StartPlatform("C:\\Users\\Asus\\Desktop\\test",
                    "C:\\Users\\Asus\\Desktop\\sorted",
                    false);

            platform.startPlatform();
        }
}
