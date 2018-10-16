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
import java.util.AbstractMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Double;
import java.util.AbstractMap.SimpleEntry;

import routing.JaviRouter;
import core.Coord;
import core.SimClock;


import java.util.ArrayList;

/**
 * Class for storing and manipulating the global maps computed.
 */
public class GlobalMap {
	
	/**
	 * synced:
	 * -1 if not synced -> must compute new map
	 * 0 if synced with my own map
	 * 1 if not synced, using someone else's map -> must merge maps
	 */
	final static double EPSILON = 0.2;
	final static double TIMEOUT = 100;
	public int myID, synced, mapCount, ref_id;
	public Coord ref_c;
	public boolean mapExists;
	private static HashMap<Integer, Coord> staticNodes;
	private ArrayList<Integer> myMapNodes;
	private NodePositionsSet myMap;
	
	public Map<Integer, NodePositionsSet> allMaps;
	public Map<Integer, Coord> globalMap, oldMap;
	
	private double creationTime, updateTime;
	
	public GlobalMap(NodePositionsSet myMap, int myID, HashMap<Integer, Coord> staticNodes){
	
		this.myID = myID;
		this.myMap = myMap;
		this.synced = -1;
		this.ref_id = -1;
		this.mapExists = false;
		this.staticNodes = staticNodes;
		
		this.myMapNodes = new ArrayList<Integer>();
		this.allMaps = new HashMap<Integer, NodePositionsSet>();
		this.globalMap = new HashMap<Integer, Coord>();
		this.creationTime = SimClock.getTime();
		this.updateTime = SimClock.getTime();
	}

	public GlobalMap(){
		this.myID = 0;
		this.myMap = null;
		this.synced = -1;
		this.ref_id = -1;
		this.mapExists = false;
		this.staticNodes = new HashMap<Integer, Coord>();
		
		this.myMapNodes = new ArrayList<Integer>();
		this.allMaps = new HashMap<Integer, NodePositionsSet>();
		this.globalMap = new HashMap<Integer, Coord>();
		this.creationTime = SimClock.getTime();
		this.updateTime = SimClock.getTime();

	}

	public void restart(){
		core.Debug.p("********************************restarting map: " + myID);
		this.myMap = null;
		this.synced = -1;
		this.ref_id = -1;
		this.ref_c = null;
		this.mapExists = false;
		this.globalMap = new HashMap<Integer, Coord>();
		this.creationTime = SimClock.getTime();
		this.updateTime = SimClock.getTime();
	}
	
	/*update my own local map and recalculate the global map*/
	public int updateMap(NodePositionsSet myMap){
		/*check updated ID*/
		if(myID == 0){
			core.Debug.p("must set myID in global map before updating");
			return -1;
		}
		this.myMap = myMap;
		synced=-1;
		return 0;
	}	

	/*update my map with someone else's local map*/
	public int addMap(NodePositionsSet otherMap, int otherID){
		if(otherMap.getMap() == null){
			return -1;
		}
		allMaps.put(otherID, otherMap);
		synced = -1;
		return 0;
	}
	
	/*update my map with someone else's global map*/
	public int addMap(GlobalMap otherMap){
		if(otherMap.getMap().size() < 3) return -1;
		/*map exists, but i need to re-mix it*/
		if(mapExists && synced == -1){
			localmix();
		}	
		/*if i cant make a map yet, i use the one i got*/
		else if(!mapExists){
			int c = makeGlobal();
			if(c < 0){
				if(otherMap.getCT() > (SimClock.getTime() - TIMEOUT)){
					this.globalMap = otherMap.globalMap;
					this.myMapNodes = otherMap.myMapNodes;
					this.synced = 1;
					this.mapExists = true;
					this.ref_id = otherMap.ref_id;
					this.ref_c = otherMap.ref_c;
					core.Debug.p("a");
					return 1;
				}
			}
			else{
				this.synced = 0;
				this.mapExists = true;
				core.Debug.p("b");
			}
		}
		/*if i have an updated map, just join them preferring the one that used newer data*/
		if(mapExists && (synced == 0 || synced == 1)){
			core.Debug.p("nene");
			boolean newer = this.updateTime < otherMap.updateTime ? true : false;
			//have to be careful with shifted values!
			Coord c = new Coord(this.ref_c.getX() - otherMap.ref_c.getX(), this.ref_c.getY() - otherMap.ref_c.getY());
			Map<Integer, Coord> othershifted = shiftMap(otherMap.getMap(), c);
			
			for(Map.Entry<Integer, Coord> node : othershifted.entrySet()){
				if(globalMap.containsKey(node.getKey())){
					if(newer) globalMap.replace(node.getKey(), node.getValue());
				}
				else{
					globalMap.put(node.getKey(), node.getValue());
					myMapNodes.add(node.getKey());
				}
			}
			return myMapNodes.size();
		}
		else return -1;
	}

