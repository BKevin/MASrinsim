package mas.cbba.snapshot;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import mas.cbba.agent.AbstractConsensusAgent;

import java.util.Map;

/**
 * Timestamped snapshot and shallow copy of the state of the agent it is based upon.
 */
public abstract class Snapshot implements MessageContents{

    private final long timestamp;
    private final int agent;

    private Map<AbstractConsensusAgent, Long> communicationTimestamps;

    public Snapshot(AbstractConsensusAgent agent, Long time) {

        // Timestamp static builders are not public, we have to manage with long values.
        this.timestamp = time; //Timestamp.now(time.getTime());
        this.agent = agent.hashCode();
        this.communicationTimestamps = agent.getCommunicationTimestamps();
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Integer getAgentHash() {
        return agent;
    }


    /**
     * Snapshot can be equal when they are about the same agent with the same timestamps of
     * messages from other agents.
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Snapshot)){
            return false;
        }

        Snapshot sn = (Snapshot) o;

        return sn.getAgentHash().equals(this.getAgentHash());
            // Timestamps should not be checked for equality.
//                && sn.getCommunicationTimestamps().entrySet().containsAll(this.getCommunicationTimestamps().entrySet());
    }

    public Map<AbstractConsensusAgent, Long> getCommunicationTimestamps() {
        return communicationTimestamps;
    }


    @Override
    public String toString() {
        return ""+this.timestamp;
    }
}
