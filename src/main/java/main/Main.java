package main;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.View;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.LinkedList;
import java.util.List;

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
    private static final double VEHICLE_SPEED = 1000d;


    private static final int NUM_PARCELS = 1;
    private static final int SERVICE_DURATION = 10000; // in ms


    public static void main(String[] args) {
        run();
    }

    public static void run() {
        View.Builder viewBuilder = createGUI();

        final Simulator simulator = Simulator.builder()
                .addModel(RoadModelBuilders.plane()
                        .withMinPoint(MIN_POINT)
                        .withMaxPoint(MAX_POINT))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(viewBuilder)
                .build();


        final RandomGenerator rng = simulator.getRandomGenerator();

        //initializeDepot(simulator, rng);
        initializeVehicles(simulator, rng);
        initializeParcels(simulator, rng);
        
        configureParcelSpawn(simulator, rng);
        
        simulator.start();
    }


    private static void configureParcelSpawn(Simulator simulator, RandomGenerator rng) {
    }

    private static List<MyParcel> initializeParcels(Simulator simulator, RandomGenerator rng) {
        final RoadModel roadModel = simulator.getModelProvider().getModel(
                RoadModel.class);

        LinkedList<MyParcel> parcelList = new LinkedList<MyParcel>();

        for (int i = 0; i < NUM_PARCELS; i++) {
            MyParcel parcel = new MyParcel(
                    Parcel.builder(roadModel.getRandomPosition(rng),
                            roadModel.getRandomPosition(rng))
                            .serviceDuration(SERVICE_DURATION)
                            .neededCapacity(1)
                            .buildDTO());
            simulator.register(parcel);
            parcelList.add(parcel);
        }

        return parcelList;
    }

    private static void initializeDepot(Simulator simulator, RandomGenerator rng) {
        final RoadModel roadModel = simulator.getModelProvider().getModel(
                RoadModel.class);

        // add depots, taxis and parcels to simulator
        for (int i = 0; i < NUM_DEPOTS; i++) {
            simulator.register(new MyDepot(roadModel.getRandomPosition(rng),
                    DEPOT_CAPACITY));
        }
    }

    private static void initializeVehicles(Simulator simulator, RandomGenerator rng) {
        final RoadModel roadModel = simulator.getModelProvider().getModel(
                RoadModel.class);

        for (int i = 0; i < NUM_VEHICLES; i++) {
            simulator.register(new MyVehicle(roadModel.getRandomPosition(rng),
                    VEHICLE_CAPACITY,VEHICLE_SPEED));
        }
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
