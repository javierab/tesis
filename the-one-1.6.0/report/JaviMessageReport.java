/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import core.DTNHost;
import core.Message;
import core.JMessage;
import routing.JaviRouter;
import core.MessageListener;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class JaviMessageReport extends Report implements MessageListener {
	Set<String> seenMessages;
	private double count;
	private double match;
	private double nomatch;
	private double noinfo;
	
	/**
	 * Constructor.
	 */
	public JaviMessageReport() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.seenMessages = new HashSet<String>();
		this.match = 0;
		this.nomatch = 0;
		this.noinfo = 0;
	}
	public void newMessage(Message m){
	}

	/**
	 * Method is called when a message's transfer is started
	 * @param m The message that is going to be transferred
	 * @param from Node where the message is transferred from 
	 * @param to Node where the message is transferred to
	 */
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to){
		if(seenMessages.contains(m.getId())) return;
		else{
		count+=1.0;
		seenMessages.add(m.getId());
		JMessage jm = (JMessage) m;
		JaviRouter r = (JaviRouter) jm.getFrom().getRouter();
		if(r.currentArea == -1 || jm.getArea() == -1) noinfo+=1.0;
		else if(r.currentArea > 0 && r.currentArea != jm.getArea()) nomatch+=1.0;
		else match+=1.0;
		}
	}
	
	/**
	 * Method is called when a message is deleted
	 * @param m The message that was deleted
	 * @param where The host where the message was deleted
	 * @param dropped True if the message was dropped, false if removed
	 */
	public void messageDeleted(Message m, DTNHost where, boolean dropped){}
	
	/**
	 * Method is called when a message's transfer was aborted before 
	 * it finished
	 * @param m The message that was being transferred
	 * @param from Node where the message was being transferred from 
	 * @param to Node where the message was being transferred to
	 */
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to){
		
	}


	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery){
	}

	@Override
	public void done() {
		write("JaviMessage stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		String statsText = "created: " + this.count + 
			"\nmatch %: " + (this.match/this.count) +
			"\nnomatch %: " + (this.nomatch/this.count) +
			"\nnoinfo %: " + (this.noinfo/this.count)
			;
		
		write(statsText);
		super.done();
	}
	
}
