package mas.cbba.parcel;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.collect.ImmutableList;
import com.sun.jna.WeakIdentityHashMap;
import mas.MyParcel;
import mas.cbba.agent.AbstractConsensusAgent;
import mas.cbba.agent.CbgaAgent;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Parcel that consists of n subtasks.
 *
 * Initialisation creates n-1 SubParcels
 */
public class MultiParcel extends MyParcel {

    //    ParcelDTO baseParcel;
    private List<SubParcel> subParcels;

    private Map<Vehicle, Integer> allocations;

    public MultiParcel(ParcelDTO parcel, Integer requiredAgents) {
        super(parcel);

//        this.baseParcel = parcel;

        subParcels = generateParcels(parcel, requiredAgents);

        allocations = new HashMap<>();

    }

    @Override
    public void tick(TimeLapse timeLapse) {

        if(isNotAllocatedEvenly()){
            LoggerFactory.getLogger(this.getClass()).info("Triggered reallocation for {}: \nAllocations: {}", this, this.getAllocations());

            triggerReEvaluation();

            LoggerFactory.getLogger(this.getClass()).info("Finished reallocation for {}:" +
                    "\nAllocation: {}", this, this.getAllocations());
        }
        super.tick(timeLapse);
    }

    /**
     * Trigger updateRoute in every CbgaAgent
     */
    private void triggerReEvaluation() {
        for(Vehicle v : this.getAllocations().keySet()){
            ((CbgaAgent) v).updateRoute();
        }
    }

    /**
     * Even allocation means every rank only occurs once.
     * @return
     */
    public boolean isNotAllocatedEvenly() {
        return !((new HashSet<>(this.getAllocations().values())).size() == this.getAllocations().values().size());
    }

    public Map<Vehicle, Integer> getAllocations() {
        return allocations;
    }

    private List<SubParcel> generateParcels(ParcelDTO parcelDto, Integer subParcels){
        Builder builder = Parcel.builder(parcelDto);

        List<SubParcel> result = new ArrayList<SubParcel>();

        // Generate n-1 other subparcels
        for(int i = 0; i < subParcels-1 ; i++) {
            SubParcel subparcel = new SubParcel(builder.buildDTO(), this);
            result.add(subparcel);
        }

        return result;
    }

    public Integer getRequiredAgents(){
        return 1 + (int) subParcels.stream().filter(p -> this.getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE, PDPModel.ParcelState.ANNOUNCED).contains(p)).count();
    }

    public List<SubParcel> getSubParcels(){
        return this.subParcels;
    }

    /*
     * Allocation methods
     */

    /**
     * The algorithm guarantees this is only called when a vehicle is allocating to itself!
     * @param vehicle
     * @return
     */
    public Parcel allocateTo(Vehicle vehicle) {

        Integer rank = this.getBidRank((CbgaAgent) vehicle);

        if(rank < 0){
            throw new IllegalArgumentException("Vehicle is not ranked in best bids.");
        }

        this.allocations.put(vehicle, rank);

        return rank < this.subParcels.size() ? this.subParcels.get(rank) : this;

//        if(vehicle instanceof CbgaAgent){
//            CbgaAgent agent = (CbgaAgent) vehicle;
//
//            // Get allocated agents according to the given vehicle.
//            Set<AbstractConsensusAgent> agents = agent.getX().row(this)
//                    .entrySet().stream()
//                    .filter(entry -> entry.getValue() > 0)
//                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
//                    .keySet();
//
//            List<Vehicle> allocated = getAllocatedVehicles();
//
//            // Difference between allocated and to-be assigned
//            allocated.removeAll(agents);
//
//            if(allocated.size() > 0){
//                // The allocated agents loses the subparcel to the calling vehicle
//                changeAllocation(allocated.get(0), vehicle);
//            }
//            else{
//                //nothing happens otherwise, the allocation didn't really change.
//            }
//        }
//
//        return changeAllocation(null, vehicle);
    }

    /**
     * The rank of this bid in relation to others.
     * @param p
     * @return
     */
    protected Integer getBidRank(CbgaAgent p) {
        List<Long> sortedList = p.getX().row(this).values().stream().filter(v -> v < Long.MAX_VALUE).sorted().collect(Collectors.toList());

        return sortedList.indexOf(p.getX().get(this, p));
    }

    @Override
    public boolean canBePickedUp(Vehicle v, long time) {
        return this.getAllocations().keySet().size() == this.getRequiredAgents();
    }
//    public Parcel changeAllocation(Vehicle from, Vehicle to){
//        List<SubParcel> matches;
//        if(from == null) {
//            matches = subParcels.stream().filter((SubParcel s) -> !s.isAllocated()).collect(Collectors.<SubParcel>toList());
//            if(matches.size() == 0){
//                throw new IllegalArgumentException("All subparcels are allocated, you can only change allocations now.");
//            }
//        }
//        else{
//            matches = subParcels.stream().filter((SubParcel s) -> !s.getAllocatedVehicle().equals(from)).collect(Collectors.<SubParcel>toList());
//        }
//
//        /**
//         * TODO consistency problem
//         * Allocation cannot change after pickup, if some subparcel is not yet picked up and changes owner, but another
//         * subparcel in the list is changed owner, then this allocation change can fail.
//         */
//        return matches.get(0).allocateTo(to);

//    }

    public Vehicle getAllocatedVehicle(){
        throw new UnsupportedOperationException("MultiParcel is not allocated directly. Use getAllocatedVehicles instead.");
    }

    public List<Vehicle> getAllocatedVehicles(){
        return subParcels.stream().map(SubParcel::getAllocatedVehicle).collect(Collectors.toList());
    }

    /**
     * Return the subparcel by determining your own rank in the bidding.
     * @return
     */
    public Parcel getAllocatedSubParcel(Vehicle v){
        Integer rank = this.getAllocations().get(v);
        if(this.getAllocations().get(v) == this.getRequiredAgents()-1){
            return this;
        }
        return subParcels.get(rank);
    }

    @Override
    public Parcel loseAllocation(Vehicle vehicle) {
        allocations.remove(vehicle);
        return null;
    }

}
