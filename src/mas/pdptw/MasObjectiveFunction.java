package mas.pdptw;

import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.pdptw.common.StatisticsDTO;

/**
 * Created by pieter on 09.05.16.
 */
public class MasObjectiveFunction implements ObjectiveFunction {


    @Override
    public boolean isValidResult(StatisticsDTO statisticsDTO) {
        return true;
    }

    @Override
    public double computeCost(StatisticsDTO statisticsDTO) {

        //linear penalty - easy

        return statisticsDTO.pickupTardiness + statisticsDTO.deliveryTardiness;


    }


    // Blijkbaar ook ergens een statspanel available?
    @Override
    public String printHumanReadableFormat(StatisticsDTO statisticsDTO) {

        return String.format(
                    "Statistics \n" +
                            "Total accumulated penalty: %s \n" +
                            "Total parcels delivered: %s \n" +
                            "Pickup tardiness: %s \n" +
                            "Delivery tardiness: %s \n" +
                            "",
                this.computeCost(statisticsDTO),
                statisticsDTO.totalDeliveries,
                statisticsDTO.pickupTardiness,
                statisticsDTO.deliveryTardiness
        );

    }
}
