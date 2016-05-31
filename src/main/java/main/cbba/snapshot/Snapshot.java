package main.cbba.snapshot;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import main.cbba.agent.AbstractConsensusAgent;
import main.cbba.agent.AbstractConsensusAgent;

import java.util.Map;

/**
 * Timestamped snapshot and shallow copy of the state of the agent it is based upon.
 */
public abstract class Snapshot implements MessageContents{

    private final long timestamp;
    private final int agent;

    private Map<AbstractConsensusAgent, Long> communicationTimestamps;

    public Snapshot(AbstractConsensusAgent agent, TimeLapse time) {

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

    public Map<AbstractConsensusAgent, Long> getCommunicationTimestamps() {
        return communicationTimestamps;
    }


    @Override
    public String toString() {
        return ""+this.timestamp;
    }
}
