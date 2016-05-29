package main.cbba;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import main.MyParcel;
import main.MyVehicle;
import main.route.evaluation.RouteEvaluation;
import main.route.evaluation.RouteTimes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by KevinB on 26/05/2016.
 */
public class CBBAVehicle extends MyVehicle implements ConsensusAgent {

    private LinkedList<MyParcel> b;
    private ArrayList<MyParcel> p;
    private Map<MyParcel, Long> y;
    private Map<MyParcel, CBBAVehicle> z;


    public CBBAVehicle(VehicleDTO vehicleDTO) {
        super(vehicleDTO);
    }

    @Override
    public void communicate() {
        //receive messages
            //New Tasks
            //Handle new tasks
            //Other CBBAVehicle info
                //Handle new info
        //If changes were made: rebroadcast own information
    }

    @Override
    public void constructBundle() {
        LinkedList<MyParcel> newB = getB();
        ArrayList<MyParcel> newP = getP();
        Map<MyParcel, Long> newY = getY();
        Map<MyParcel, CBBAVehicle> newZ = getZ();

        long currentPenalty = calculatePenalty(newP);

        boolean bIsChanging = true;
        while(bIsChanging){
            bIsChanging = false;

            long bestBid = Long.MAX_VALUE;
            MyParcel bestParcel = null;
            int bestPosition = -1;

            //look at all parcels
            for(MyParcel parcel : newZ.keySet()){
                //if you don't own the parcel yet, check it
                if(!newZ.get(parcel).equals(this)){

                    for(int pos = 0; pos <= newP.size(); pos++){
                        //calculate a bid for each position
                        long bid = calculatePenaltyAtPosition(newP,parcel,pos) - currentPenalty; //TODO aftrekken of optellen? (beter aftrek functie in penalty)
                        //check if bid is better than current best
                        if(isBetterBidThan(bid,newY.get(parcel))){
                            //check if bid is better than previous best
                            if(isBetterBidThan(bid, bestBid)){
                                //If better, save appropriate info
                                bestBid = bid;
                                bestParcel = parcel;
                                bestPosition = pos;
                            }
                        }
                    }
                }
            }
            if(bestParcel != null){
                newB.addLast(bestParcel);
                newP.add(bestPosition,bestParcel);
                newY.put(bestParcel,bestBid);
                newZ.put(bestParcel,this);
                bIsChanging = true;
            }
        }
    }


    private long calculatePenaltyAtPosition(ArrayList<MyParcel> path, MyParcel parcel, int positionOfParcel) {
        ArrayList<MyParcel> adaptedPath = new ArrayList<MyParcel>(path);
        adaptedPath.add(positionOfParcel,parcel);
        return calculatePenalty(adaptedPath);
    }

    private long calculatePenalty(ArrayList<MyParcel> path) {
        RouteTimes routeTimes = new RouteTimes(this,new ArrayList<Parcel>(path),this.getPosition().get(),this.getCurrentTime(),this.getCurrentTimeLapse().getTimeUnit());
        RouteEvaluation evaluation = new RouteEvaluation(routeTimes);
        return evaluation.getPenalty().getRoutePenalty();
    }

    @Override
    public void findConsensus() {

    }


    private boolean isBetterBidThan(double bid, double otherBid) {
        return bid < otherBid;
    }

    public LinkedList<MyParcel> getB() {
        return new LinkedList<MyParcel>(b);
    }

    public ArrayList<MyParcel> getP() {
        return new ArrayList<MyParcel>(p);
    }

    public Map<MyParcel, Long> getY() {
        return new HashMap<MyParcel, Long>(y);
    }

    public Map<MyParcel, CBBAVehicle> getZ() {
        return new HashMap<MyParcel, CBBAVehicle>(z);
    }
}
