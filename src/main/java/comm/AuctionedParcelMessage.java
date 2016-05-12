package comm;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import main.MyParcel;

/**
 * Created by KevinB on 8/05/2016.
 */
public class AuctionedParcelMessage implements MessageContents {
    private final MyParcel parcel;

    public AuctionedParcelMessage(MyParcel myParcel) {
        parcel = myParcel;
    }


}