	public void setID(int myid){
		this.myID = myid;
	}
	
	public int getID(){
		return this.myID;
	}
	
	public void setStaticNodes(HashMap<Integer, Coord> nodes){
		staticNodes = nodes;
	}
	
	public Map<Integer, Coord> getMap(){
		if(!mapExists || globalMap == null){
			return new HashMap<Integer, Coord>();
		}
		return globalMap;
	}
	
	public Map<Integer, Coord> getGlobalMap(){
		//core.Debug.p("id " + this.myID + ", ex? " + mapExists + ", size?" + globalMap.size());
		if(mapExists){
			Map<Integer, Coord> realCoordMap = new HashMap<Integer, Coord>();
			for(Map.Entry<Integer, Coord> c : this.globalMap.entrySet()){
				//core.Debug.p("c: id" + c.getKey() + ", v:" + c.getValue().toString() + ", r:"+ realCoord(c.getValue()).toString());
				realCoordMap.put(c.getKey(), realCoord(c.getValue()));
			}
			return realCoordMap;
		}
		else return null;
	}
	
	public Coord getGlobalCoord(int i){
		if(globalMap != null && globalMap.containsKey(i)){
			return realCoord(globalMap.get(i));
		}
		else{
			return null;
		}
	}
	
	/*make my global map. first create a map to adjust my map to real coordinates, then 'add' the nodes from the other localmaps*/
	public int makeGlobal(){
		if(mapExists && this.creationTime > SimClock.getTime() - TIMEOUT){
		//TODO: merge new maps	
			return globalMap.size();
		}
		else if(mapExists && this.creationTime <= SimClock.getTime() - TIMEOUT){
		//restart my map
			globalMap = null;
			synced = -1;
			mapExists = false;
			ref_id = -1;
			this.creationTime = SimClock.getTime();
			
		}
		//usar 3 estáticos para ajustar coordenadas
		
		Map<Integer, Coord> m = new HashMap<Integer, Coord>();
		ArrayList<Integer> staticNBs = new ArrayList<Integer>();
		/*first find the static nodes i have in my map*/
		if(this.myMap == null || this.myMap.getMap() == null) return -1;
		for(Map.Entry<Integer, Coord> nb : this.myMap.getMap().entrySet()){
			/*add them to my map*/
			if (staticNodes.keySet().contains(nb.getKey())){
				staticNBs.add(nb.getKey());	
				m.put(nb.getKey(), staticNodes.get(nb.getKey()));
			}
		}
		if(staticNBs.size() < 3){
			//core.Debug.p("can't determine global coordinates");
			return -1;
		}
		else{
			//map centered on certain static node, using real coordinates.
			Map.Entry<Integer, NodePositionsSet> s = startingGlobalMap(staticNBs);
			//core.Debug.p("***map centered on static node" + ref_id + "***");
			//core.Debug.p(this.toString());
			if(s.getValue() != null){
				//core.Debug.p("globalmap not null! wohoo!");
				//core.Debug.p("t= " + SimClock.getTime() + "  base map:" + realMap(s.getValue().getMap()));
				/*now mix this map*/
				//core.Debug.p("+++mixed map:" + realMap(s.getValue().getMap()));
				globalMap = NodePositionsSet.mixMap(s.getValue().getMap(), myMap.getMap(), ref_id, myID);
				//core.Debug.p("+++mixed map:" + realMap(globalMap));
				if(globalMap == null) return -1;
				localmix();
				if(globalMap == null) return -1;
				this.updateTime = SimClock.getTime();
				return globalMap.size();
			}
			else{
				core.Debug.p("can't determine global coordinates");
				return -1;
			}
		}
	}
	
