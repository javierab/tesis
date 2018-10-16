
package edu.bonn.cs.iv.bonnmotion.models.mine;

import java.util.Random;
import java.util.HashMap;
import java.util.LinkedList;


import edu.bonn.cs.iv.bonnmotion.MobileNode;
import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Waypoint;
import edu.bonn.cs.iv.util.PositionHashMap;


/** Mine node */

public class MineNode extends MobileNode {
	public int type;
	public MineArea current_area;
	public Access access;
	public Position start;
	public Position current_position;
	public Position dest_position;
	public int timeout = 0;
	public int timeout_avg;
	public static double min_speed;
	public static double max_speed;
	public static double pause;
	public static int repetitions;
	public double avg_pause;
	public double std_pause;
	public double step_time;
	Random r = new Random();
	
	
	public int state, step;
	public HashMap<Integer, Position> route = new HashMap<Integer, Position>();
	/* * * * * * * * * * *
	 * area:
	 * 0: global 
	 * 1: extraction
	 * 2: maintenance
	 * * * * * * * * * * *
	 * type:
	 * -1: fijo: sin movimiento
	 * 0: maquina: movimiento circular, solo en area de extraccion
	 * 1: mantenimiento: movimiento entre areas de mantencion
	 * 2: supervisor: se mueve por todas las areas
	 * * * * * * * * * * *
	 * * * * * * * * * * */

	public MineNode(Position start) {
		super();
		this.type = -1;
		this.start = start;
		this.dest_position = current_position = start;
		/*each node define its pause*/
		this.avg_pause = 4;
		this.std_pause = 2;
		this.pause = getPause();
		this.state = 0;
		this.step_time = 0;

	}
	
	public String getType(){
		switch(this.type){
		case -1: return "FX";
		case 0: return "LH";
		case 1: return "OP";
		case 2: return "SU";
		default: return "??";
		}
	}
	
	public void add(Position start) {
		this.start = start;
	}
	
	public HashMap<Double, Position> getNextStep(){
		return null;
	}
	
	public void print() {
		System.out.println("Node type " + type + "\nstart " + start.toString() + "\n area " + current_area.getType());
	}

	public String movementString() {
		StringBuffer sb = new StringBuffer(140*waypoints.size());
		for (int i = 0; i < waypoints.size(); i++) {
			Waypoint w = waypoints.elementAt(i);
			sb.append("\n");
			sb.append(w.time);
			sb.append("\n");
			sb.append(w.pos.x);
			sb.append("\n");
			sb.append(w.pos.y);
			sb.append("\n");
			sb.append(w.pos.status);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}


	/*we'll assume it's between 2 and 6s*/
	public double getPause(){
		return r.nextDouble()*(2*avg_pause+1)+std_pause/2;
		
	}

	
	public static LinkedList<Position> getRoute(MineArea current_area, Position current_position, Position dest_position){
		
		LinkedList<Position> route = new LinkedList<Position>();
		HashMap<Double, Position> step_route;
		
		if(current_area.intersectObstacles(current_position, dest_position)){
			int v_index_src = 0;
			int v_index_dst = 0;
			double v_distance_src = 1000000.0;
			double v_distance_dst = 1000000.0;
			
			for(int i = 0; i < current_area.vertices.size(); i++){
				
				if(current_position.distance(current_area.vertices.get(i)) < v_distance_src){
					v_distance_src = current_position.distance(current_area.vertices.get(i));
					v_index_src = i;
				}
				if(dest_position.distance(current_area.vertices.get(i)) < v_distance_dst){
					v_distance_dst = dest_position.distance(current_area.vertices.get(i));
					v_index_dst = i;
				}
			}
			PositionHashMap toSrc = ((PositionHashMap)current_area.shortestpaths.get(current_area.vertices.get(v_index_src)));
			PositionHashMap toDst = ((PositionHashMap)current_area.shortestpaths.get(current_area.vertices.get(v_index_dst)));

			LinkedList<Position> l = (LinkedList<Position>)toSrc.get(current_area.vertices.get(v_index_dst));
			if(l!= null) route = l;
			route.addFirst(current_position);
			route.add(dest_position);
		}
		else{
			route.add(current_position);
			route.add(dest_position);
		}
		
		return route;
		//step_route = stepify(speed, route);
		//return step_route;	
	}
	
	
	/*get a linked list of the route from current_position to dest_position. Only works inside one area!!*/
	public HashMap<Double,Position> getRoute(){
		
		LinkedList<Position> route = new LinkedList<Position>();
		HashMap<Double, Position> step_route;
		
		if(current_area.intersectObstacles(current_position, dest_position)){
			int v_index_src = 0;
			int v_index_dst = 0;
			double v_distance_src = 1000000.0;
			double v_distance_dst = 1000000.0;
			
			for(int i = 0; i < this.current_area.vertices.size(); i++){
				
				if(this.current_position.distance(current_area.vertices.get(i)) < v_distance_src){
					v_distance_src = current_position.distance(current_area.vertices.get(i));
					v_index_src = i;
				}
				if(this.dest_position.distance(current_area.vertices.get(i)) < v_distance_dst){
					v_distance_dst = dest_position.distance(current_area.vertices.get(i));
					v_index_dst = i;
				}
			}
			PositionHashMap toSrc = ((PositionHashMap)current_area.shortestpaths.get(current_area.vertices.get(v_index_src)));
			PositionHashMap toDst = ((PositionHashMap)current_area.shortestpaths.get(current_area.vertices.get(v_index_dst)));

			LinkedList<Position> l = (LinkedList<Position>)toSrc.get(current_area.vertices.get(v_index_dst));
			if(l!= null) route = l;
			route.addFirst(current_position);
			route.add(dest_position);
		}
		else{
			route.add(current_position);
			route.add(dest_position);
		}
		
		step_route = stepify(this.getSpeed(), route);
		return step_route;
	}
	
	
	public HashMap<Double, Position> stepify(double speed, LinkedList<Position> route){
	
		Position[] pos = route.toArray(new Position[0]);
		HashMap<Double, Position> map = new HashMap<Double, Position>();
		double dist;
		map.put(this.step_time, pos[0]);
		
		for(int i = 1; i < pos.length; i++){
			dist = pos[i].distance(pos[i-1]);
			this.step_time += dist/speed;
			map.put(this.step_time, pos[i]);
		}
		return map;
		
	}
	
	
//	public static LinkedList<Position> stepify(double speed, LinkedList<Position> route){
//		
//		LinkedList<Position> ret = new LinkedList<Position>();
//		int i = 0, steps = 0;
//		double distance = 0.0;
//		double v_step = 0.0;
//		double h_step = 0.0;
//		Position src, dst;
//		
//		while(i < route.size()-1){
//			src = route.get(i);
//			dst = route.get(i+1);
//			distance = src.distance(dst);
//			steps = (int) Math.round(distance/speed);
//			v_step = (dst.x - src.x)/steps;
//			h_step = (dst.y - src.y)/steps;
//			for(int j = 0; j < steps-1; j++){
//				ret.add(src.newShiftedPosition(v_step*j, h_step*j));
//			}
//			//System.out.println("route" + src.toString() + " to " + dst.toString() + " will take " + steps + " steps");
//			i++;
//		}
//		//System.out.println("final route will take " + ret.size() + " steps");
//		return ret;
//	}
	
	
	public double getSpeed(){
		return r.nextDouble()*(max_speed - min_speed) + min_speed;
	}
	
}
