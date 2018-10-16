/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.javi;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.AbstractMap.SimpleEntry;
import java.lang.Double;
import core.Coord;
import core.SimClock;

/**
 * Class for storing and manipulating the meeting probabilities for the Javi
 * router module.
 */
public class NodePositionsSet {
	
	final double TIMEOUT = 100.0;
	final double EPSILON = 0.01;
	int myID;
	public boolean synced = true;
	private List<Integer> staticIDs;
	private List<Integer> myNeighbors;
	Map<Integer, NodeDistancesSet> allNeighbors;
	Map<Integer, Coord> myMap, myMapOld;
	public double creationTime, updateTime;
	
	public NodePositionsSet(Map<Integer, NodeDistancesSet> allNeighbors, int myID, ArrayList<Integer> staticIDs){
	
		this.allNeighbors = allNeighbors;
		this.myID = myID;
		/*TODO: update neighbor input from allNeighbors to be arraylist*/
		this.myNeighbors = allNeighbors.get(myID).knownNeighbors();
		this.creationTime = updateTime = SimClock.getTime();
		this.staticIDs = staticIDs;

	}

	public NodePositionsSet(int myID, ArrayList<Integer> staticIDs){
		this.myID = myID;
		this.staticIDs = staticIDs;
	}
	//only to use by static nodes for global map
	public NodePositionsSet(int myID, List<Integer> staticIDs, Map<Integer, Coord> map){
		this.myID = myID;
		this.synced = true;
		this.staticIDs = staticIDs;
		this.myMap = map;
		this.myNeighbors = new ArrayList<Integer>();
		myNeighbors.addAll(map.keySet());
		//max time, so it never gets overwritten.
		this.creationTime = this.updateTime = Double.MAX_VALUE;
	}
	
	public NodePositionsSet(){
		this.myID = 0;
	}

	public void setID(int myID){
		this.myID = myID;
	}
	
	public int getID(){
		return this.myID;
	}
	
	public void setStaticIDs(ArrayList<Integer> set){
		staticIDs = set;
	}
	
	public Coord getCoord(int x){
		if(myMap.containsKey(x))
			return myMap.get(x);
		else return new Coord(0.0,0.0);
	}
	
	
	/* update neighbors, without computing a map */
	/* return: number of static nodes, or -1 if i didn't compute the map */
	public int update(Map<Integer, NodeDistancesSet> newNeighbors, boolean update){
		int ret;
		this.allNeighbors = newNeighbors;
		//core.Debug.p("allneighbors: " + this.allNeighbors.size());
		/*update known nbs*/
		myNeighbors = allNeighbors.get(myID).knownNeighbors();
		//core.Debug.p("my neighbors:" + this.myNeighbors);
		myMapOld = myMap;
		/*compute new map*/
		if(update == true && myNeighbors.size()> 2){
			ret = this.computeMap();
		}
		else{
			synced = false;
			ret = -1;
		}
		this.updateTime = SimClock.getTime();
		return ret;
	}
	
	/* if the list of neighbor data matches my current map */
	public boolean isSynced(){
		return synced;
	}
	
	
	public Map<Integer, Coord> getMap(){
		return myMap;
	}
	
	
	/*computes the map with me as the center using the grid of distances
	 * return: -1 if map couldn't be computed
	 * 		   number of static nodes in the map */
	
