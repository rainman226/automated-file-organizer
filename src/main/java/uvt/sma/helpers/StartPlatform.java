package uvt.sma.helpers;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class StartPlatform {
    private static String sourceFolder;
    private static String targetFolder;
    private Boolean deepScan;

    public StartPlatform(String sourceFolder, String targetFolder, Boolean deepScan) {
        StartPlatform.sourceFolder = sourceFolder;
        StartPlatform.targetFolder = targetFolder;
        this.deepScan = deepScan;
    }

    public static void startPlatform() {
        // Additional initialization logic can be added here
        //
        jade.core.Runtime rt = jade.core.Runtime.instance();

        // === Main Container ===
        Profile pMain = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(pMain);

        // === Monitor Container ===
        Profile pMonitor = new ProfileImpl();
        pMonitor.setParameter(Profile.CONTAINER_NAME, "MonitorContainer");
        pMonitor.setParameter(Profile.MAIN_HOST, "localhost");
        AgentContainer monitorContainer = rt.createAgentContainer(pMonitor);

        // === Classifier Container ===
        Profile pClassifier = new ProfileImpl();
        pClassifier.setParameter(Profile.CONTAINER_NAME, "ClassifierContainer");
        pClassifier.setParameter(Profile.MAIN_HOST, "localhost");
        AgentContainer classifierContainer = rt.createAgentContainer(pClassifier);

        // === Sorter Container ===
        Profile pSorter = new ProfileImpl();
        pSorter.setParameter(Profile.CONTAINER_NAME, "SorterContainer");
        pSorter.setParameter(Profile.MAIN_HOST, "localhost");
        AgentContainer sorterContainer = rt.createAgentContainer(pSorter);
        // === Deploy Agents ===
        try {

            // should get the container for the classifier workers
            mainContainer.createNewAgent("manager", "uvt.sma.agents.ClassifierManager", null).start();

            // MonitorAgent
            Object[] argsMonitor = new Object[]{"C:\\Users\\Asus\\Desktop\\test", "false"};
            monitorContainer.createNewAgent("monitor", "uvt.sma.agents.MonitorAgent", argsMonitor).start();

            // SorterAgent
            // should get target folder from param
            sorterContainer.createNewAgent("sorter1", "uvt.sma.agents.SortingAgent", null).start();

            // GUI + ClassifierManager
            // GUI should get the file path from the user
            Object[] argsGui = new Object[]{sourceFolder, targetFolder};
            mainContainer.createNewAgent("gui", "uvt.sma.agents.GUIAgent", argsGui).start();

            // Tools
            mainContainer.createNewAgent("rma", "jade.tools.rma.rma", null).start();
            mainContainer.createNewAgent("sniffer", "jade.tools.sniffer.Sniffer", null).start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

}
