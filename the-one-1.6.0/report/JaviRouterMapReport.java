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
public class JaviRouterMapReport extends Report implements ConnectionListener {
	private NavigableMap<Double, Map <Integer, Double>> distanceStatsByNode;
	private NavigableMap<Double, Map <Integer, Integer>> mapSizeByNode;
	private NavigableMap<Double, Map <Integer, Integer>> ct0_100ByNode;
	private NavigableMap<Double, Map <Integer, Integer>> ct100_200ByNode;
	private NavigableMap<Double, Map <Integer, Integer>> ct200ByNode;
	private NavigableMap<Double, Integer> nrofMaps;
	double lastTime;
	private Map<Integer, DTNHost> knownHosts;
	/**
	 * Constructor.
	 */
	public JaviRouterMapReport() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.distanceStatsByNode = new TreeMap<Double, Map<Integer, Double>>();
		this.mapSizeByNode = new TreeMap<Double, Map<Integer, Integer>>();
		this.ct0_100ByNode = new TreeMap<Double, Map<Integer, Integer>>();
		this.ct100_200ByNode = new TreeMap<Double, Map<Integer, Integer>>();
		this.ct200ByNode = new TreeMap<Double, Map<Integer, Integer>>();
		this.lastTime = 0;
		this.knownHosts = new HashMap<Integer, DTNHost>();
		this.nrofMaps = new TreeMap<Double, Integer>();
	}

	private void mapError(DTNHost d){
		JaviRouter r = (JaviRouter) d.getRouter();
		Map<Integer, Coord> global = r.globalmap.globalMap;
		if(global == null || global.size() < 3) return;
		this.knownHosts.putAll(r.seenHosts);
		if (global != null){
			double currentError = 0.0;
			int maxErrorNode = 0;
			double maxError = 0.0;
			int ct = 0;
			int ct_0_100 = 0;
			int ct_100_200 = 0;
			int ct_200 = 0;
			if(global.size() > 0){
				for(Map.Entry<Integer, Coord> pos : global.entrySet()){
					ct++;
					if(knownHosts.containsKey(pos.getKey())){
						currentError += pos.getValue().distance(knownHosts.get(pos.getKey()).getLocation());
						if(maxError < currentError){ 
							maxError = currentError;
							maxErrorNode = pos.getKey();
						}
						if(currentError > 0 && currentError <= 100) ct_0_100++;
						if(currentError > 100 && currentError <= 200) ct_100_200++;
						if(currentError > 200) ct_200++;
						
					}
				}
				//distance
				if(!distanceStatsByNode.containsKey(getSimTime())){
					Map <Integer, Double> hm = new HashMap<Integer, Double>();
					hm.put(d.getAddress(), currentError/global.size());
					distanceStatsByNode.put(getSimTime(), hm);
				}
				else{
					distanceStatsByNode.get(getSimTime()).put(d.getAddress(), currentError/global.size());
				}
				//size
				if(!mapSizeByNode.containsKey(getSimTime())){
					Map <Integer, Integer> hm = new HashMap<Integer, Integer>();
					hm.put(d.getAddress(), global.size());
					mapSizeByNode.put(getSimTime(), hm);
				}
				else{
					mapSizeByNode.get(getSimTime()).put(d.getAddress(), global.size());
				}
				//0-100
				if(!ct0_100ByNode.containsKey(getSimTime())){
					Map <Integer, Integer> hm = new HashMap<Integer, Integer>();
					hm.put(d.getAddress(), ct_0_100);
					ct0_100ByNode.put(getSimTime(), hm);
				}
				else{
					ct0_100ByNode.get(getSimTime()).put(d.getAddress(), ct_0_100);
				}	
				//100-200
				if(!ct100_200ByNode.containsKey(getSimTime())){
					Map <Integer, Integer> hm = new HashMap<Integer, Integer>();
					hm.put(d.getAddress(), ct_100_200);
					ct100_200ByNode.put(getSimTime(), hm);
				}
				else{
					ct100_200ByNode.get(getSimTime()).put(d.getAddress(), ct_100_200);
				}	
				//200+
				if(!ct200ByNode.containsKey(getSimTime())){
					Map <Integer, Integer> hm = new HashMap<Integer, Integer>();
					hm.put(d.getAddress(), ct_200);
					ct200ByNode.put(getSimTime(), hm);
				}
				else{
					ct200ByNode.get(getSimTime()).put(d.getAddress(), ct_200);
				}
				//increase number of maps
				if(!nrofMaps.containsKey(getSimTime())){
					nrofMaps.put(getSimTime(), 1);
				}
				else{
					int n = nrofMaps.get(getSimTime());
					nrofMaps.put(getSimTime(), n+1);
				}
				this.lastTime = getSimTime();
			}
		}
	}
	
	
	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2){
		mapError(host1);
		mapError(host2);
		return;
	}
	
	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2){
		mapError(host1);
		mapError(host2);

	}

	

	@Override
	public void done() {
		write("Map stats for scenario " + getScenarioName() + 
				"\nsim_time: " + format(getSimTime()));
		
		
		int nPeaks = 10;
		double peakInterval = this.lastTime/nPeaks;

		String stats = "nPeaks: " + nPeaks + "\npeakInterval:" + peakInterval + "\n";
		
		for(int i = 0; i < nPeaks-1; i++){
			
			//distances
			List<Double> sizesPerNode = new ArrayList<Double>();
			List<Integer> mapSizes = new ArrayList<Integer>();
			List<Integer> ct0_100 = new ArrayList<Integer>();
			List<Integer> ct100_200 = new ArrayList<Integer>();
			List<Integer> ct200 = new ArrayList<Integer>();
			List<Integer> nrof = new ArrayList<Integer>();

			
			for(Map<Integer, Double> m : distanceStatsByNode.subMap(i*peakInterval, (i+1)*peakInterval).values()){
				sizesPerNode.addAll(m.values());
				sizesPerNode.removeIf(Objects::isNull);
			}
			for(Map<Integer, Integer> m : mapSizeByNode.subMap(i*peakInterval, (i+1)*peakInterval).values()){
				mapSizes.addAll(m.values());
				mapSizes.removeIf(Objects::isNull);
			}
			for(Map<Integer, Integer> m : ct0_100ByNode.subMap(i*peakInterval, (i+1)*peakInterval).values()){
				ct0_100.addAll(m.values());
				ct0_100.removeIf(Objects::isNull);
			}
			for(Map<Integer, Integer> m : ct100_200ByNode.subMap(i*peakInterval, (i+1)*peakInterval).values()){
				ct100_200.addAll(m.values());
				ct100_200.removeIf(Objects::isNull);
			}
			for(Map<Integer, Integer> m : ct200ByNode.subMap(i*peakInterval, (i+1)*peakInterval).values()){
				ct200.addAll(m.values());
				ct200.removeIf(Objects::isNull);
			}
			for(Integer m : nrofMaps.subMap(i*peakInterval, (i+1)*peakInterval).values()){
				nrof.add(m);
				nrof.removeIf(Objects::isNull);
			}
			stats += "\n\nTime:" + (i*peakInterval) + " - " + ((i+1)*peakInterval);
			stats += "\n--AvgDistError: " + this.getAverage(sizesPerNode);
			stats += "\n--MedianDistError: " + this.getMedian(sizesPerNode);
			stats += "\n--VarianceDistError: " + this.getVariance(sizesPerNode);
			stats += "\n--AvgSize: " + this.getIntAverage(mapSizes);
			stats += "\n--AvgNrofMaps: " + this.getIntAverage(nrof);
			stats += "\n--MedianNrofMaps: " + this.getIntMedian(nrof);
			//stats += "\n--VarianceNrofMaps: " + this.getIntVariance(nrof);

			//stats += "\n--avgError 0-100: " + this.getIntAverage(ct0_100);
			//stats += "\n--avgError 100-200: " + this.getIntAverage(ct100_200);
			//stats += "\n--avgError 200+: " + this.getIntAverage(ct200);
			
		}
		write(stats);
		super.done();
	}
	
}
