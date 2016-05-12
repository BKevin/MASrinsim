package comm;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.pdp.Parcel;

/**
 * Created by pieter on 12.05.16.
 */
public class ParcelMessage implements MessageContents{

    private final Parcel parcel;

    public ParcelMessage(Parcel parcel) {
        this.parcel = parcel;
    }

    public Parcel getParcel() {
        return parcel;
    }
}
