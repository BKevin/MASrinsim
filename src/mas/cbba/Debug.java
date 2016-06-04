package mas.cbba;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import mas.cbba.agent.AbstractConsensusAgent;
import mas.cbba.agent.CbbaAgent;
import mas.cbba.agent.CbgaAgent;
import mas.cbba.snapshot.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pieter on 01.06.16.
 */
public class Debug {

    private static boolean construct =false;
    private static boolean consensus =false;
    private static boolean evalSnapshot = false;
    private static boolean sentSnapshot = false;
    private static boolean route = false;
    private static boolean reset = true;
    private static boolean parcelList = false;

    public static void logParcelListForAgent(CbbaAgent agent, Map<Parcel, PDPModel.ParcelState> states, Collection<Parcel> availableParcels){
        Logger logger = LoggerFactory.getLogger(agent.getClass());

        Set<Parcel> parcels = agent.getZ().keySet();

        logger.info("Parcels for {} (Parcels:{} - Available:{}):  {}",
                agent,
                parcels.size(),
                availableParcels.size(),
                agent.getZ());

        logger.info("Parcel states: {}", states);


    }

    public static void logParcelListForAgent(CbgaAgent agent, Map<Parcel, PDPModel.ParcelState> states, Collection<Parcel> availableParcels) {
        if(parcelList)
        {
            Logger logger = LoggerFactory.getLogger(agent.getClass());

            Set<Parcel> parcels = agent.getX().rowKeySet();

            logger.info("Parcels for {} (Parcels:{} - Available:{}):  \n{}",
                    agent,
                    parcels.size(),
                    availableParcels.size(),
                    agent.getX());

            logger.info("Parcel states: {}", states);
        }

    }

    public static void logRouteForAgent(AbstractConsensusAgent agent, Map<Parcel, PDPModel.ParcelState> states){
        if(!route)
            return;

        if(agent instanceof CbbaAgent){
            logRouteForAgent((CbbaAgent)agent, states);
        }
        else if(agent instanceof CbgaAgent) {
            logRouteForAgent((CbgaAgent) agent, states);
        }
    }

    public static void logRouteForAgent(CbbaAgent agent, Map<Parcel, PDPModel.ParcelState> states){
        Logger logger = LoggerFactory.getLogger(agent.getClass());

        logger.info("Route for {} (Parcels:{}) built from {}: \nRoute: {}",
                agent,
                agent.getZ().keySet().size(),
                agent.getZ().keySet(),
                agent.getRoute());

        logger.info("Parcel states: {}", states);


    }

    public static void logRouteForAgent(CbgaAgent agent, Map<Parcel, PDPModel.ParcelState> states){
        Logger logger = LoggerFactory.getLogger(agent.getClass());

        logger.info("Route for {} (Parcels:{}) built from {}: \nRoute: {}",
                agent,
                agent.getX().rowKeySet().size(),
                agent.getX().rowKeySet(),
                agent.getRoute());

        logger.info("Parcel states: {}", states);


    }

    public static void logConstructBundle(AbstractConsensusAgent agent){
        if(construct)
            LoggerFactory.getLogger(agent.getClass()).info("Do ConstructBundle {}", agent);
    }

    public static void logFindConsensus(AbstractConsensusAgent agent){
        if(consensus)
            LoggerFactory.getLogger(agent.getClass()).info("FindConsensus {}", agent);
    }

    public static void logEvaluateSnapshot(AbstractConsensusAgent agent){
        if(evalSnapshot)
            LoggerFactory.getLogger(agent.getClass()).info("Do evaluate snapshot of {}", agent);
    }


    public static void logSentSnapshot(AbstractConsensusAgent agent, Snapshot snapshot) {
        if(sentSnapshot)
            LoggerFactory.getLogger(agent.getClass()).info("Sent snapshot from {},  {}", agent, snapshot);
    }

    public static void logResetHeThinksMeIThinkHim(AbstractConsensusAgent me, AbstractConsensusAgent sender) {
        if(reset)
            LoggerFactory.getLogger(me.getClass()).info("Reset HeThinksMeIThinkHim : {},  {}", me, sender);
    }

    public static void logResetHeThinksOtherIThinkHim(AbstractConsensusAgent me, AbstractConsensusAgent sender, AbstractConsensusAgent otherIdea) {
        if(reset)
            LoggerFactory.getLogger(me.getClass()).info("Reset HeThinksOtherIThinkHim : {},  {}  and other: {}", me, sender, otherIdea );

    }
}
