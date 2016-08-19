package mas.cbba.agent;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by pieter on 17.08.16.
 */
public class Deadlocked implements TickListener{

    private static Deadlocked singleton = new Deadlocked();

    public static Deadlocked getInstance(){
        return singleton;
    }

    private Map<Parcel, AbstractConsensusAgent> deadlocked_parcel = new HashMap<Parcel, AbstractConsensusAgent>();

    @Override
    public void tick(TimeLapse timeLapse) {

        release();

    }

    public boolean addParcel(Parcel p, AbstractConsensusAgent a){
        if(!deadlocked_parcel.containsKey(p)) {
            deadlocked_parcel.put(p, a);
            a.getUnallocatable().add(p);
            a.removeParcelAllocationFromYourself(p);
            return true;
        }
        return false;
    }
    private synchronized void release() {
        Iterator it = deadlocked_parcel.entrySet().iterator();
        while(it.hasNext()){
            if(ThreadLocalRandom.current().nextInt(0,5) == 0){
                Parcel p = (Parcel) ((Map.Entry)it.next()).getKey();
                deadlocked_parcel.get(p).getUnallocatable().remove(p);
                it.remove();
            };
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }
}