	public int computeMap(){
		
		/*not enough neighbors*/
		if(allNeighbors.size() < 2){
			//core.Debug.p("not enough neighbors");
			return -1;
		}
		
		/* comparator to sort good candidates for the map 
		 * if they have more neighbors, we assume better chance to triangulate */
		SortedSet<Map.Entry<Integer, Integer>> sortedset = new TreeSet<Map.Entry<Integer, Integer>>(
	            new Comparator<Map.Entry<Integer, Integer>>() {
	                @Override
	                public int compare(Map.Entry<Integer, Integer> e1,
	                        Map.Entry<Integer, Integer> e2) {
	                	int v = e1.getValue().compareTo(e2.getValue());
	                	int k = e1.getKey().compareTo(e2.getKey());
	                    if(v==0 && k != 0) return 1;
	                    else return v;
	                }
	            });


		/*list of neighbors of each of my neighbors*/
		Map <Integer, Integer> nb_nbs= new TreeMap<Integer, Integer>();
		for(int nb : myNeighbors){
			NodeDistancesSet s = allNeighbors.get(nb);
			if(s != null){
				sortedset.add(new SimpleEntry<Integer, Integer>(nb, s.knownNeighbors().size()));
			}
		}

		
		/* not enough data for the map*/
		if(sortedset.size() < 2){
			//core.Debug.p("--I don't have 2 neighbors:" + sortedset.size() + "--");
			return -1;
		}
		
		/* verify my 2 max are also neighbors. if not, go through it again finding a good one --or failing */
		
		int staticCount = 0;
		int node_p_nbs = sortedset.last().getValue();
		int node_p = sortedset.last().getKey();
		sortedset.remove(sortedset.last());
		
		int node_q_nbs = sortedset.last().getValue();
		int node_q = sortedset.last().getKey();
		sortedset.remove(sortedset.last());
		
		int r;
		if (!allNeighbors.get(node_p).knownNeighbors().contains(node_q)){ /*not neighbors, must iterate*/
			while(sortedset.size()> 0){
				r = sortedset.last().getKey();
				
				/* check if the next one is neighbor with either p or q */
				if(allNeighbors.get(node_p).knownNeighbors().contains(r)){
					node_q = r;
					break;
				}
				else if(allNeighbors.get(node_p).knownNeighbors().contains(r)){
					node_p = r;
					break;
				}				
				else{
					sortedset.remove(sortedset.last());
				}
			}
			/*looped without success*/
			if(sortedset.size() == 0){
				//core.Debug.p("neighbors don't know each other");
				return -1;
			}
			else{ /*check if they're static*/
				if(staticIDs.contains(node_p)) staticCount++;
				if(staticIDs.contains(node_q)) staticCount++;
			}
			//core.Debug.p("static count:" + staticCount);

		}	
		
		/*prepare the map*/
		if(myMap == null){
			myMap = new HashMap<Integer, Coord>();
		}
		else{
			myMap.clear();
		}
		
		/*get distances and angle*/
		double x, y, alpha, beta, ang;
		
		double dist_i_p = allNeighbors.get(myID).getDistFor(node_p);
		double dist_i_q = allNeighbors.get(myID).getDistFor(node_q);
		double dist_p_q = allNeighbors.get(node_p).getDistFor(node_q);
		
		if(dist_i_p < 0 || dist_i_q < 0|| dist_p_q < 0){
			return -1;
		}
		ang = Math.acos( //PIQ
				(dist_i_p*dist_i_p + dist_i_q*dist_i_q - dist_p_q*dist_p_q)/(2*dist_i_p*dist_i_q));
		
		//core.Debug.p("dip:" + dist_i_p + ", diq:" + dist_i_q + ", dpq:" + dist_p_q + ", ang" + ang);

		
		/*add first values*/
		this.myMap.put(myID, new Coord(0.0, 0.0));
		this.myMap.put(node_p, new Coord(dist_i_p, 0.0));
		this.myMap.put(node_q, new Coord(dist_i_q*Math.cos(ang), dist_i_q*Math.sin(ang)));

		
		/*add the rest-- if i can*/
		for(int n : myNeighbors){
			if (n==myID || n==node_p || n==node_q){
				continue;
			}
			else{
				double dist_p_n = -1.0, dist_q_n = -1.0;
				double dist_i_n = allNeighbors.get(myID).getDistFor(n);
				
				/*get the distances of the node to my map nodes*/
				NodeDistancesSet dp = allNeighbors.get(node_p);
				if(dp != null)
					dist_p_n = dp.getDistFor(n);
				
				NodeDistancesSet dq = allNeighbors.get(node_q);
				if(dq != null)
					dist_q_n = dq.getDistFor(n);
				
				/* i know all distances -> can add it to the map */
				if(dist_i_n > 0.0 && dist_p_n > 0.0 && dist_q_n > 0.0){
			
					alpha = Math.acos(
							(dist_i_n*dist_i_n + dist_i_p*dist_i_p - dist_p_n*dist_p_n)/(2*dist_i_n*dist_i_p));
					
					beta = Math.acos(
							(dist_i_n*dist_i_n + dist_i_q*dist_i_q - dist_q_n*dist_q_n)/(2*dist_i_n*dist_i_q));
					x = dist_i_n*Math.cos(alpha);
					/*see what side it is on */
					
					double angdiff = Math.abs(alpha-ang);
					if(Math.abs(beta-angdiff) < EPSILON){
						y = dist_i_n*Math.sin(alpha);
					}
					else{
						y = -dist_i_n*Math.sin(alpha);
					}
					if(!(Double.isNaN(x) || Double.isNaN(y)))
						this.myMap.put(n, new Coord(x, y));
					
					/*we see if it's static*/
					if(staticIDs.contains(n)) staticCount++;
				}
			}
		}
		creationTime = SimClock.getTime();
		return staticCount;
	}
	
	
	/*functions for merging maps*/
	
