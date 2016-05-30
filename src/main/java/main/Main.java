package main;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.scenario.*;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.scenario.StopConditions;
import scenario.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by KevinB on 8/05/2016.
 */
public class Main {

    static final Point MIN_POINT = new Point(0, 0);
    static final Point MAX_POINT = new Point(10, 10);


    private static final int NUM_DEPOTS = 1;
    private static final int DEPOT_CAPACITY = 1;

    private static final int NUM_VEHICLES = 5;
    private static final int VEHICLE_CAPACITY = 1;
    private static final double VEHICLE_SPEED = 100d;


    private static final int NUM_PARCELS = 1;
    private static final int SERVICE_DURATION = 1000; // in ms
    private static final double NEW_PARCEL_PROB = 0.002;

    public static void main(String[] args) {
        runWithScenario();
    }

    public static void runWithScenario() {

        View.Builder viewBuilder = createGUI();

        int id = 0;
        Scenario myScenario = makeScenario(viewBuilder, id);


        Simulator.builder()
                .addModel(ScenarioController
                                .builder(myScenario)
                        .withEventHandler(NewParcelEvent.class, NewParcelEvent.defaultHandler())
                        .withEventHandler(NewParcelEvent.class, NewMultiParcelEvent.defaultHandler())
                        .withEventHandler(NewVehicleEvent.class, NewVehicleEvent.defaultHandler())
                        .withEventHandler(NewDepotEvent.class, NewDepotEvent.defaultHandler()))
                .build()
                .start();

    }


    private static Scenario makeScenario(View.Builder viewBuilder, int id) {
        Scenario.Builder scenarioBuilder = Scenario.builder();

        scenarioBuilder.
                addModel(PDPRoadModel.builder(
                        RoadModelBuilders.plane()
                                .withMinPoint(MIN_POINT)
                                .withMaxPoint(MAX_POINT)
                                .withMaxSpeed(10000d)))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(viewBuilder);

        File file = Paths.get("src\\main\\resources\\ThreeParcelScenario.txt").toFile();
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

        StopCondition stopCondition= StopConditions.and(StopConditions.limitedTime(lastEventTime+1),new NoParcelsStopCondition());
        scenarioBuilder.setStopCondition(stopCondition);

        return scenarioBuilder.build();
    }


    private static View.Builder createGUI() {
        View.Builder view = View.builder()
                .with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(
                                MyDepot.class, "/graphics/perspective/tall-building-64.png")
                        .withImageAssociation(
                                MyVehicle.class, "/graphics/flat/taxi-32.png")
                        .withImageAssociation(
                                MyParcel.class, "/graphics/flat/person-red-32.png"))
                .with(CommRenderer.builder()
                        .withMessageCount())
                .withTitleAppendix("MAS Project");

        return view;
    }
}
