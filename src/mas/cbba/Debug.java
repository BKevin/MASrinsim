package mas.cbba;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import mas.cbba.agent.AbstractConsensusAgent;
import mas.cbba.agent.CbbaAgent;
import mas.cbba.agent.CbgaAgent;
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

    public static void logParcelListForAgent(CbgaAgent agent, Map<Parcel, PDPModel.ParcelState> states, Collection<Parcel> availableParcels){
        Logger logger = LoggerFactory.getLogger(agent.getClass());

        Set<Parcel> parcels = agent.getX().rowKeySet();

        logger.info("Parcels for {} (Parcels:{} - Available:{}):  \n{}",
                agent,
                parcels.size(),
                availableParcels.size(),
                agent.getX());

        logger.info("Parcel states: {}", states);


    }

    public static void logRouteForAgent(AbstractConsensusAgent agent, Map<Parcel, PDPModel.ParcelState> states){
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
}