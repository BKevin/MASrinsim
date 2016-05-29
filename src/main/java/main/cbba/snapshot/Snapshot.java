package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.core.model.time.Timestamp;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;
import main.cbba.ConsensusAgent;
import main.cbga.CbgaAgent;
import main.route.evaluation.RouteEvaluation;

/**
 * Timestamped snapshot and shallow copy of the state of the agent it is based upon.
 */
public abstract class Snapshot implements MessageContents{

    private final long timestamp;

    private final int agent;
    public Snapshot(ConsensusAgent agent, TimeLapse time) {

        // Timestamp static builders are not public, we have to manage with long values.
        this.timestamp = time.getTime(); //Timestamp.now(time.getTime());
        this.agent = agent.hashCode();

    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Integer getAgentHash() {
        return agent;
    }


    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Snapshot)){
            return false;
        }

        Snapshot sn = (Snapshot) o;

        return sn.getTimestamp().equals(this.getTimestamp())
                && sn.getAgentHash().equals(this.getAgentHash());
    }

}
