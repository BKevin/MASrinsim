package mas.cbba.agent;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.google.common.collect.*;
import mas.MyParcel;
import mas.cbba.Debug;
import mas.cbba.parcel.MultiParcel;
import mas.cbba.parcel.SubParcel;
import mas.cbba.snapshot.CbgaSnapshot;
import mas.cbba.snapshot.Snapshot;

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
    private Set<MyParcel> unAllocatable;

    public CbgaAgent(VehicleDTO vehicleDTO) {
        super(vehicleDTO);

        this.X = HashBasedTable.create(
//                this.getPDPModel().getParcels(PDPModel.ParcelState.AVAILABLE).size(), //expected parcels
//                this.getPDPModel().getVehicles().size() //expected vehicles
        );

        this.unAllocatable = new HashSet<>();

    }

    /**
     * @return Immutable version of the winning bids array
     */
    public ImmutableTable<Parcel, AbstractConsensusAgent, Long> getX() {
        return ImmutableTable.copyOf(X);
    }

    @Override
    protected void replaceWinningBid(Parcel parcel, AbstractConsensusAgent from, AbstractConsensusAgent to, Long bid){
        this.X.put(parcel, from, this.NO_BID);
        this.setWinningBid(parcel, to, bid);
    }

    /**
     * Set winning bid value for the given Parcel and AbstractConsensusAgent
     * @param parcel
     * @param agent
     * @param bid
     */
    @Override
    protected void setWinningBid(Parcel parcel, AbstractConsensusAgent agent, Long bid){
        this.X.put(parcel, agent, bid);

        super.setWinningBid(parcel, agent, bid);
    }

    @Override
    protected void handleLostParcels(Parcel cause,List<Parcel> parcels) {
        super.handleLostParcels(cause,parcels);
        // remove bids of this Agent on the given parcels
        this.X.column(this).replaceAll(
                ((parcel, bid)
                        -> parcels.contains(parcel)
                        ? this.NO_BID
                        : bid));
    }

    @Override
    protected void addParcel(Parcel parcel) {
        // Initial case: no agent or parcels are in the table
        if(this.X.cellSet().isEmpty()){
            // Get all parcels in pdpmodel, get all agents in pdpmodel
            this.X.put(parcel, this, NO_BID);
        }
        else {
            // Addition to Table cause columnKeySet to be updated. This yields ConcurrentModificationExceptions.
            // Use an ImmutableList instead
            // http://code-o-matic.blogspot.be/2009/06/funny-concurrentmodificationexception.html
            for (final AbstractConsensusAgent agent : ImmutableList.copyOf(this.X.columnKeySet())) {
                this.X.put(parcel, agent, this.NO_BID);
            }
        }
    }

    @Override
    protected void removeParcel(Parcel p){
        MyParcel parcel = (MyParcel) p;
        if(parcel.getRequiredAgents() > 0){

            if(parcel instanceof SubParcel){
                parcel = ((SubParcel) parcel).getParent();
            }
            if(parcel instanceof MultiParcel &&
                    this.getPDPModel().getContents(this).contains(((MultiParcel) parcel).getAllocatedSubParcel(this))){
                this.X.put(parcel, this, NO_BID);
                ((MultiParcel) parcel).getSubParcels().remove(((MultiParcel) parcel).getAllocatedSubParcel(this));
                this.unAllocatable.add(parcel);
            }
        }
        else {
            for (AbstractConsensusAgent agent : ImmutableList.copyOf(this.X.columnKeySet())) {
                this.X.remove(parcel, agent);
            }
            this.unAllocatable.remove(parcel);
        }

        super.removeParcel(parcel);
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
//    }

    protected void addAgent(AbstractConsensusAgent k) {
        for(Parcel p : this.getX().rowKeySet()) {
            this.X.put(p, k, NO_BID);
        }
    }

    @Override
    public void constructBundle() {

        Set<Parcel> parcels = X.column(this).keySet();

        // Debugging
        Map<Parcel, PDPModel.ParcelState> states = parcels.stream().collect(Collectors.toMap(p -> p, p -> this.getPDPModel().getParcelState(p)));
        Collection<Parcel> availableParcels = this.getPDPModel().getParcels(PDPModel.ParcelState.ANNOUNCED, PDPModel.ParcelState.AVAILABLE).stream().filter(p -> !this.unAllocatable.contains(p)).collect(Collectors.toList());
        Debug.logParcelListForAgent(this, states, availableParcels);

        boolean bIsChanging = true;

        while(bIsChanging) {

            // Get all parcels not already in B
            List<Parcel> notInB = parcels.stream().filter(p -> !this.getB().contains(p)).collect(Collectors.toList());

            // Find best route values for every parcel currently not assigned to this vehicle
            Map<Parcel, Long> c_ij =
                    notInB.stream().collect(Collectors.toMap(Function.identity(), this::calculateBestRouteWith));

            // Get the best bid
            Stream<Map.Entry<Parcel, Long>> stream = c_ij.entrySet().stream()
                    // Remove all entries for which c_ij < max(y_ij)
                    .filter(entry ->
                            this.isBetterBidThan(
                                    // bid value of the entry
                                    entry.getValue(),
                                    // calculate the current maximum bid for every parcel not in B
                                    getHighestBid(entry.getKey())
                            )
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

                this.setWinningBid(bestEntry.getKey(), this, bestEntry.getValue());
            }
        }

    }

    /**
     * Highest bid: the worst bid if all the bids that are not MAXVALUE
     * (otherwise we'd just ask the lowest bid)
     * @param parcel
     * @return
     */
    private Long getHighestBid(Parcel parcel){
        Optional<Long> value = this.getX().row(parcel).values()
                .stream()
                .filter(p -> p < Long.MAX_VALUE)
                .min(Long::compareTo);

        return value.isPresent() ? value.get() : Long.MAX_VALUE;
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


            // (if) default number of agents required (==1)
            if(j.getRequiredAgents().equals(MyParcel.DEFAULT_REQUIRED_AGENTS)) {

                super.evaluateSnapshot(snapshot, k);

//                CbbaAgent thisAgent = new CbbaAgent(
//                        this,
//                        (CbgaSnapshot) this.generateSnapshot(),
//                        j
//                );

//                CbbaAgent other = new CbbaAgent(k, snapshot, j);
//
//                thisAgent.evaluateSnapshot(other.generateSnapshot(), other);
//
//                projectOntoX(thisAgent, other, k, j);
            }
            // (else) multiparcel
            else{

                Map<AbstractConsensusAgent, Long> timestamps = snapshot.getCommunicationTimestamps();


                // (for all m) m /= i
                for(AbstractConsensusAgent m : bids.row(j).keySet()){
                    if(m.equals(i))
                        continue;

                    // Agent i believes an assignment is taking place between agent m and task j
                    //(if) Xijm>0
                    if(!i.getX().get(j, m).equals(NO_BID) ){

                        // If K has newer information about assignment of task j to  M, update info.
                        //ORIGINAL (if) Skm > Sim (or) m = k
                        //CHANGED (if) m = k (or) Skm > Sim
                        if (m.equals(k) || i.getCommunicationTimestamps().get(m).compareTo(timestamps.get(m)) < 0 ) {

                            // Xijm = Xkjm
                            i.setWinningBid(j, m, bids.get(j, m));
                        }
                    }
                }
                // (for all m E A)
                for(AbstractConsensusAgent m : bids.row(j).keySet()){
                    //ORIGINAL (if) m /= i (and) Xijm > 0 (and) Xkjm >= 0
                    //CHANGED (if) m /= i (and) Xkjm > 0 (and) Xkjm /= Xijm
                    if(m.equals(i) || NO_BID.equals(snapshot.getWinningbids().get(j, m)) || i.getX().get(j, m).equals(snapshot.getWinningbids().get(j, m)))
                        continue;

                    // Number of agents assigned to J according to I
                    List<Long> bidsOnJ = i.getX().row(j).values().stream().filter((Long d) -> isBetterBidThan(d, this.NO_BID)).collect(Collectors.<Long>toList());

                    // There are less than the required number of agents assigned and m does a bid on j according to k
                    // (Sum of all N: Xijn > 0) < Qj
                    if(bidsOnJ.size() < j.getRequiredAgents()){
                        // Assign m to j
                        //Xijm = Xkjm
                        i.setWinningBid(j, m, bids.get(j, m));
                    }

                    // (Assumes the number of required agents is reached)
                    // Determine the maximum bid value and the associated agent N for task J
                    Optional<Long> minBid = bidsOnJ.stream().filter((Long l) -> l < Long.MAX_VALUE).max(Long::compareTo);

                    if(!minBid.isPresent() ) {
                        continue;
//                    throw new IllegalArgumentException("No minimum bid found in bid table.");
                    }

                    Long minimum = minBid.get();
                    AbstractConsensusAgent n = getAgentByLowestBid(minimum, p);

                    // If the maximum bid of N is higher than the bid of M for J, assign M instead of N
                    // (Min of all n: Xijn) > Xkjm
                    if(this.isBetterBidThan(bids.get(j, m), minimum)){
                        i.replaceWinningBid(j, n, m, bids.get(j, m));
                    }
                    // If the maximum bid of N is equal to the bid of M for J, the greatest ID (hashvalue) wins the assignment of J
                    else if(minimum.compareTo(bids.get(j, m)) == 0 && i.hashCode() > m.hashCode()){
                        i.replaceWinningBid(j, n, m, bids.get(j, m));
                    }

                }
            }


        }
    }

    private AbstractConsensusAgent getAgentByLowestBid(Long minBid, Parcel p) {
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

//    private void projectOntoX(CbbaAgent agent, CbbaAgent other, AbstractConsensusAgent realOther, Parcel j) {
//        if(agent.getZ().get(j).equals(agent)){
//            this.setWinningBid(j, this, agent.getY().get(j));
//        }
//        else if(agent.getZ().get(j).equals(other)) {
//            this.setWinningBid(j, realOther, agent.getY().get(j));
//        }
//        else {
//            this.setWinningBid(j, agent.getZ().get(j), agent.getY().get(j));
//        }
//    }

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

    @Override
    protected void allocateParcelToWinner(Parcel parcel, AbstractConsensusAgent agent) {

        if (this == agent) {
            // You get the allocation
            super.allocateParcelToWinner(parcel, agent);

        } else {
            Optional<Long> maximumBid = CbgaAgent.getValidBidsForParcel(this.getX(), parcel).stream().max(Long::compareTo);

            // The worst bid is still better than you, you lose
            if(maximumBid.isPresent() && this.isBetterBidThan(maximumBid.get(), this.getX().get(parcel, this))) {
                super.allocateParcelToWinner(parcel, agent);
            }
        }

    }

    /**
     * Get all bids not equal to NO_BID
     * @param x
     * @param p
     * @return
     */
    protected static List<Long> getValidBidsForParcel(ImmutableTable<Parcel, AbstractConsensusAgent, Long> x, Parcel p) {
        //TODO move to util

        return x.row(p).values().stream().filter(l -> !NO_BID.equals(l)).collect(Collectors.toList());
    }

}
