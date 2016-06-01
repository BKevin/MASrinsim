package mas.route.evaluation.strategy;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import mas.MyParcel;
import mas.route.evaluation.RouteTimes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Route evaluation strategy based on timewindow violations
 */
public class TimeWindowViolationValue implements RouteValueStrategy {

    private Map<MyParcel, Long> violationMap;

    @Override
    public Long computeValue(RouteTimes times) {

        if(violationMap == null) {
            // cached penalty map
            buildViolationMap(times);
        }

        return getRoutePenalty();
    }

    private void buildViolationMap(RouteTimes times) {

        violationMap = new HashMap<>();

        for(MyParcel p : Arrays.asList(times.getPath().toArray(new MyParcel[0]))) {
            violationMap.put(p, p.computePenalty(times.getPickupTimes().get(p), times.getDeliveryTimes().get(p)));
        }
    }

    public Long getRoutePenalty(){

        Long penalty = 0L;

        for(Parcel p : this.violationMap.keySet()){
            penalty += violationMap.get(p);
        }

        return penalty;

    }

}
