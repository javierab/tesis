/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.ArrayList;
import routing.MessageRouter;
import routing.JaviRouter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A message that is created at a node or passed between nodes.
 */
public class JMessage extends Message{
	private int area;
	private int priority;

	static {
		reset();
		DTNSim.registerForReset(Message.class.getCanonicalName());
	}
	
	/**
	 * Creates a new JMessage.
	 * @param from Who the message is (originally) from
	 * @param to Who the message is (originally) to
	 * @param id Message identifier (must be unique for message but
	 * @param area Message area of origin
	 * 	will be the same for all replicates of the message)
	 * @param size Size of the message (in bytes)
	 */
	public JMessage(DTNHost from, DTNHost to, String id, int size, int area){
		super(from, to, id, size);
		this.area = area;
	}
	
	public JMessage(Message m){
		super(m.getFrom(), m.getTo(), m.getId(), m.getSize());
		this.area = -1;
	}
	
	public JMessage(DTNHost from, DTNHost to, String id, int size){
		super(from, to, id, size);
		this.area = -1;
	}
	
	public void setArea(int area){
		this.area = area;
	}
	
	public int getArea() {
		return this.area;
	}
	
	public int getDestArea(){
		MessageRouter mr = this.getTo().getRouter();
		assert mr instanceof JaviRouter : "No javirouter!!!";
		JaviRouter jr = (JaviRouter) mr;
		return jr.currentArea;
		
	}

	/**
	 * Returns a replicate of this message (identical except for the unique id)
	 * @return A replicate of the message
	 */
	public JMessage replicate() {
		JMessage jm = new JMessage(this.getFrom(), this.getTo(), this.getId(), this.getSize());
		jm.copyFrom(this);
		jm.area = this.area;
		return jm;
	}
}