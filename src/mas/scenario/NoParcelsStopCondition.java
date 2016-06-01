package mas.scenario;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.scenario.StopCondition;
import com.google.common.collect.ImmutableSet;

/**
 * Created by KevinB on 10/05/2016.
 */
public class NoParcelsStopCondition implements StopCondition {

    private final ImmutableSet<Class<?>> getTypes;

    public NoParcelsStopCondition() {
        this.getTypes = ImmutableSet.<Class<?>>of(PDPModel.class);
    }

    @Override
    public ImmutableSet<Class<?>> getTypes() {
        return getTypes;
    }

    @Override
    public boolean evaluate(TypeProvider provider) {
        PDPModel pdpModel = provider.get(PDPModel.class);
        return pdpModel.getParcels(
                PDPModel.ParcelState.ANNOUNCED,
                PDPModel.ParcelState.AVAILABLE,
                PDPModel.ParcelState.PICKING_UP,
                PDPModel.ParcelState.IN_CARGO,
                PDPModel.ParcelState.DELIVERING).isEmpty();
    }
}
