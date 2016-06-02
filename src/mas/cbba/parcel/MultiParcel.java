package mas.cbba.parcel;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.google.common.collect.ImmutableList;
import mas.MyParcel;
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

    private Map<CbgaAgent, Long> allocations;

    public MultiParcel(ParcelDTO parcel, Integer requiredAgents) {
        super(parcel);

//        this.baseParcel = parcel;

        subParcels = generateParcels(parcel, requiredAgents);

        allocations = new HashMap<>();

    }

//    @Override
//    public void tick(TimeLapse timeLapse) {
//
//        if(isNotAllocatedEvenly()){
//            LoggerFactory.getLogger(this.getClass()).info("Triggered reallocation for {}: \nAllocations: {}", this, this.getAllocations());
//
//            triggerRerankOfAllocatedAgents();
//
//            LoggerFactory.getLogger(this.getClass()).info("Finished reallocation for {}:" +
//                    "\nAllocation: {}", this, this.getAllocations());
//        }
//        super.tick(timeLapse);
//    }
//
    /**
     * Trigger updateRoute in every CbgaAgent
     */
    private void triggerRerankOfAllocatedAgents() {
        for(Vehicle v : this.getAllocations().keySet()){
            CbgaAgent agent = ((CbgaAgent) v);

            //Trigger allocation onto this object for vehicles that are in the allocation list
            // TODO unencapsulated allocation changes!
            this.allocateTo(agent);

            agent.updateRoute();
        }
    }
//
//    /**
//     * Even allocation means every rank only occurs once.
//     * @return
//     */
//    public boolean isNotAllocatedEvenly() {
////        boolean hasDoubles = !((new HashSet<>(this.getAllocations().values())).size() == this.getAllocations().values().size());;
//
//        //maxSum ensures that every index occurs once.
//        boolean maxSum = this.getAllocations().values().stream().mapToInt(Integer::valueOf).sum() == countSum(this.getAllocations().size()-1);
//        return !maxSum;
//    }

    private int countSum(int size) {
        if(size <= 0)
            return 0;
        return countSum(size-1) + size;
    }

    public Map<CbgaAgent, Long> getAllocations() {
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
        return subParcels.size() + (this.isAvailable() ? 1 : 0);
    }

    public List<SubParcel> getSubParcels(){
        return ImmutableList.copyOf(this.subParcels);
    }

    /**
     * Remove a subparcel.
     * Triggers reranking of all allocated agents
     * @param parcel
     */
    public void removeSubParcel(Parcel parcel){
        this.subParcels.remove(parcel);
        triggerRerankOfAllocatedAgents();
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

        // Calculate rank
        this.setBidRank((CbgaAgent) vehicle);

        return getDelegateSubParcel(vehicle);

    }

    /**
     * The rank of this bid in relation to others.
     * @param p
     * @return
     */
    protected int getBidRank(CbgaAgent p) {
//        List<Long> sortedList = CbgaAgent.getValidBidsForParcel(p.getX(), this).stream().sorted().collect(Collectors.toList());

        return new ArrayList<>(new TreeSet(this.allocations.values())).indexOf(this.allocations.get(p));

    }

    protected void setBidRank(CbgaAgent p){
        this.allocations.put(p, p.getProjectedPickupTime(this));
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
    public Parcel getDelegateSubParcel(Vehicle v){

        int rank = this.getBidRank((CbgaAgent) v);

        if(rank == this.getRequiredAgents()-1 || rank >= subParcels.size()){
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
