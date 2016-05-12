package comm;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.pdp.Parcel;

/**
 * Created by KevinB on 8/05/2016.
 */
public class RefuseBidMessage implements MessageContents {

    private final Parcel parcel;

    RefuseBidMessage(Parcel parcel){
        this.parcel = parcel;
    }

    public Parcel getParcel(){
        return this.parcel;
    }

}
