package comm;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import main.MyParcel;

/**
 * Created by KevinB on 8/05/2016.
 */
public class AcceptBidMessage implements MessageContents {

    private final MyParcel parcel;

    public AcceptBidMessage(MyParcel myParcel) {
        parcel = myParcel;
    }
    public MyParcel getParcel() {
        return parcel;
    }
}
