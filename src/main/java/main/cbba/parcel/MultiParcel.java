package main.cbba.parcel;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.google.common.collect.ImmutableList;
import main.MyParcel;
import main.cbba.agent.AbstractConsensusAgent;
import main.cbba.agent.CbgaAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parcel that consists of n subtasks.
 *
 * Initialisation creates n-1 SubParcels
 */
public class MultiParcel extends MyParcel {

    //    ParcelDTO baseParcel;
    private List<SubParcel> subParcels;

    public MultiParcel(ParcelDTO parcel, Integer requiredAgents) {
        super(parcel);

//        this.baseParcel = parcel;

        subParcels = generateParcels(parcel, requiredAgents);

    }

    private List<SubParcel> generateParcels(ParcelDTO parcelDto, Integer subParcels){
        Builder builder = Parcel.builder(parcelDto);

        List<SubParcel> result = new ArrayList<SubParcel>();

        // Create first subparcel from the given DTO
        result.add(new SubParcel(parcelDto, this));
        // Generate n-1 other subparcels
        for(int i = 0; i < subParcels-1 ; i++) {
            SubParcel subparcel = new SubParcel(builder.buildDTO(), this);
            result.add(subparcel);
            //TODO we are creating extra object, should we register them?
//            this.getPDPModel().register(subparcel);
        }

        return result;
    }

    public Integer getRequiredAgents(){
        return subParcels.size();
    }

    public List<SubParcel> getSubParcels(){
        return ImmutableList.copyOf(this.subParcels);
    }

    /*
     * Allocation methods
     */

    /**
     * The algorithm guarantees this is only called when a vehicle is allocating to itself!
     * @param vehicle
     * @return
     */
    @Override
    public Parcel allocateTo(Vehicle vehicle) {
        if(vehicle instanceof CbgaAgent){
            CbgaAgent agent = (CbgaAgent) vehicle;

            // Get allocated agents according to the given vehicle.
            Set<AbstractConsensusAgent> agents = agent.getX().row(this)
                    .entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
                    .keySet();

            List<Vehicle> allocated = getAllocatedVehicles();

            // Difference between allocated and to-be assigned
            allocated.removeAll(agents);

            if(allocated.size() > 0){
                // The allocated agents loses the subparcel to the calling vehicle
                changeAllocation(allocated.get(0), vehicle);
            }
            else{
                //nothing happens otherwise, the allocation didn't really change.
            }
        }

        return changeAllocation(null, vehicle);
    }

    public Parcel changeAllocation(Vehicle from, Vehicle to){
        List<SubParcel> matches;
        if(from == null) {
            matches = subParcels.stream().filter((SubParcel s) -> !s.isAllocated()).collect(Collectors.<SubParcel>toList());
            if(matches.size() == 0){
                throw new IllegalArgumentException("All subparcels are allocated, you can only change allocations now.");
            }
        }
        else{
            matches = subParcels.stream().filter((SubParcel s) -> !s.getAllocatedVehicle().equals(from)).collect(Collectors.<SubParcel>toList());
        }

        /**
         * TODO consistency problem
         * Allocation cannot change after pickup, if some subparcel is not yet picked up and changes owner, but another
         * subparcel in the list is changed owner, then this allocation change can fail.
         */
        return matches.get(0).allocateTo(to);
    }

    public Vehicle getAllocatedVehicle(){
        throw new UnsupportedOperationException("MultiParcel is not allocated directly. Use getAllocatedVehicles instead.");
    }

    public List<Vehicle> getAllocatedVehicles(){
        return subParcels.stream().map(SubParcel::getAllocatedVehicle).collect(Collectors.toList());
    }

    public Parcel getAllocatedSubParcel(Vehicle vehicle){
        return subParcels.stream().filter((SubParcel s) -> s.getAllocatedVehicle() == vehicle).collect(Collectors.<Parcel>toList()).get(0);
    }

}
