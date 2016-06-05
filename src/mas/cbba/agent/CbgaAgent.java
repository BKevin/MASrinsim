package mas.cbba.agent;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import mas.MyParcel;
import mas.cbba.parcel.MultiParcel;
import mas.cbba.parcel.SubParcel;
import mas.cbba.snapshot.CbgaSnapshot;
import mas.cbba.snapshot.Snapshot;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by pieter on 26.05.16.
 */
public class CbgaAgent extends AbstractConsensusAgent {

    public static final Long NO_BID = Long.MAX_VALUE;

    /* m*n matrix with the winning bids of agents.
     * Xij is equal to the winning bid of agent i for task j or equal to  0 if no assignment has been made.
     */
    private Table<Parcel, AbstractConsensusAgent, Long> X; //sparse table

    public CbgaAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);

        this.X = HashBasedTable.create();

    }

    /**
     * @return Immutable version of the winning bids array
     */
    public ImmutableTable<Parcel, AbstractConsensusAgent, Long> getX() {
        return ImmutableTable.copyOf(X);
    }

    @Override
    protected void replaceWinningBid(Parcel parcel, AbstractConsensusAgent from, AbstractConsensusAgent to, Long bid){
        super.replaceWinningBid(parcel, from, to, bid);

        // Update route because after setting a winning bid, your rank may have changed.
        updateRoute();
    }

    /**
     * Set winning bid value for the given Parcel and AbstractConsensusAgent and change allocations
     * @param parcel
     * @param agent
     * @param bid
     */
    @Override
    protected void setWinningBid(Parcel parcel, AbstractConsensusAgent agent, Long bid){
        updateBidValue(parcel, agent, bid);

        super.setWinningBid(parcel, agent, bid);
    }

    @Override
    public void updateBidValue(Parcel j, AbstractConsensusAgent m, Long bid) {
        this.X.put(j, m, bid);
    }

    //    /**
//     * We handle MultiParcel too here, allocation is different than for Cbba single Parcels
//     * @param parcel
//     * @param agent
//     */
//    @Override
//    protected void allocateParcelToWinner(Parcel parcel, AbstractConsensusAgent agent) {
//        if (parcel instanceof MultiParcel) {
//
//            ((MultiParcel) parcel).allocateTo(agent);
//
//            //Find out who other allocated vehicles are
//        }
//        if (parcel instanceof SubParcel){
//            throw new IllegalArgumentException("Should not directly allocate parcels of type "+parcel.getClass().getName());
//        }
//        else if(parcel instanceof MyParcel){
//            ((MyParcel) parcel).allocateTo(agent);
//        }
//        else{
//            throw new IllegalArgumentException("Should not allocate parcels of type "+parcel.getClass().getName());
//        }

