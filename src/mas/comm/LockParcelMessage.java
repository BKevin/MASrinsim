package mas.comm;

import com.github.rinde.rinsim.core.model.pdp.Parcel;

/**
 * Created by pieter on 03.06.16.
 */
public class LockParcelMessage extends ParcelMessage{
    public LockParcelMessage(Parcel parcel) {
        super(parcel);
    }
}