	private void localmix(){
		//core.Debug.p("starting localmix");
		ArrayList<Integer> keysToRemove = new ArrayList<Integer>();
		for(Map.Entry<Integer, NodePositionsSet> local : this.allMaps.entrySet()){
			//don't use old maps
			if(local.getValue() == null || local.getValue().updateTime < (SimClock.getTime() - TIMEOUT)) {
				keysToRemove.add(local.getKey());
				continue;
			}
			//core.Debug.p("adding localmap " + local.getKey() + "time: " + SimClock.getTime());
			//see if i can use my current reference or if i need to change it
			Map<Integer, Coord> currentlocal = local.getValue().getMap();

			//null map, omit it.
			if(currentlocal == null){
				keysToRemove.add(local.getKey());
				//useless map, we just remove it.
			}

			else if(globalMap.containsKey(local.getKey()) && currentlocal.containsKey(ref_id)){
				Map<Integer, Coord> newMap = NodePositionsSet.mixMap(globalMap, currentlocal, ref_id, local.getKey());
				if(newMap != null){
					globalMap = newMap;
					mapExists = true;
					this.updateTime = SimClock.getTime();
				}
			}
			//if it doesn't, i have to find a node that we share
			else if(globalMap.get(local.getKey()) != null){
				int tempID = -1;
				Coord tempC = new Coord(0.0,0.0);
				for(Map.Entry<Integer, Coord> i: globalMap.entrySet()){
					if(i.getKey() != this.myID && i.getKey() != local.getKey() && currentlocal.containsKey(i.getKey())){
						tempID = i.getKey();
						tempC = i.getValue();
						break;
					}
				}
				if(tempID>0){
					Map<Integer, Coord> newMap = NodePositionsSet.mixMap(shiftMap(this.globalMap, tempC), currentlocal, tempID, local.getKey());
					if(newMap != null){
						globalMap = shiftMap(newMap, new Coord(-(tempC.getX()), -(tempC.getY())));
						mapExists = true;
						this.updateTime = SimClock.getTime();
						//core.Debug.p("localmix - new ref");
					}
				}
			}
			else{
				globalMap.remove(local.getKey());
			}
		}
		for(Integer i: keysToRemove){
			this.allMaps.remove(i);
			core.Debug.p("removing map "+ i);
		}
		//DEBUG: see diffs with statics
		synced = 0;
		
	}

	public void checkGlobalMap(){
		for(Map.Entry<Integer, Coord> c : globalMap.entrySet()){
			if(staticNodes.containsKey(c.getKey()) && realCoord(c.getValue()).compareTo(staticNodes.get(c.getKey())) != 0){
				core.Debug.p("static node not done well: " + realCoord(c.getValue()).toString() + " vs " + staticNodes.get(c.getKey()));
				//System.exit(1);
			}
		}
	}
	//globalmap shifted by coordinate c.
	public static Map<Integer, Coord> shiftMap(Map<Integer, Coord> map, Coord c){
		Map<Integer, Coord> retmap = new HashMap<Integer, Coord>();
		if(c == null || map == null) {core.Debug.p("WUT"); return null;}
		for(Map.Entry<Integer, Coord> i: map.entrySet())
				retmap.put(i.getKey(), new Coord(i.getValue().getX() - c.getX(), i.getValue().getY() - c.getY()));
		return retmap;

	}
				
		
	//shifted by ref_c
	public Coord realCoord(Coord c){
		//core.Debug.p("ref: " + ref_c.toString());		
		//core.Debug.p("c: " + c.toString());
		if(c == null){
			core.Debug.p("trying to get realcoord of null coord..");
		}
		Coord ret = new Coord(c.getX() + ref_c.getX(), c.getY() + ref_c.getY());
		//Coord ret2 = new Coord(c.getY() + ref_c.getX(), c.getX() + ref_c.getY());
		//core.Debug.p("new: " + ret.toString());
		return ret;

	}
	
