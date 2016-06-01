package mas;

import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.geom.Point;

/**
 * Created by KevinB on 8/05/2016.
 */
public class MyDepot extends Depot{

    public MyDepot(Point position, int depotCapacity) {
        super(position);
        setCapacity(depotCapacity);
    }

}
