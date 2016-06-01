package mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
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
import mas.scenario.*;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

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
        Scenario scenario = makeScenario("testDistance.txt");;
        int numRepeats= 2;


        ExperimentResults results = Experiment.builder()
                .addConfiguration(config)
                .addScenario(scenario)
                .repeat(numRepeats)
                .usePostProcessor(PostProcessors.statisticsPostProcessor(new MyObjectiveFunction()))
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
            return null;
        }

        @Override
        public FailureStrategy handleFailure(Exception e, Simulator sim, Experiment.SimArgs args) {
            throw new IllegalStateException("Failure happened in the experiment:" + e);
        }
    }

    private static class MyResults {
        //amount of messages sent by each agent (Total & number/MessageType)
        //amount of messages received by each agent (Total & number/MessageType)
        //amount of constructBundle calls
        //amount of average available parcels
        //amount of calculate bestRoute (total)


    }
}