//    @Override
//    protected void handleLostParcels(Parcel cause,List<Parcel> parcels) {
//        super.handleLostParcels(cause,parcels);
//        // remove bids of this Agent on the given parcels
//        this.X.column(this).replaceAll(
//                ((parcel, bid)
//                        -> parcels.contains(parcel)
//                        ? this.NO_BID
//                        : bid));
//    }

    @Override
    protected void addParcelToBidList(Parcel parcel) {
        // Initial case: no agent or parcels are in the table
        if(this.getX().cellSet().isEmpty()){
            // Get all parcels in pdpmodel, get all agents in pdpmodel
            this.updateBidValue(parcel, this, NO_BID);
        }
        else {
            // Addition to Table cause columnKeySet to be updated. This yields ConcurrentModificationExceptions.
            // Use an ImmutableList instead
            // http://code-o-matic.blogspot.be/2009/06/funny-concurrentmodificationexception.html
            for (final AbstractConsensusAgent agent : ImmutableList.copyOf(this.getX().columnKeySet())) {
                updateBidValue(parcel,agent, this.NO_BID);
            }
        }
    }

    @Override
    protected void removeParcelFromBidList(Parcel p){
        MyParcel parcel = (MyParcel) p;
        if(parcel.getRequiredAgents() > 0){

            if(parcel instanceof SubParcel){
                parcel = ((SubParcel) parcel).getParent();
            }
            if(parcel instanceof MultiParcel
                    && this.getP().contains(parcel)
//                    && !this.unallocatable.contains(parcel)
                    && this.getPDPModel().getContents(this).contains(((MultiParcel) parcel).getDelegateSubParcel(this))
                    ){

                // Grab reference to subparcel
                Parcel subParcel = ((MultiParcel) parcel).getDelegateSubParcel(this);

                // Remove allocation from parent parcel
                parcel.loseAllocation(this);

                // Update bid
                updateBidValue(parcel, this, NO_BID);

                // Make unallocateble in the next round
                this.getUnallocatable().add(parcel);

                // Remove parcel from multiparcel
                ((MultiParcel) parcel).removeSubParcel(subParcel);

                super.removeParcelFromBidList(parcel);

            }
        }
        else {
            // Actual removing of parcel from bidlist
            for (AbstractConsensusAgent agent : ImmutableList.copyOf(this.X.columnKeySet())) {
                this.X.remove(parcel, agent);
            }
            this.getUnallocatable().remove(parcel);

            super.removeParcelFromBidList(parcel);
        }

    }

    protected void addAgent(AbstractConsensusAgent k) {
        for(Parcel p : this.getX().rowKeySet()) {
            updateBidValue(p, k, NO_BID);
        }
    }

    @Override
    public void constructBundle() {

        Set<Parcel> parcels = X.column(this).keySet();

        boolean bIsChanging = true;

//        // Debugging
//        Map<Parcel, PDPModel.ParcelState> states = parcels.stream().collect(Collectors.toMap(p -> p, p -> this.getPDPModel().getParcelState(p)));
//        List<Parcel> available = parcels
//                .stream()
//                .filter(p -> !this.getB().contains(p)
//                        && !this.getUnallocatable().contains(p)
//                        // FIXME should not use isAvailable here
//                        /**
//                         * We expect to know which parcels are available and which aren't based on communication
//                         */
//                        && ((MyParcel) p).isAvailable()
//                )
//                .collect(Collectors.toList());
//        Debug.logParcelListForAgent(this.getCurrentTime(), this, states, available);
        // /debugging

        while(bIsChanging) {

            // Get all parcels not already in B
            List<Parcel> notInB = parcels
                    .stream()
                    .filter(p -> !this.getB().contains(p)
                            && !this.getUnallocatable().contains(p)
                            // FIXME should not use isAvailable here
                            /**
                             * We expect to know which parcels are available and which aren't based on communication
                             */
                            && ((MyParcel) p).isAvailable()
                    )
                    .collect(Collectors.toList());


            List<Parcel> regularNotInB = parcels
                    .stream()
                    .filter(p -> !this.getB().contains(p)
                            && !this.getUnallocatable().contains(p)
                    )
                    .collect(Collectors.toList());

            if(!notInB.containsAll(regularNotInB)) {
                LoggerFactory.getLogger(this.getClass()).warn(
                        "notInB depends on isAvailable(). " +
                                "\n NotInB (with    isAvailable) ({}): {}" +
                                "\n NotInB (without isAvailable) ({}): {}",
                        notInB.size(),
                        notInB,
                        regularNotInB.size(),
                        regularNotInB
                );
            }

            // Find best route values for every parcel currently not assigned to this vehicle
            Map<Parcel, Long> c_ij =
                    notInB.stream().collect(Collectors.toMap(Function.identity(), this::calculateBestRouteWith));

            // Get the best bid
            Stream<Map.Entry<Parcel, Long>> stream = c_ij.entrySet().stream()
                    // Remove all entries for which c_ij < max(y_ij)
                    .filter(entry -> {
                                Optional<Long> bid = getHighestBidExcludingYourOwnBid(entry.getKey());
                                return this.isBetterBidThan(
                                        // bid value of the entry
                                        entry.getValue(),
                                        // calculate the current maximum bid for every parcel not in B
                                        bid.isPresent() ? bid.get() : Long.MAX_VALUE
                                );
                            }
                    );

            // Calculate the minimum argument in h_ij
            Optional<Map.Entry<Parcel, Long>> optBestEntry = stream.min(new Comparator<Map.Entry<? extends Parcel, Long>>() {
                @Override
                public int compare(Map.Entry<? extends Parcel, Long> parcelLongEntry, Map.Entry<? extends Parcel, Long> t1) {
                    return parcelLongEntry.getValue().compareTo(t1.getValue());
                }
            });

            if(bIsChanging = optBestEntry.isPresent()) {
                Map.Entry<? extends Parcel, Long> bestEntry = optBestEntry.get();

                this.getB().add(bestEntry.getKey());
                this.getP().add(this.calculateBestRouteIndexWith(bestEntry.getKey()), bestEntry.getKey());

                LoggerFactory.getLogger(this.getClass()).error("Table Pre: {}",
                        this.printTable(this.getX(),new StringBuilder(), true).toString());
                if(this.getValidBidsForParcel(this.getX(), bestEntry.getKey()).size() < ((MyParcel) bestEntry.getKey()).getRequiredAgents()) {
                    this.setWinningBid(bestEntry.getKey(), this, bestEntry.getValue());
                }else{
                    this.replaceWinningBid(bestEntry.getKey(),
                            getAgentByBid(this.getHighestBid(bestEntry.getKey()).get(), bestEntry.getKey()),
                            this, bestEntry.getValue());
                }


                LoggerFactory.getLogger(this.getClass()).error("Table Post: {}",
                        this.printTable(this.getX(),new StringBuilder(), true).toString());
                MyParcel j = (MyParcel) bestEntry.getKey();
                if(this.getValidBidsForParcel(this.getX(), j).size() > j.getRequiredAgents()){
                    LoggerFactory.getLogger(this.getClass()).warn(
                            "{} PostConstructBundleConsistencyCheck: {} has more bids for {} in Bidlist than required agents (bids: {} required:{}).",
                            this.getCurrentTime(),
                            this,
                            j,
                            this.getValidBidsForParcel(this.getX(), j).size(),
                            j.getRequiredAgents());
//                    LoggerFactory.getLogger(this.getClass()).error(this.dumpState());
//                    LoggerFactory.getLogger(this.getClass()).error("Snapshot Table: {}",
//                            this.printTable(snapshot.getWinningbids(),new StringBuilder(), true).toString());
//                    LoggerFactory.getLogger(this.getClass()).error(k.dumpState());
                }

            }
        }
    }

    private Optional<Long> getHighestBidExcludingYourOwnBid(Parcel parcel) {
        HashMap<AbstractConsensusAgent, Long> notMyBids = new HashMap<>(this.getX().row(parcel));
        notMyBids.remove(this);
        Optional<Long> value = notMyBids
                .values()
                .stream()
                .filter(p -> this.isValidBid(p))
                .max(Long::compareTo);

        return value;


    }

    /**
     * Highest bid: the worst bid if all the bids that are not MAXVALUE
     * (otherwise we'd just ask the lowest bid)
     * @param parcel
     * @return
     */
    private Optional<Long> getHighestBid(Parcel parcel){
        HashMap<AbstractConsensusAgent, Long> validBids = new HashMap<>(this.getX().row(parcel));
        Optional<Long> value = validBids
                .values()
                .stream()
                .filter(p -> this.isValidBid(p))
                .max(Long::compareTo);

        return value;
    }

    /**
     * Evaluate a single snapshot message from another sender
     *
     */
    public void evaluateSnapshot(Snapshot s, AbstractConsensusAgent k){
        if(!(s instanceof CbgaSnapshot)){
            throw new IllegalArgumentException("Snapshot does not have the right format. Expected CbgaSnapshot");
        }

        CbgaSnapshot snapshot = (CbgaSnapshot) s;

        ImmutableTable<Parcel, AbstractConsensusAgent, Long> bids = snapshot.getWinningbids();

        if(!this.getX().columnKeySet().contains(k)){
            addAgent(k);
        }

        // Convenience variable to adhere to the original algorithm
        CbgaAgent i = this;

        for(Parcel p : bids.rowKeySet()){
            MyParcel j = (MyParcel) p;

            //Consistency checks
            if(this.getValidBidsForParcel(this.getX(), j).size() > j.getRequiredAgents()){
                LoggerFactory.getLogger(this.getClass()).warn(
                        "{} PreEvaluationConsistencyCheck: {} has more bids for {} in Bidlist than required agents (bids: {} required:{}).",
                        this.getCurrentTime(),
                        this,
                        j,
                        this.getValidBidsForParcel(this.getX(), j).size(),
                        j.getRequiredAgents());
//                LoggerFactory.getLogger(this.getClass()).error(this.dumpState());
//                LoggerFactory.getLogger(this.getClass()).error("Snapshot Table: {}",
//                        this.printTable(snapshot.getWinningbids(),new StringBuilder(), true).toString());
//                LoggerFactory.getLogger(this.getClass()).error(k.dumpState());
            }

//            // (if) default number of agents required (==1)
//            if(!(j instanceof MultiParcel)) {
//
//                super.evaluateSnapshotForParcel(j, snapshot, k);
//            }
//
//            // (else) multiparcel
//            else{

            // Communication timestamps
            Map<AbstractConsensusAgent, Long> timestamps = snapshot.getCommunicationTimestamps();

            // Check all other agents, not yourself
            // (for all m) m /= i
            for(AbstractConsensusAgent m : bids.row(j).keySet()){
                if(m.equals(i))
                    continue;

                // Agent i believes an assignment is taking place between agent m and task j
                //(if) Xijm>0
                if(isValidBid(i.getX().get(j, m))){

                    // If K has newer information about assignment of task j to  M, update info.
                    // K has newer information about M if K IS M, or if its timestamp for M is greater than yours.
                    //ORIGINAL (if) Skm > Sim (or) m = k
                    //CHANGED (if) m = k (or) Skm > Sim
                    //Because timestamp is null for yourself and thus incomparable
                    if (m.equals(k) || !i.hasMoreRecentTimestampFor(m, timestamps.get(m))){
                        // Update the information in your table, because k has better info.
                        // Xijm = Xkjm
                        i.updateBidValue(j, m, bids.get(j, m));
                    }
                }
//                }
            }

            // Check all agents, not yourself, for better bids
            // (for all m E A)
            for(AbstractConsensusAgent m : bids.row(j).keySet()){

                //ORIGINAL (if) m /= i (and) Xijm > 0 (and) Xkjm >= 0
                //CHANGED (if) m /= i (and) Xkjm > 0 (and) (Xkjm /= Xijm (or) Xijm == 0 )
                if(m.equals(i)
                        || !this.isValidBid(snapshot.getWinningbids().get(j, m))
                        // i must not have an assignment for m on j yet
                        || this.isValidBid(i.getX().get(j, m))
                        ) {
                    continue;
                }

                // Number of agents assigned to J according to I
                List<Long> bidsOnJ = getValidBidsForParcel(i.getX(), j);

                // There are less than the required number of agents assigned and m does a bid on j according to k
                // (Sum of all N: Xijn > 0) < Qj
                if(bidsOnJ.size() < j.getRequiredAgents()){
                    // Assign m to j
                    //Xijm = Xkjm
                    i.setWinningBid(j, m, bids.get(j, m));
                }
                else{

                    // (Assumes the number of required agents is reached)
                    // Determine the maximum bid value and the associated agent N for task J
                    Optional<Long> worstBid = getHighestBid(j);


                    if (!worstBid.isPresent()) {
                        continue;
//                    throw new IllegalArgumentException("No minimum bid found in bid table.");
                    }

                    Long worstValue = worstBid.get();

                    AbstractConsensusAgent n = getAgentByBid(worstValue, p);

                    // If the maximum bid of N is higher than the bid of M for J, assign M instead of N
                    // (Max of all n: Xijn) > Xkjm
                    if (this.isBetterBidThan(bids.get(j, m), worstValue)) {
                        i.replaceWinningBid(j, n, m, bids.get(j, m));
                    }
                    // If the maximum bid of N is equal to the bid of M for J, the greatest ID (hashvalue) wins the assignment of J
                    else if (worstValue.equals(bids.get(j, m)) && n.hashCode() > m.hashCode()) {
                        i.replaceWinningBid(j, n, m, bids.get(j, m));
                    }
                }
            }
            //Consistency checks
            if(this.getValidBidsForParcel(this.getX(), j).size() > j.getRequiredAgents()){
                LoggerFactory.getLogger(this.getClass()).warn(
                        "{} PostEvaluationConsistencyCheck: {} has more bids for {} in Bidlist than required agents (bids: {} required:{}).",
                        this.getCurrentTime(),
                        this,
                        j,
                        this.getValidBidsForParcel(this.getX(), j).size(),
                        j.getRequiredAgents());
                LoggerFactory.getLogger(this.getClass()).error(this.dumpState());
                LoggerFactory.getLogger(this.getClass()).error("Snapshot Table: {}",
                        this.printTable(snapshot.getWinningbids(),new StringBuilder(), true).toString());
                LoggerFactory.getLogger(this.getClass()).error(k.dumpState());
            }
        }

    }

    private boolean isValidBid(Long aLong) {
        return ! NO_BID.equals(aLong);
    }

    private AbstractConsensusAgent getAgentByBid(Long minBid, Parcel p) {
        for(AbstractConsensusAgent a : this.getX().row(p).keySet()){
            if(minBid.equals(this.getX().get(p, a))){
                return a;
            };
        }
        return null;
    }

    @Override
    public Set<Parcel> getParcels() {
        return this.getX().rowKeySet();
    }

    @Override
    protected Snapshot generateSnapshot() {
        return new CbgaSnapshot(this, this.getCurrentTime());
    }

    @Override
    protected AbstractConsensusAgent getWinningAgentBy(Parcel parcel) {

        Long minBid = getWinningBidBy(parcel);

        for(AbstractConsensusAgent a : this.getX().row(parcel).keySet()){
            if(this.getX().get(parcel, a).equals(minBid)){
                return a;
            }
        }
        return null;
    }

    @Override
    protected Long getWinningBidBy(Parcel parcel) {
        Optional<Long> bid = this.getX().row(parcel).values().stream().min(Long::compareTo);
        return bid.isPresent() ? bid.get() : Long.MAX_VALUE;
    }

    /**
     * Get all bids not equal to NO_BID
     * @param x
     * @param p
     * @return
     */
    public static List<Long> getValidBidsForParcel(ImmutableTable<Parcel, AbstractConsensusAgent, Long> x, Parcel p) {
        //TODO move to util

        return x.row(p).values().stream().filter(l -> !NO_BID.equals(l)).collect(Collectors.toList());
    }

    public String dumpState(){
        StringBuilder builder = new StringBuilder();
        builder.append(this.getCurrentTime());
        builder.append(" State for ");
        builder.append(this);
//        builder.append(" at ")
        builder.append("\nBundle: ");
        builder.append(this.getB());
        builder.append("\nPath: ");
        builder.append(this.getP());
        builder.append("\nUnallocatable: ");
        builder.append(this.getUnallocatable());
        builder = printTable(this.getX(), builder, true);
        return builder.toString();
    }

    public StringBuilder printTable(ImmutableTable<Parcel, AbstractConsensusAgent, Long> x, StringBuilder builder, boolean validBids){
        builder.append("\n BidTable (");
        builder.append(validBids?"valid-only":"all");
        builder.append(")");
        for(Parcel p : x.rowKeySet()){
            builder.append("\nParcel: ");
            builder.append(p);
            builder.append("/");
            builder.append(((MyParcel) p).getRequiredAgents());
            builder.append(" Values: ");
            builder.append(validBids?this.getValidBidsForParcel(x, p):x.row(p).values());
//            builder.append();
//            builder.append()
        }
        return builder;
    }

}
