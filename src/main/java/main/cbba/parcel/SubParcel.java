
package main.cbba.parcel;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import main.MyParcel;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Created by pieter on 30.05.16.
 */
public class SubParcel extends MyParcel {

    private final MultiParcel parent;

    public SubParcel(ParcelDTO parcelDTO, MultiParcel parent) {
        super(parcelDTO);
        this.parent = parent;

        // announcement is handled by parent MultiParcel
        setAnnounced();
    }

    public MultiParcel getParent(){
        return parent;
    }

//    @Override
//    public Parcel allocateTo(Vehicle vehicle) {
//         Allocation is handled by parent MultiParcel
//        throw new UnsupportedOperationException("Cannot allocate SubParcel directly, allocate via MultiParcel.");
//    }

}