	public int mixMap(NodePositionsSet other){
		//core.Debug.p("mixing maps");
		
		if(!mapExists){
			globalMap = new HashMap<Integer, Coord>();
		}
		else{
			oldMap = globalMap;
		}
		int j = -1;
		globalMap = NodePositionsSet.mixMap(myMap.getMap(), other.getMap(), this.myID, other.getID());
		if(globalMap == null) return -1;
		else{
			mapExists = true;
			synced = 0;
		}
		return globalMap.size();
	}
	
	/*how many localmaps make my globalmap*/
	public int mapCount(){
		return this.mapCount;
	}

	public double getCT(){
		return this.creationTime;
	}

	public double updateTime(){
		return this.updateTime;
	}

	public int mapSize(){
		return this.globalMap.size();
	}
	
	
/*get the coord of my point*/
private static Coord findCoordinate(Coord p0, Coord p1, Coord p2, double r0, double r1, double r2){

	double x0, y0, x1, y1, x2, y2;
	x0 = p0.getX(); y0 = p0.getY();
	x1 = p1.getX(); y1 = p1.getY();
	x2 = p2.getX(); y2 = p2.getY();
	//core.Debug.p("x0:" + x0 + ",y0:" + y0 + ",r0:" + r0);
	//core.Debug.p("x1:" + x1 + ",y1:" + y1 + ",r1:" + r1);
	//core.Debug.p("x2:" + x2 + ",y2:" + y2 + ",r2:" + r2);
	double a, dx, dy, d, h, rx, ry;
	double point2_x, point2_y;

	/* dx and dy are the vertical and horizontal distances between
	 * the circle centers.
	 */	
	dx = x1 - x0;
	dy = y1 - y0;

	/* Determine the straight-line distance between the centers. */
	d = Math.sqrt((dy*dy) + (dx*dx));

	/* Check for solvability. */
	if (d > (r0 + r1)){
		/* no solution. circles do not intersect. */
		//core.Debug.p("no solution1");
		return null;
	}
	if (d < Math.abs(r0 - r1)){
		/* no solution. one circle is contained in the other */
		//core.Debug.p("no solution2");
		return null;
	}

	/* 'point 2' is the point where the line through the circle
	 * intersection points crosses the line between the circle
	 * centers.
	 */

	/* Determine the distance from point 0 to point 2. */
	a = ((r0*r0) - (r1*r1) + (d*d)) / (2.0 * d) ;

	/* Determine the coordinates of point 2. */
	point2_x = x0 + (dx * a/d);
	point2_y = y0 + (dy * a/d);

	/* Determine the distance from point 2 to either of the
	 * intersection points.
	 */	
	h = Math.sqrt((r0*r0) - (a*a));

	/* Now determine the offsets of the intersection points from
	 * point 2.
	 */
	rx = -dy * (h/d);
	ry = dx * (h/d);

	/* Determine the absolute intersection points. */
	double intersectionPoint1_x = point2_x + rx;
	double intersectionPoint2_x = point2_x - rx;
	double intersectionPoint1_y = point2_y + ry;
	double intersectionPoint2_y = point2_y - ry;

	/* Lets determine if circle 3 intersects at either of the above intersection points. */
	dx = intersectionPoint1_x - x2;
	dy = intersectionPoint1_y - y2;
	double d1 = Math.sqrt((dy*dy) + (dx*dx));

	dx = intersectionPoint2_x - x2;
	dy = intersectionPoint2_y - y2;
	double d2 = Math.sqrt((dy*dy) + (dx*dx));

	//core.Debug.p("d1: " + d1 + ", d2: " + d2 + ", r2: " + r2);
	//core.Debug.p("diff1: " + Math.abs(d1-r2));
	//core.Debug.p("point1: (" + intersectionPoint1_x + ", " + intersectionPoint1_y + ")");
	//core.Debug.p("diff2: " + Math.abs(d2-r2));
	//core.Debug.p("point2: (" + intersectionPoint2_x + ", " + intersectionPoint2_y + ")");


	if(Math.abs(d1 - r2) < EPSILON) {
		//core.Debug.p("point: (" + intersectionPoint1_x + ", " + intersectionPoint1_y + ")");
		return new Coord(intersectionPoint1_x, intersectionPoint1_y);
	}
	else if(Math.abs(d2 - r2) < EPSILON) {
		//core.Debug.p("point: (" + intersectionPoint2_x + ", " + intersectionPoint2_y + ")");
		return new Coord(intersectionPoint2_x, intersectionPoint2_y);}
	else {
		//core.Debug.p("no intersection");
		return null;
	}
}


public Map.Entry<Integer, NodePositionsSet> startingGlobalMap(ArrayList<Integer> staticNBs){

	boolean imstatic = false;
	if(this.staticNodes.containsKey(this.myID)){
		imstatic = true;
	}
	
	//core.Debug.p("***** starting globalmap " + this.myID + " ***** " + imstatic);
	int node_i  = -1; int node_p = -1; int node_q = -1;
	Coord coord_i = new Coord(0.0,0.0);
	Coord coord_o = new Coord(0.0,0.0);
	double dist_i_j, dist_i_p, dist_i_q, dist_p_j, dist_q_j, dist_p_q, ang, alpha, beta, x, y;
	
	Map<Integer, Coord> output = new HashMap<Integer, Coord>();
	Map<Integer, Coord> map = this.myMap.getMap();
	
	for(Integer st : staticNBs){
		if(imstatic && st == this.myID) continue;
		//core.Debug.p("nb " + st);
		if(node_i < 0 && map.containsKey(st)){
			node_i = st;
			coord_i = staticNodes.get(st);
//			core.Debug.p("node_i:" + st);
			output.put(st, new Coord(0.0,0.0));
		}
		else if(map.containsKey(st)){ 
			if(node_p < 0){ 
				node_p = st;
//				core.Debug.p("node_p: " + st);
			}
			else if(node_q < 0){
				node_q = st;
//				core.Debug.p("node_q: " + st);
			}
			Coord newcord = new Coord(staticNodes.get(st).getX()-coord_i.getX(), staticNodes.get(st).getY()-coord_i.getY());
//			core.Debug.p("node " + st + ": " + newcord.toString());
			output.put(st, newcord);
		}
	}
	
	if(imstatic && node_i >= 0){
		//no need for calculations, just take the staticnodes centered on node_i
		
		output = shiftMap(staticNodes, coord_i);
//		core.Debug.p("should be 0: " + output.get(node_i).toString());
		this.ref_id = node_i;
		this.ref_c = coord_i;
		List<Integer> l = new ArrayList<Integer>();
		l.addAll(staticNodes.keySet());
		
		NodePositionsSet ret = new NodePositionsSet(this.ref_id, l, output);
		this.globalMap = output;
		this.mapExists = true;
		this.synced = 0;
		return new AbstractMap.SimpleEntry(node_i, ret);
		
	}
	
	//core.Debug.p("outputsize: " + output.size());
	if(output.size() < 3 || node_i < 0 || node_p < 0 || node_q < 0) return null;
	
	//now add our node - use staticnode coords
	dist_i_p = coord_i.distance(staticNodes.get(node_p));
	dist_i_q = coord_i.distance(staticNodes.get(node_q));
	dist_p_q = staticNodes.get(node_p).distance(staticNodes.get(node_q));
	//this uses myMap;
	dist_i_j = coord_o.distance(this.myMap.getCoord(node_i));
	dist_p_j = coord_o.distance(this.myMap.getCoord(node_p));
	dist_q_j = coord_o.distance(this.myMap.getCoord(node_q));

//	core.Debug.p("dist_i_j:" + dist_i_j);
//	core.Debug.p("dist_p_j:" + dist_p_j);
//	core.Debug.p("dist_q_j:" + dist_q_j);
//	core.Debug.p("dist_p_q:" + dist_p_q);

	if(dist_i_j > 0 && dist_p_j > 0 && dist_q_j > 0){
		
		ang = Math.acos(
				(dist_i_p*dist_i_p + dist_i_q*dist_i_q - dist_p_q*dist_p_q)/(2*dist_i_p*dist_i_q));
		
		alpha = Math.acos(
				(dist_i_j*dist_i_j + dist_i_p*dist_i_p - dist_p_j*dist_p_j)/(2*dist_i_j*dist_i_p));
		
		beta = Math.acos(
				(dist_i_j*dist_i_j + dist_i_q*dist_i_q - dist_q_j*dist_q_j)/(2*dist_i_j*dist_i_q));
		
		x = dist_i_j*Math.cos(alpha);
		/*see what side it is on */
		
		double angdiff = Math.abs(alpha-ang);
		
//		if(output.get(node_p).getX() == 0.0) core.Debug.p("CASO 1A");
//		if(output.get(node_p).getX() > 0.0) core.Debug.p("CASO 1B");
//		if(output.get(node_p).getX() < 0.0) core.Debug.p("CASO 1C");
//		
//		if(output.get(node_p).getY() == 0.0) core.Debug.p("CASO 2A");
//		if(output.get(node_p).getY() > 0.0) core.Debug.p("CASO 2B");
//		if(output.get(node_p).getY() < 0.0) core.Debug.p("CASO 2C");
//
//		if(output.get(node_q).getX() == 0.0) core.Debug.p("CASO 3A");
//		if(output.get(node_q).getX() > 0.0) core.Debug.p("CASO 3B");
//		if(output.get(node_q).getX() < 0.0) core.Debug.p("CASO 3C");
//
//		if(output.get(node_q).getY() == 0.0) core.Debug.p("CASO 4A");
//		if(output.get(node_q).getY() > 0.0) core.Debug.p("CASO 4B");
//		if(output.get(node_q).getY() < 0.0) core.Debug.p("CASO 4C");
//
//		if(output.get(node_p).getX() == output.get(node_q).getX()) core.Debug.p("CASO 5A");
//		if(output.get(node_p).getX() > output.get(node_q).getX()) core.Debug.p("CASO 5B");
//		if(output.get(node_p).getX() < output.get(node_q).getX()) core.Debug.p("CASO 5C");
//
//		if(output.get(node_p).getY() == output.get(node_q).getY()) core.Debug.p("CASO 6A");
//		if(output.get(node_p).getY() > output.get(node_q).getY()) core.Debug.p("CASO 6B");
//		if(output.get(node_p).getY() < output.get(node_q).getY()) core.Debug.p("CASO 6C");
//		
		if(Math.abs(beta-angdiff) < EPSILON){
			y = dist_i_j*Math.sin(alpha);
		}
		else{
			y = -dist_i_j*Math.sin(alpha);
		}
		boolean vert = false;
		boolean mirr = false;
		if(output.get(node_p).getX() == output.get(node_i).getX()){
			if(output.get(node_q).getY() == 0 && output.get(node_q).getX() != output.get(node_p).getY()){
				vert = true;
				mirr = false;
//				core.Debug.p("case A");

			}
			else if(output.get(node_q).getX() == output.get(node_p).getY()){
				vert = false;
				mirr = true;
//				core.Debug.p("case B");

			}
			else{
				vert = true;
				mirr = true;
//				core.Debug.p("case C");

			}
		}
		else if(output.get(node_p).getY() == output.get(node_i).getY()){
			if(output.get(node_q).getX() == 0){
				vert = false;
				mirr = false;
//				core.Debug.p("case D");
			}
			else{
				vert = false;
				mirr = true;
//				core.Debug.p("case E");
			}
		}
		else{
//			core.Debug.p("case F");
		}
		
//		core.Debug.p("vert:" + vert + ", mirr:" + mirr + ", c_i " + coord_i.toString() + ", x,y: " + x + "," + y);
//		core.Debug.p("OP1 node " + myID + " time " + SimClock.getTime() + ", x: " + (-y+coord_i.getX()) + ", y:" + (x+coord_i.getY()));
//		core.Debug.p("OP2 node " + myID + " time " + SimClock.getTime() + ", x: " + (-y+coord_i.getX()) + ", y:" + (-x+coord_i.getY()));
//		core.Debug.p("OP3 node " + myID + " time " + SimClock.getTime() + ", x: " + (x+coord_i.getX()) + ", y:" + (y+coord_i.getY()));
//		core.Debug.p("OP4 node " + myID + " time " + SimClock.getTime() + ", y: " + (x+coord_i.getX()) + ", x:" + (y+coord_i.getY()));
//		core.Debug.p("OP5 node " + myID + " time " + SimClock.getTime() + ", y: " + (-x+coord_i.getX()) + ", x:" + (-y+coord_i.getY()));

		if(vert == true && mirr == false){
			output.put(myID, new Coord(-y, x));
		}
		else if(vert == true && mirr == true){
			output.put(myID, new Coord(-y, x));
		}
		else if(vert == false && mirr == true){
			output.put(myID, new Coord(x,-y));
		}
		else if(vert == false && mirr == false){
			output.put(myID, new Coord(x,y));
		}
		else{
			output.put(myID, new Coord(y,x));
		}
	}
	
	this.ref_id = node_i;
	this.ref_c = coord_i;
	List<Integer> l = new ArrayList<Integer>();
	l.addAll(staticNodes.keySet());
	
	NodePositionsSet ret = new NodePositionsSet(this.ref_id, l, output);
	this.globalMap = output;
	this.mapExists = true;
	this.synced = 0;
	return new AbstractMap.SimpleEntry(node_i, ret);
}



//
//
///**Get an initial map with at least 3 static nodes using their real coordinates */
//public Map.Entry<Integer, NodePositionsSet> startingGlobalMap(ArrayList<Integer> staticNBs){
//	core.Debug.p("**** making global map ****");
//	
//	boolean rot = false;
//	List<Integer> l = new ArrayList<Integer>();
//	l.addAll(staticNodes.keySet());
//	int statid = -1, node_p = -1, node_q = -1;
//	Coord statc = new Coord(0.0,0.0);
//	
//	Map<Integer, Coord> output = new HashMap<Integer, Coord>();
//	
//	Map<Integer, Coord> map = this.myMap.getMap();
//	for(Integer st : staticNBs){
//		if(statid < 0 && map.containsKey(st)){
//			//set this coord as 0.0, and save it's shift for future reference
//			statid = st;
//			statc = staticNodes.get(st);
//			output.put(st, new Coord(0.0,0.0));
//			//core.Debug.p("first coord "  + st + ": " + map.get(st).toString() + " => " + statc.toString() + "set as 0,0");
//		}
//		else if(map.containsKey(st)){
//			//find our 2nd node, with equal x or y
//			snx = staticNodes.get(statid).getX();
//			sny = staticNodes.get(statid).getY();
//			stx = staticNodes.get(st).getX();
//			sty = staticNodes.get(st).getX()
//			if(stat2 < 0 && snx == stx){
//				//equal X, must rotate pi/2.
//				rot = true;
//				output.put(st, new Coord(sny-sty, 0.0));
//			}
//			else if(stat2 < 0 && sny == sty){
//				//equal Y, we're in the good coords!
//				stat2 = st;
//				output.put(st, new Coord(snx-stx, 0.0));
//			}			
//			//we have two references: add the shifted ones to the map.
//			Coord newcord = new Coord(staticNodes.get(st).getX()-statc.getX(), staticNodes.get(st).getY()-statc.getY());
//			output.put(st, newcord);
//			//core.Debug.p("following coord " + st + ":"  + map.get(st).toString() + " => " + staticNodes.get(st).toString() + " set as " + newcord.toString());	
//		}
//	}
//	
//	if(output.size() < 3 || statid < 0 || stat2 < 0) return null;
//	else{
//		if(rot == true){
//			for(Map.Entry<Integer, Coord> ent : output.entrySet()){
//				output.replace(ent.getKey(), new Coord(ent.getValue().getY(), ent.getValue().getX()));
//			}
//		}
//		//add my node
//		double alpha_j = Math.acos()
//		
//		
//		this.ref_id = statid;
//		this.ref_c = statc;
//		this.ref_id2 = stat2;
//		NodePositionsSet ret = new NodePositionsSet(this.ref_id, l, output);
//		this.globalMap = output;
//		return new AbstractMap.SimpleEntry(this.ref_id, ret);
//	}
//	//
//	}
	

@Override
public String toString(){
	String s = "globalmap " + myID + ":";
	if(mapExists){
		for(Map.Entry<Integer, Coord> e : globalMap.entrySet()){
			s += "[" + e.getKey() + ": " + realCoord(e.getValue()).toString() + "]";
		}
	}
	else{
		s += "no map found";
	}
	return s;
}

public String realMap(Map<Integer, Coord> map){
	String s = "";
		for(Map.Entry<Integer, Coord> e : map.entrySet()){
			s += "[" + e.getKey() + ": " + realCoord(e.getValue()).toString() + "]";
		}
	return s;
}

}