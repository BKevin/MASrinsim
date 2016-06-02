
package mas.cbba.parcel;

import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import mas.MyParcel;

/**
 * Created by pieter on 30.05.16.
 */
public class SubParcel extends MyParcel {

    private final MultiParcel parent;

    public SubParcel(ParcelDTO parcelDTO, MultiParcel parent) {
        super(parcelDTO);
        this.parent = parent;

        // announcement is handled by parent MultiParcel
        setAnnouncedArrival();
//        setAnnouncedSold();
    }

    public MultiParcel getParent(){
        return parent;
    }

    @Override
    public boolean canBePickedUp(Vehicle v, long time) {
        return parent.canBePickedUp(v, time);
    }

    @Override
    public boolean canBeDelivered(Vehicle v, long time) {
        return parent.canBeDelivered(v, time);
    }

    @Override
    public Integer getRequiredAgents() {
        return parent.getRequiredAgents();
    }

    //    @Override
//    public Parcel allocateTo(Vehicle vehicle) {
//         Allocation is handled by parent MultiParcel
//        throw new UnsupportedOperationException("Cannot allocate SubParcel directly, allocate via MultiParcel.");
//    }

}