	/*recenter a map*/
	public static Map<Integer, Coord> recenter(Map<Integer, Coord> map, Coord orig){
		
		Map<Integer, Coord> ret = new HashMap<Integer, Coord>();
				
		double deltax = orig.getX();
		double deltay = orig.getY();
		for(Map.Entry<Integer, Coord> entry : map.entrySet()){
			ret.put(entry.getKey(), new Coord(entry.getValue().getX() + deltax, entry.getValue().getY() + deltay));
		}
		return ret;
	}
	
	
	/*rotate the coordinates in certain angle*/
	public static Map<Integer, Coord> rotate(Map<Integer, Coord> map1, int orig_m1, double angle){
		Map<Integer, Coord> ret = new HashMap<Integer, Coord>();
		Coord orig = map1.get(orig_m1);
		for(Map.Entry<Integer, Coord> entry : map1.entrySet()){	
			double r = orig.distance(entry.getValue());
			double theta = Math.atan2(entry.getValue().getY(),entry.getValue().getX());
			theta -= angle;
			double x = r*Math.cos(theta);
			double y = r*Math.sin(theta);
			ret.put(entry.getKey(), new Coord(x, y));
		}
		return ret;
	}
	
	/*mirror through y axis*/
	public static Map<Integer, Coord> mirror(Map<Integer, Coord> map){
		Map<Integer, Coord> ret = new HashMap<Integer, Coord>();
		for(Map.Entry<Integer, Coord> entry: map.entrySet()){
			ret.put(entry.getKey(), new Coord(-(entry.getValue().getX()), entry.getValue().getY()));
		}
		return ret;
	}
	
