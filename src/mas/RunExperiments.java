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
import mas.cbba.agent.AbstractConsensusAgent;
import mas.scenario.*;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by KevinB on 2/06/2016.
 */
public class RunExperiments {
    static final Point MIN_POINT = new Point(0, 0);
    static final Point MAX_POINT = new Point(10, 10);


    public static void main(String[] args) {
        runExperiments(args);
    }

    private static void runExperiments(String[] args) {

        MASConfiguration config = makeMASConfig();
//        Scenario scenario = makeScenario("testDistance.txt");;
        Scenario scenario = makeScenario("scene_2016.06.01.23.02.txt");;
        int numRepeats= 1;


        ExperimentResults results = Experiment.builder()
                .addConfiguration(config)
                .addScenario(scenario)
                .repeat(numRepeats)
                .usePostProcessor(PostProcessors.statisticsPostProcessor(new MyObjectiveFunction()))
                .usePostProcessor(new MyPostProcessor())
                .perform();

        for(Experiment.SimulationResult result : results.getResults()){
            System.out.println(result.getResultObject().toString());
        }


    }

    private static MASConfiguration makeMASConfig() {
        return MASConfiguration.builder()
                .addModel(
                        PDPRoadModel.builder(
                                RoadModelBuilders.plane()
                                        .withMinPoint(MIN_POINT)
                                        .withMaxPoint(MAX_POINT)
                                        .withMaxSpeed(10000d))
                                .withAllowVehicleDiversion(true))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addEventHandler(NewParcelEvent.class, NewParcelEvent.defaultHandler())
                .addEventHandler(NewVehicleEvent.class, NewVehicleEvent.defaultHandler())
                .addEventHandler(NewDepotEvent.class, NewDepotEvent.defaultHandler())
                .build();

    }

    private static Scenario makeScenario(String... filenames) {
        Scenario.Builder scenarioBuilder = Scenario.builder();


        String filename = filenames.length > 0 ? filenames[0] : "scene.txt";

        File file = Paths.get("resources/scenario/" + filename).toFile();

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
            result.setRouteCostCalculationsByAgent(extractRouteCostCalculationsByAgent(sim));
            result.setConstructBundleCallsByAgent(extractConstructBundleCallsByAgent(sim));
            result.setAverageAvailableParcel(extractAverageAvailableParcel(sim));
            result.setAverageClaimedParcel(extractAverageClaimedParcel(sim));

            return result;
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
    }
}
