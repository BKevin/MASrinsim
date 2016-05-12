package main.comm;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import main.MyVehicle;

/**
 * Created by KevinB on 8/05/2016.
 */
public class BidMessage implements MessageContents {

    private final int bidInformation;
    private final MyVehicle vehicle;

    public BidMessage(int bidInfo, MyVehicle myVehicle) {
        bidInformation = bidInfo;
        vehicle = myVehicle;

    }


    public int getBidInformation() {
        return bidInformation;
    }

    public MyVehicle getVehicle() {
        return vehicle;
    }
}
