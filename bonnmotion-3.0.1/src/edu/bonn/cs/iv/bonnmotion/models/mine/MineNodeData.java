
package edu.bonn.cs.iv.bonnmotion.models.mine;

import edu.bonn.cs.iv.bonnmotion.Position;


/** Mine node data: keeps a list of times and positions */

public class MineNodeData implements Comparable<MineNodeData>{
	public int node_id;
	public double time;
	public Position position;


	public MineNodeData(int node_id, double time, Position position) {
		this.node_id = node_id;
		this.time = time;
		this.position = position;
	}
	
	public String toString() {
		String s = "" + (int) time + " " + node_id + " " + position.x + " " + position.y;
		return s;
	}
	
	@Override
	public int compareTo(MineNodeData other){	
		if(this.time < other.time) return -1;
		else if (this.time > other.time) return 1;
		else if(other.time == this.time){
			if(this.node_id < other.node_id) return -1;
			else if(this.node_id > other.node_id) return 1;
			else return 0;
		}
		else return 0;			
	}
}
