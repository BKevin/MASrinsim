package mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.experiment.*;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;
import com.github.rinde.rinsim.pdptw.common.StatsTracker;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.StopCondition;
import com.github.rinde.rinsim.scenario.StopConditions;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.ui.View;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import mas.cbba.agent.AbstractConsensusAgent;
import mas.scenario.*;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by KevinB on 2/06/2016.
 */
public class RunExperiments {
    static final int amountOfExperiments = 10;


    static final Point MIN_POINT = new Point(0, 0);
    static final Point MAX_POINT = new Point(10, 10);

    static final String seperator = ",";
    private static int MAX_AGENTS = 10;

    public static void main(String[] args) {
        //args =  {CBBA,CBGA,BOTH} {Single,Multi,Mixed} distribution (optionalScenarioFile)
        //example
        //CBBA Single 1
        //CBBA Multi 0,0.5,0.5
        //CBGA Mixed 0.3,0.2,0.5
        boolean batch = true;
        if (batch) {
            batchExperiments();
            return;
        }
        else{
            if (args.length == 3) {
                runNewExperiments(MAX_AGENTS, 100, MAX_AGENTS, 0, 10, args);
                return;
            }
            if (args.length == 4) {
                runExperimentWithScenario(args);
                return;
            }
        }
        System.out.println("Arguments not correct, we need: {CBBA,CBGA,BOTH} {Single,Multi,Mixed} distribution (optionalScenarioFile)");
    }

    private static void batchExperiments(){
        String mode = "BOTH";
        String type;
        int amountExperiments = 2;
        //efficiency experiments
        //voor elke n=1..10 doe experiment met 10scenarios met CBBA en CBGA  (Single)
        for(int agents = 1; agents < 11; agents++){
            MAX_AGENTS = agents;
            long parcelInterArrival = 2500000/agents;
            type = "Single";
            String distribution = "1";
            int numParcels = 10;
            int numInitParcels = agents;

            System.out.println("----------- Experiment with " + agents + " agents --------------");
            runNewExperiments(agents,numParcels,numInitParcels,parcelInterArrival, amountExperiments,mode,type,distribution,"agent" + agents);
        }

        //Mixed
        //voor elke 1, 2 3 en 4 doe 10 scenores met CBBA en CBGA

    }

