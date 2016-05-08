package main;

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import comm.AcceptBidMessage;
import comm.AuctionedParcelMessage;
import comm.BidMessage;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyParcel extends Parcel implements CommUser, TickListener{

    private Optional<CommDevice> device;
    private boolean auctioned;
    private boolean broadcasted;
    private List<Message> bids;

    public MyParcel(ParcelDTO parcelDTO) {
        super(parcelDTO);
        auctioned = false;
        broadcasted = false;
        bids = null;
    }

    @Override
    public Optional<Point> getPosition() {
        return Optional.absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {

        device = Optional.of(commDeviceBuilder.build());
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (auctioned){
            return;
        }
        // if not yet broadcasted, broadcast
        if (!broadcasted){
            device.get().broadcast(new AuctionedParcelMessage(this));
            bids = new ArrayList<Message>();
            broadcasted = true;
            return;
        }
        //check incoming messages
        if (device.get().getUnreadCount() > 0) {
            ImmutableList<Message> messages = device.get().getUnreadMessages();
            for(Message message : messages){
                MessageContents contents = message.getContents();
                //if BidMessage then save
                if(contents instanceof BidMessage){
                    BidMessage bidContent = (BidMessage) contents;
                    bids.add(message);
                }
            }
        }

        if(bids.size() < this.getPDPModel().getVehicles().size()) {
            //wait until all bids are done
            return;
        }

        //handle ending auction:
        //find the best bid
        Message bestMess = bids.get(0);
        for (Message message : bids) {
            BidMessage bestBid = (BidMessage) bestMess.getContents();
            BidMessage bid = (BidMessage) message.getContents();
            if (false) { //TODO compare bids
                bestMess = message;
                throw new NotImplementedException();
            }
        }
        //send AcceptBidMessage to winner of auction
        device.get().send(new AcceptBidMessage(this), bestMess.getSender());
        //send RefuseBidMessage to losers
        for (Message message : bids) {

        }
        auctioned = true;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }
}
