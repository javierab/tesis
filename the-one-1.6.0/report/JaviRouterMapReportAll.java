/* 
 * Report for maps.
 */
package report;

import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.stream.Collectors;

import core.DTNHost;
import core.Message;
import core.Coord;
import core.MessageListener;
import core.UpdateListener;
import core.SimClock;
import core.ConnectionListener;
import routing.MessageRouter;
import routing.JaviRouter;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class JaviRouterMapReportAll extends Report implements UpdateListener {
	
	private NavigableMap<Double, Map<Integer, Coord>> globalMap;
	private NavigableMap<Double, Map<Integer, Coord>> globalRealMap;
	double lastUpdate;
	double granularity;
	/**
	 * Constructor.
	 */
	public JaviRouterMapReportAll() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.lastUpdate = 0.0;
		this.granularity = 100.0;
		this.globalMap = new TreeMap<Double, Map<Integer, Coord>>();
		this.globalRealMap = new TreeMap<Double, Map<Integer, Coord>>();
	}

	
	public void updated(List<DTNHost> hosts) {

		double simTime = getSimTime();
		if (simTime - lastUpdate < granularity )
			return;
		Map<Integer, Coord> realPos = new TreeMap<Integer, Coord>();
		for( DTNHost host : hosts){
			realPos.put(host.getAddress(), host.getLocation());
		}
		Map<Integer, Coord> currentMap = new TreeMap<Integer, Coord>();
		for( DTNHost host : hosts){
			JaviRouter r = (JaviRouter) host.getRouter();
			Map<Integer, Coord> global = r.globalmap.globalMap;
			boolean isStatic = r.staticNodes.containsKey(host.getAddress());
			//core.debug("node " + host.getAddress() + " static? " + isStatic);
			if( isStatic && global != null && global.size() > 0 ){
				for(Map.Entry<Integer, Coord> entry : global.entrySet()){
					if (currentMap.containsKey( entry.getKey() )){ //repeated key, use the closest
							if( currentMap.get( entry.getKey()).distance(realPos.get( entry.getKey() )) >
							entry.getValue().distance(realPos.get( entry.getKey() )) )
								currentMap.put(entry.getKey(), entry.getValue());
					}
					else{
						currentMap.put(entry.getKey(), entry.getValue());
					}
				}
			
			}
		}
		this.globalRealMap.put(simTime, realPos);
		this.globalMap.put(simTime, currentMap);
		lastUpdate = simTime;
	}

	
	@Override
	public void done() {
		write("Map all stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		
		for(Double d : this.globalMap.keySet() ){

		String stats = "Stats time " + d ;

			
			Map<Integer, Coord> myMap = this.globalMap.get(d);
			Map<Integer, Coord> realMap = this.globalRealMap.get(d);
			
			stats += "\n--Map size: " + myMap.size() + " of " + realMap.size();
			stats += "\n--Node % coverage " + ((double)myMap.size())*100.0/((double)realMap.size());
			
			//distances
			int n_200 = 0;
			List<Double> distData = new ArrayList<Double>();
			for(Integer node : myMap.keySet()){
				double dist = myMap.get(node).distance(realMap.get(node));
				if(dist < 200) n_200++;
				if(dist < 1000)
					distData.add( dist );
			}
			stats += "\n--NodesUnder200mError: " + n_200 + " - %: " + ((double)n_200)*100.0/((double)myMap.size());
			stats += "\n--AvgDistError: " + this.getAverage(distData);
			stats += "\n--MedianDistError: " + this.getMedian(distData);
			stats += "\n--VarianceDistError: " + this.getVariance(distData);
			
			write(stats);

		}

		super.done();
	}
	
}