	/*find suitable neighbor for angle correction*/
	public static int findNeighbor(Map<Integer, Coord> map1, Map<Integer, Coord> map2, int id1, int id2){
		if(map1 == null || map2 == null){
			return -1;
		}
		int k = -1;
		for(Integer m: map1.keySet()){
			if(map2.keySet().contains(m) && m!=id1 && m!=id2){ 
				k = m;
				break;
			}
		}
		return k;
		
	}
	
	
	/*mix 2 maps into the coord system of the 1st
	 * both maps must contain both ids and a common neighbor
	 * */
	public static Map<Integer, Coord> mixMap(Map<Integer, Coord> map1, Map<Integer, Coord> map2, int id1, int id2){

		//core.Debug.p("**mixmap**");
		//core.Debug.p(toString(map1, id1));
		//core.Debug.p(toString(map2, id2));
		Map<Integer, Coord> ret = new HashMap<Integer, Coord>();
		//first: pick the neighbors we share to triangulate -- we use 2
		int nb = findNeighbor(map1, map2, id1, id2);
		//core.Debug.p("id1:" + id1 + ", id2:" + id2 + ", nb:" + nb);
		if(nb < 0){
			//core.Debug.p("No suitable neighbor found for mixing maps");
			return null;
		}
		//second: get the angles and find if we need to mirror and get corr angle.
		//map1 -> i; map2 -> k.
		
		boolean mirr = false;
		double corr;
		
		Coord j_map1 = map1.get(nb);
		Coord j_map2 = map2.get(nb);
		Coord k_map1 = map1.get(id2);
		Coord i_map2 = map2.get(id1);
		
		//core.Debug.p("coords j1" + j_map1 + ", j2:" + j_map2 + ", k1:" + k_map1 + ", i2:" + i_map2);
		
		
		//core.Debug.p(toString(map1, id1));
		//core.Debug.p(toString(map2, id2));
		if(j_map1 == null){
			//core.Debug.p("my nodes are not neighbors between themselves????? c1");
			return null;
		}
		if(j_map2 == null){
			//core.Debug.p("my nodes are not neighbors between themselves????? c2");
			return null;
		}
		if(k_map1 == null){
			//core.Debug.p("my nodes are not neighbors between themselves????? c3");
			return null;
		}
		if(i_map2 == null){
			//core.Debug.p("my nodes are not neighbors between themselves????? c4");
			return null;
		}
		double alpha_j = Math.atan2(j_map1.getY(), j_map1.getX());
		double alpha_k = Math.atan2(k_map1.getY(), k_map1.getX());
		double beta_i = Math.atan2(i_map2.getY(), i_map2.getX());
		double beta_j = Math.atan2(j_map2.getY(), j_map2.getX());
		
		double aj_ak = alpha_j - alpha_k;
		double bj_bi = beta_j - beta_i;
		
		//core.Debug.p("aj -ak: " + aj_ak);
		//core.Debug.p("bj -bi: " + bj_bi);
		
		if(aj_ak< 0) aj_ak = Math.PI*2-aj_ak;
		if(bj_bi< 0) bj_bi = Math.PI*2-bj_bi;
		
		//core.Debug.p("aj -ak: " + aj_ak);
		//core.Debug.p("bj -bi: " + bj_bi);
		
		if( ((aj_ak <= Math.PI) && (bj_bi >= Math.PI)) 
				|| ((aj_ak >= Math.PI) && (bj_bi <= Math.PI))){
			mirr = false;
			corr = beta_i - alpha_k + Math.PI;
			//core.Debug.p("CASE A");
		}
		else if(((aj_ak <= Math.PI) && (bj_bi <= Math.PI)) 
				|| ((aj_ak >= Math.PI) && (bj_bi >= Math.PI))){
			mirr = true;
			corr = beta_i + alpha_k;
			//core.Debug.p("CASE B");
		}
		else{
			//System.out.println("weird angles: aj-ak " + aj_ak + ", bj-bi " + bj_bi);
			return null;
		}
		
		ret = rotate(map2, id2, corr);
		if(mirr) ret = mirror(ret);
		ret = recenter(ret, map1.get(id2));
		//core.Debug.p("map2 final: " + toString(ret, id2));
		//core.Debug.p("map1 final: " + toString(map1, id1));

		/*third: mix coords*/
		for(Map.Entry<Integer, Coord> e : ret.entrySet()){
			if(!map1.keySet().contains(e.getKey())){
				map1.put(e.getKey(), e.getValue());
			}
		}
		return map1;
	}
		
	@Override
	public String toString(){
		String s = "*** " + myID + ":";
		for(Map.Entry<Integer, Coord> e : myMap.entrySet()){
			s += "[" + e.getKey() + ": " + e.getValue().toString() + "]";
		}
		return s;
	}
	
	public static String toString(Map<Integer, Coord> m, int id){
		String s = "*** " + id + ":";
		for(Map.Entry<Integer, Coord> e : m.entrySet()){
			s += "[" + e.getKey() + ": " + e.getValue().toString() + "]";
		}
		return s;
	}
}