    private static void runExperimentWithScenario(String... args) {
        Experiment.Builder builder = Experiment.builder();
        Map<MASConfiguration, String> mapConfigs = new HashMap<>();
        builder = makeMASConfig(builder, mapConfigs, args);

        LinkedList<Scenario> scenarios = new LinkedList<>();
        Scenario scenario = makeScenario(args[3]);
        scenarios.add(scenario);
        builder.addScenario(scenario);

        ExperimentResults results = builder
                .usePostProcessor(PostProcessors.statisticsPostProcessor(new MyObjectiveFunction()))
                .usePostProcessor(new MyPostProcessor())
                .perform();

        for(Experiment.SimulationResult result : results.getResults()){
            System.out.println(result.getResultObject().toString());
        }

        putResultsInFile(results,scenarios,mapConfigs, "resources/experiments/singleExperiment/experiment_" + new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date()) +  ".txt");
    }

    private static void runNewExperiments(int agents, int numParcels, int numInitParcels, long parcelInterArrival, int amountExperiments, String... args) {



//        String mapName = "resources/experiments/experiment_" + new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date()) + "/";
        String mapName = "resources/experiments/experiment_" + args[3] + "/";

        String[] split = args[2].split(",");

        double[] distribution = new double[split.length];
        for(int i = 0; i < split.length; i++){
            distribution[i] = Double.parseDouble(split[i]);
        }


        //long parcelInterArrivalTime = Long.parseLong(args[3]);
//        long parcelInterArrivalTime = 250000; //3750000  //250000
        for(int i = 0; i < amountExperiments; i++){

            String filePath = mapName + "scene" + i + ".txt";
            ScenarioGenerator.generateScenario(filePath, agents, numParcels, numInitParcels, distribution, parcelInterArrival);
            System.out.println("Scene" + i + " is generated.");

//            parcelInterArrivalTime += 25000;
        }


        Experiment.Builder builder = Experiment.builder();
        Map<MASConfiguration, String> mapConfigs = new HashMap<>();
        builder = makeMASConfig(builder,mapConfigs,  args);

        LinkedList<Scenario> scenarios = new LinkedList<>();
        for(int i = 0; i < amountExperiments; i++){

            String filePath = mapName + "scene" + i + ".txt";
            Scenario scenario = makeScenario(filePath);
            scenarios.add(scenario);
            builder.addScenario(scenario);
        }

        System.out.println("Start Experiment.");
        ExperimentResults results = builder
                .usePostProcessor(PostProcessors.statisticsPostProcessor(new MyObjectiveFunction()))
                .usePostProcessor(new MyPostProcessor())
                .perform();

        System.out.println("Finish Experiment.");


//        for(Experiment.SimulationResult result : results.getResults()){
//            System.out.println(result.getResultObject().toString());
//        }

        putResultsInFile(results, scenarios, mapConfigs, mapName+"results.txt");


    }

    private static void putResultsInFile(ExperimentResults results, LinkedList<Scenario> scenarios, Map<MASConfiguration, String> mapConfigs, String... filenames) {

//        String filename = filenames.length > 0 ? filenames[0] : "result"
//                +"_"+new SimpleDateFormat("yyyy.MM.dd.HH.mm").format(new Date())
//                +".txt";
//
//        String path = "resources/results/" + filename;

        String path = filenames[0];

        File file = Paths.get(path).toFile();

        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter br = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file), "utf-8"))) {
            ImmutableSet<Experiment.SimulationResult> simResults = results.getResults();

            String header =
                    "MASConfig" + seperator
                            + "Scenario" + seperator
                            + "computationTime" + seperator
                            + "simulationTime" + seperator
                            + "numParcels" + seperator
                            + "numVehicles" + seperator
                            + "pickupTardiness" + seperator
                            + "deliveryTardiness" + seperator;
            for(int i = 0; i < MAX_AGENTS; i++){
                header += "messagesSentByByAgent" + i + seperator;
            }
            header += "totalMessagesSent" + seperator;
            for(int i = 0; i < MAX_AGENTS; i++){
                header += "messagesReceivedByAgent" + i + seperator;
            }
            header += "totalMessagesReceived" + seperator;
            for(int i = 0; i < MAX_AGENTS; i++){
                header += "idleTimeByAgent" + i + seperator;
            }
            for(int i = 0; i < MAX_AGENTS; i++){
                header += "averageAvailableParcelsByAgent" + i + seperator;
            }
            for(int i = 0; i < MAX_AGENTS; i++){
                header += "averageClaimedParcelsByAgent" + i + seperator;
            }
            for(int i = 0; i < MAX_AGENTS; i++){
                header += "routeCostCalculationsByAgent" + i + seperator;
            }
            header += "totalRouteCostCalculations" + seperator;
            for(int i = 0; i < MAX_AGENTS; i++){
                header += "constructBundleCalculationsByAgent" + i + seperator;
            }
            header += "totalConstructBundleCalculations";


            br.write(header);
            br.newLine();

            for(int i = 0; i < simResults.size(); i++){
                Experiment.SimulationResult simResult = (Experiment.SimulationResult) simResults.toArray()[i];
                MyResults myResults = (MyResults) simResult.getResultObject();

                Set<Vehicle> vehicleList = myResults.getVehicles();

                br.write(String.valueOf(mapConfigs.get(simResult.getSimArgs().getMasConfig())) + seperator);
                br.write(String.valueOf(scenarios.indexOf(simResult.getSimArgs().getScenario())) + seperator);
                br.write(String.valueOf(myResults.getStatistics().computationTime) + seperator);
                br.write(String.valueOf(myResults.getStatistics().simulationTime) + seperator);
                br.write(String.valueOf(myResults.getStatistics().acceptedParcels) + seperator);
                br.write(String.valueOf(myResults.getStatistics().movedVehicles) + seperator);
                br.write(String.valueOf(myResults.getStatistics().pickupTardiness) + seperator);
                br.write(String.valueOf(myResults.getStatistics().deliveryTardiness) + seperator);


                for(Vehicle v: vehicleList)
                    br.write(myResults.getMessagesSentByAgent().get(v) + seperator);
                setEmpty(br, vehicleList);

                br.write(myResults.getTotalAmountOfMessagesSentByAgents() + seperator);

                for(Vehicle v: vehicleList)
                    br.write(myResults.getMessagesReceivedByAgent().get(v) + seperator);
                setEmpty(br, vehicleList);

                br.write(myResults.getTotalAmountOfMessagesReceivedByAgents() + seperator);

                for(Vehicle v: vehicleList)
                    br.write(myResults.getIdleTimeByAgent().get(v) + seperator);
                setEmpty(br, vehicleList);

                for(Vehicle v: vehicleList)
                    br.write(myResults.getAverageAvailableParcel().get(v) + seperator);
                setEmpty(br, vehicleList);
                for(Vehicle v: vehicleList)
                    br.write(myResults.getAverageClaimedParcel().get(v) + seperator);
                setEmpty(br, vehicleList);


                for(Vehicle v: vehicleList)
                    br.write(myResults.getRouteCostCalculationsByAgent().get(v) + seperator);
                setEmpty(br, vehicleList);

                br.write(myResults.getTotalAmountOfRouteCostCalculationsByAgents() + seperator);

                for(Vehicle v: vehicleList)
                    br.write(myResults.getConstructBundleCallsByAgent().get(v) + seperator);
                setEmpty(br, vehicleList);

                br.write(myResults.getTotalConstructBundleCallsByAgents() + "");


                br.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void setEmpty(BufferedWriter br, Set<Vehicle> vehicleList) throws IOException {
        for(int k = vehicleList.size(); k < MAX_AGENTS; k++){
            br.write(seperator);
        }
    }

    private static Experiment.Builder makeMASConfig(Experiment.Builder builder, Map<MASConfiguration,String> map, String... args) {

        String vehicleMode = args[0];
        String parcelMode = args[1];
        if("CBBA".equals(vehicleMode) || "BOTH".equals(vehicleMode)){
            MASConfiguration.Builder config = MASConfiguration.builder()
                    .addModel(
                            PDPRoadModel.builder(
                                    RoadModelBuilders.plane()
                                            .withMinPoint(MIN_POINT)
                                            .withMaxPoint(MAX_POINT)
                                            .withMaxSpeed(10000d))
                                    .withAllowVehicleDiversion(true))
                    .addModel(DefaultPDPModel.builder())
                    .addModel(CommModel.builder())
                    .addEventHandler(NewDepotEvent.class, NewDepotEvent.defaultHandler());
            config = config.addEventHandler(NewVehicleEvent.class, NewVehicleEvent.cbbaHandler());

            if("Single".equals(parcelMode)){
                config = config.addEventHandler(NewParcelEvent.class, NewParcelEvent.defaultHandler());
            }
            if("Multi".equals(parcelMode)){
                config = config.addEventHandler(NewMultiParcelEvent.class, NewMultiParcelEvent.separateHandler());
            }
            if("Mixed".equals(parcelMode)){
                config = config.addEventHandler(NewParcelEvent.class, NewParcelEvent.defaultHandler());
                config = config.addEventHandler(NewMultiParcelEvent.class, NewMultiParcelEvent.separateHandler());
            }
            MASConfiguration build = config.build();
            builder.addConfiguration(build);
            map.put(build,"CBBA");
        }
        if("CBGA".equals(vehicleMode) || "BOTH".equals(vehicleMode)){
            MASConfiguration.Builder config = MASConfiguration.builder()
                    .addModel(
                            PDPRoadModel.builder(
                                    RoadModelBuilders.plane()
                                            .withMinPoint(MIN_POINT)
                                            .withMaxPoint(MAX_POINT)
                                            .withMaxSpeed(10000d))
                                    .withAllowVehicleDiversion(true))
                    .addModel(DefaultPDPModel.builder())
                    .addModel(CommModel.builder())
                    .addEventHandler(NewDepotEvent.class, NewDepotEvent.defaultHandler());
            config = config.addEventHandler(NewVehicleEvent.class, NewVehicleEvent.cbgaHandler());

            if("Single".equals(parcelMode)){
                config = config.addEventHandler(NewParcelEvent.class, NewParcelEvent.defaultHandler());
            }
            if("Multi".equals(parcelMode)){
                config = config.addEventHandler(NewMultiParcelEvent.class, NewMultiParcelEvent.defaultHandler());
            }
            if("Mixed".equals(parcelMode)){
                config = config.addEventHandler(NewParcelEvent.class, NewParcelEvent.defaultHandler());
                config = config.addEventHandler(NewMultiParcelEvent.class, NewMultiParcelEvent.defaultHandler());
            }
            MASConfiguration build = config.build();
            builder.addConfiguration(build);
            map.put(build, "CBGA");
        }


        return  builder;
    }

    private static Scenario makeScenario(String... filenames) {
        Scenario.Builder scenarioBuilder = Scenario.builder();

//
//        String filename = filenames.length > 0 ? filenames[0] : "scene.txt";
//
//        String path = "resources/scenario/" + filename;
        String path = filenames[0];
        File file = Paths.get(path).toFile();

        LoggerFactory.getLogger(Main.class).info("Reading from {} - {}", file.getPath(), file.canRead());
        long lastEventTime = -1;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                TimedEvent event = TimedEventFactory.makeTimedEventFromString(line);
                if(event != null) {
                    scenarioBuilder.addEvent(event);
                    lastEventTime = event.getTime();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StopCondition stopCondition= StopConditions.and(StopConditions.limitedTime(lastEventTime + 1), new NoParcelsStopCondition());
        scenarioBuilder.setStopCondition(stopCondition);

        return scenarioBuilder.build();
    }

    private static class MyObjectiveFunction implements ObjectiveFunction{

        @Override
        public boolean isValidResult(StatisticsDTO stats) {
            return true;
        }

        @Override
        public double computeCost(StatisticsDTO stats) {
            return 0;
        }

        @Override
        public String printHumanReadableFormat(StatisticsDTO stats) {
            return stats.toString();
        }
    }

    private static class MyPostProcessor implements PostProcessor<MyResults>{

        @Override
        public MyResults collectResults(Simulator sim, Experiment.SimArgs args) {
            MyResults result = new MyResults();
            result.setStatistics(extractStatistics(sim));
            result.setMessagesSentByAgent(extractMessagesSentByAgent(sim));
            result.setMessagesReceivedByAgent(extractMessagesReceivedByAgent(sim));
            result.setIdleTimeByAgent(IdleTimeByAgent(sim));
            result.setRouteCostCalculationsByAgent(extractRouteCostCalculationsByAgent(sim));
            result.setConstructBundleCallsByAgent(extractConstructBundleCallsByAgent(sim));
            result.setAverageAvailableParcel(extractAverageAvailableParcel(sim));
            result.setAverageClaimedParcel(extractAverageClaimedParcel(sim));

            return result;
        }

        private HashMap<Vehicle, Long> IdleTimeByAgent(Simulator sim) {

            HashMap<Vehicle, Long> map = new HashMap<>();

            final Set<Vehicle> vehicles = sim.getModelProvider()
                    .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);
            for(Vehicle vehicle : vehicles){
                AbstractConsensusAgent agent = (AbstractConsensusAgent) vehicle;
                map.put(vehicle, agent.getIdleTime());
            }

            return map;
        }


        private StatisticsDTO extractStatistics(Simulator sim) {
            final StatisticsDTO stats =
                    sim.getModelProvider().getModel(StatsTracker.class).getStatistics();
            return stats;
        }

        private HashMap<Vehicle, Integer> extractMessagesSentByAgent(Simulator sim) {

            HashMap<Vehicle, Integer> map = new HashMap<>();

            final Set<Vehicle> vehicles = sim.getModelProvider()
                    .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);
            for(Vehicle vehicle : vehicles){
                AbstractConsensusAgent agent = (AbstractConsensusAgent) vehicle;
                map.put(vehicle, agent.getNumberOfSentMessages());
            }

            return map;
        }

        private HashMap<Vehicle,Integer> extractMessagesReceivedByAgent(Simulator sim) {

            HashMap<Vehicle, Integer> map = new HashMap<>();

            final Set<Vehicle> vehicles = sim.getModelProvider()
                    .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);
            for(Vehicle vehicle : vehicles){
                AbstractConsensusAgent agent = (AbstractConsensusAgent) vehicle;
                map.put(vehicle, agent.getNumberOfReceivedMessages());
            }

            return map;
        }

        private HashMap<Vehicle,Integer> extractRouteCostCalculationsByAgent(Simulator sim) {

            HashMap<Vehicle, Integer> map = new HashMap<>();

            final Set<Vehicle> vehicles = sim.getModelProvider()
                    .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);
            for (Vehicle vehicle : vehicles) {
                AbstractConsensusAgent agent = (AbstractConsensusAgent) vehicle;
                map.put(vehicle, agent.getNumberOfRouteCostCalculations());
            }

            return map;
        }

        private HashMap<Vehicle, Integer> extractConstructBundleCallsByAgent(Simulator sim) {

            HashMap<Vehicle, Integer> map = new HashMap<>();

            final Set<Vehicle> vehicles = sim.getModelProvider()
                    .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);
            for (Vehicle vehicle : vehicles) {
                AbstractConsensusAgent agent = (AbstractConsensusAgent) vehicle;
                map.put(vehicle, agent.getNumberOfConstructBundleCalls());
            }

            return map;
        }

        private HashMap<Vehicle, Double> extractAverageAvailableParcel(Simulator sim) {

            HashMap<Vehicle, Double> map = new HashMap<>();

            final Set<Vehicle> vehicles = sim.getModelProvider()
                    .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);
            for (Vehicle vehicle : vehicles) {
                AbstractConsensusAgent agent = (AbstractConsensusAgent) vehicle;
                map.put(vehicle, agent.getAverageAvailableParcels());
            }

            return map;
        }

        private HashMap<Vehicle, Double> extractAverageClaimedParcel(Simulator sim) {

            HashMap<Vehicle, Double> map = new HashMap<>();

            final Set<Vehicle> vehicles = sim.getModelProvider()
                    .getModel(RoadModel.class).getObjectsOfType(Vehicle.class);
            for (Vehicle vehicle : vehicles) {
                AbstractConsensusAgent agent = (AbstractConsensusAgent) vehicle;
                map.put(vehicle, agent.getAverageClaimedParcels());
            }

            return map;
        }

        @Override
        public FailureStrategy handleFailure(Exception e, Simulator sim, Experiment.SimArgs args) {
            throw new IllegalStateException("Failure happened in the experiment:" + e);
        }
    }

    private static class MyResults {

        StatisticsDTO statistics;
        //amount of messages sent by each agent (Total & number/MessageType)
        HashMap<Vehicle,Integer> messagesSentByAgent;
        //amount of messages received by each agent (Total & number/MessageType)
        HashMap<Vehicle,Integer> messagesReceivedByAgent;
        //amount of constructBundle calls
        HashMap<Vehicle,Integer> constructBundleCallsByAgent;
        //amount of average available parcels
        HashMap<Vehicle,Double> averageAvailableParcel;
        //amount of average claimed parcels
        HashMap<Vehicle,Double> averageClaimedParcel;
        //amount of calculate bestRoute (total)
        HashMap<Vehicle,Integer> routeCostCalculationsByAgent;
        private HashMap<Vehicle, Long> idleTimeByAgent;

        public Set<Vehicle> getVehicles(){
            return messagesSentByAgent.keySet();
        }

        public void setMessagesSentByAgent(HashMap<Vehicle, Integer> messagesSentByAgent) {
            this.messagesSentByAgent = messagesSentByAgent;
        }

        public HashMap<Vehicle, Integer> getMessagesSentByAgent() {
            return messagesSentByAgent;
        }

        public int getTotalAmountOfMessagesSentByAgents(){
            int value = 0;
            for(int v : messagesSentByAgent.values())
                value += v;
            return value;
        }


        public StatisticsDTO getStatistics() {
            return statistics;
        }

        public void setStatistics(StatisticsDTO statistics) {
            this.statistics = statistics;
        }

        public HashMap<Vehicle, Integer> getMessagesReceivedByAgent() {
            return messagesReceivedByAgent;
        }

        public void setMessagesReceivedByAgent(HashMap<Vehicle, Integer> messagesReceivedByAgent) {
            this.messagesReceivedByAgent = messagesReceivedByAgent;
        }

        public int getTotalAmountOfMessagesReceivedByAgents(){
            int value = 0;
            for(int v : messagesReceivedByAgent.values())
                value += v;
            return value;
        }

        public HashMap<Vehicle, Integer> getRouteCostCalculationsByAgent() {
            return routeCostCalculationsByAgent;
        }

        public void setRouteCostCalculationsByAgent(HashMap<Vehicle, Integer> routeCostCalculationsByAgent) {
            this.routeCostCalculationsByAgent = routeCostCalculationsByAgent;
        }
        public int getTotalAmountOfRouteCostCalculationsByAgents(){
            int value = 0;
            for(int v : routeCostCalculationsByAgent.values())
                value += v;
            return value;
        }

        public HashMap<Vehicle, Double> getAverageAvailableParcel() {
            return averageAvailableParcel;
        }

        public void setAverageAvailableParcel(HashMap<Vehicle, Double> averageAvailableParcel) {
            this.averageAvailableParcel = averageAvailableParcel;
        }

        public HashMap<Vehicle, Double> getAverageClaimedParcel() {
            return averageClaimedParcel;
        }

        public void setAverageClaimedParcel(HashMap<Vehicle, Double> averageClaimedParcel) {
            this.averageClaimedParcel = averageClaimedParcel;
        }

        public HashMap<Vehicle, Integer> getConstructBundleCallsByAgent() {
            return constructBundleCallsByAgent;
        }

        public void setConstructBundleCallsByAgent(HashMap<Vehicle, Integer> constructBundleCallsByAgent) {
            this.constructBundleCallsByAgent = constructBundleCallsByAgent;
        }


        public int getTotalConstructBundleCallsByAgents() {
            int value = 0;
            for(int v : constructBundleCallsByAgent.values())
                value += v;
            return value;
        }

        @Override
        public String toString() {
            return "stats: " + getStatistics().toString() + "\n"
                    + "messagesSentByAgent: " + getMessagesSentByAgent() + "\n"
                    + "TotalAmountOfMessagesSentByAgents: " + getTotalAmountOfMessagesSentByAgents() + "\n"
                    + "messagesReceivedByAgent: " + getMessagesReceivedByAgent() + "\n"
                    + "TotalAmountOfMessagesReceivedByAgents: " + getTotalAmountOfMessagesReceivedByAgents() + "\n"
                    + "routeCostCalculationsByAgent: " + getRouteCostCalculationsByAgent() + "\n"
                    + "TotalAmountOfRouteCostCalculationsByAgents: " + getTotalAmountOfRouteCostCalculationsByAgents() + "\n"
                    + "constructBundleCallsByAgent: " + getConstructBundleCallsByAgent() + "\n"
                    + "averageAvailableParcel: " + getAverageAvailableParcel() + "\n"
                    + "averageClaimedParcel: " + getAverageClaimedParcel() + "\n";
        }

        public void setIdleTimeByAgent(HashMap<Vehicle,Long> idleTimeByAgent) {
            this.idleTimeByAgent = idleTimeByAgent;
        }

        public HashMap<Vehicle, Long> getIdleTimeByAgent() {
            return idleTimeByAgent;
        }
    }
}
