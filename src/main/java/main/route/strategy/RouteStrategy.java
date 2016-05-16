package main.route.strategy;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import main.route.evaluation.Penalty;

import java.util.Collection;

/**
 * Root interface for route evaluation strategies.
 */
public interface RouteStrategy {

    public Penalty computeRoute(Collection<Parcel> parcels);

}
