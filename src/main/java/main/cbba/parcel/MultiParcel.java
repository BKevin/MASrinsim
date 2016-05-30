package main.cbba.parcel;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.google.common.collect.ImmutableList;
import main.MyParcel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parcel that consists of n subtasks.
 *
 * Initialisation creates n-1 SubParcels
 */
public class MultiParcel extends MyParcel {

//    ParcelDTO baseParcel;
    private List<SubParcel> subParcels;
    private Map<SubParcel, Vehicle> allocations;

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

    @Override
    public Parcel allocateTo(Vehicle vehicle) {
        return changeAllocation(null, vehicle);
    }

    public Parcel getAllocated(Vehicle vehicle){
        return subParcels.stream().filter((SubParcel s) -> s.getAllocatedVehicle() == vehicle).collect(Collectors.<Parcel>toList()).get(0);
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

        return matches.get(0).allocateTo(to);
    }

    public Vehicle getAllocatedVehicle(){
        throw new UnsupportedOperationException("MultiParcel is not allocated directly.");
    }

}
