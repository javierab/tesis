/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.javi;

import java.lang.String;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import core.SimClock;
import core.DTNHost;


/**
 * Class for storing and manipulating the distances for Javi
 * router module.
 */
public class NodeDistancesSet {
	public static final int INFINITE_SET_SIZE = 1000;
	public static final double DEFAULT_TIMEOUT = 100;
	/** javi: distances, and time for known neighbors */
	public Map<Integer, Double> dist;
	private Map<Integer, Double> knownNeighbors;
	/** the time when this MPS was last updated */
	private double lastUpdateTime, oldestTime, timeout;
	private int oldestTimeNode, maxSetSize;

	public NodeDistancesSet(int maxSetSize) {
        if (maxSetSize == INFINITE_SET_SIZE || maxSetSize < 1) {
        	this.dist = new ConcurrentHashMap<Integer, Double>();
        	this.knownNeighbors = new ConcurrentHashMap<Integer, Double>();
        	this.maxSetSize = INFINITE_SET_SIZE;
        } else {
        	this.dist = new ConcurrentHashMap<Integer, Double>(maxSetSize);
        	this.knownNeighbors = new ConcurrentHashMap<Integer, Double>(maxSetSize);
            this.maxSetSize = maxSetSize;
        }
		this.lastUpdateTime = 0;
		this.timeout = DEFAULT_TIMEOUT;
		
	}
	
	public NodeDistancesSet() {
		this(INFINITE_SET_SIZE);
	}

	public double update(Integer index, Double d) {
		this.lastUpdateTime = SimClock.getTime();
		
		/*update*/
		dist.put(index, d);
		knownNeighbors.put(index,lastUpdateTime);
		
		/*check the times */
		if(this.lastUpdateTime > timeout){
			oldestTime = lastUpdateTime;
			clearNodes();
			timeout = lastUpdateTime + DEFAULT_TIMEOUT;
		}
		return timeout;

	}
	
	public void clearNodes(){
		/* check my map and drop values too old */
		//core.Debug.p("pre:" + knownNeighbors.size() + ", " + dist.size());
		this.knownNeighbors.forEach((k, v) -> { /*delete the values older than F*D_T old*/
			if( v < lastUpdateTime - DEFAULT_TIMEOUT ) {
				//core.Debug.p("removing v");
			    this.knownNeighbors.remove(k);
				this.dist.remove(k);
			}
			else if(v < this.oldestTime){
				//core.Debug.p("new oldest time:" + v);
				this.oldestTime = v;
			}
		});
		//core.Debug.p("post:" + knownNeighbors.size()  + ", " + dist.size());

	}
	
	public List<Integer> knownNeighbors(){
		return new ArrayList(this.knownNeighbors.keySet());
	}
	
	public NodeDistancesSet replicate() {
		NodeDistancesSet replica = new NodeDistancesSet(
				this.maxSetSize);		
		// do a deep copy
		for (Map.Entry<Integer, Double> e : dist.entrySet()) {
			replica.dist.put(e.getKey(), e.getValue().doubleValue());
		}

		for (Map.Entry<Integer, Double> e : knownNeighbors.entrySet()) {
			replica.knownNeighbors.put(e.getKey(), e.getValue().doubleValue());
		}
		
		replica.lastUpdateTime = this.lastUpdateTime;
		replica.oldestTime = this.oldestTime;
		replica.timeout = this.timeout;
		replica.oldestTimeNode = this.oldestTimeNode;
		return replica;
	}
		

	public double getDistFor(Integer index) {
		if (dist.containsKey(index)) {
			return dist.get(index);
		}
		else {
			return -1.0;
		}
	}

	public Map<Integer, Double> getDists() {
		return this.dist;
	}
	
	public double getLastUpdateTime() {
		return this.lastUpdateTime;
	}

    @Override
	public String toString() {
		return "dist: " +	this.dist.toString();
	}
}