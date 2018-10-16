/*******************************************************************************
 ** BonnMotion - a mobility scenario generation and analysis tool             **
 ** Copyright (C) 2002-2012 University of Bonn                                **
 ** Copyright (C) 2012-2015 University of Osnabrueck                          **
 **                                                                           **
 ** This program is free software; you can redistribute it and/or modify      **
 ** it under the terms of the GNU General Public License as published by      **
 ** the Free Software Foundation; either version 2 of the License, or         **
 ** (at your option) any later version.                                       **
 **                                                                           **
 ** This program is distributed in the hope that it will be useful,           **
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of            **
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             **
 ** GNU General Public License for more details.                              **
 **                                                                           **
 ** You should have received a copy of the GNU General Public License         **
 ** along with this program; if not, write to the Free Software               **
 ** Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA **
 *******************************************************************************/

package edu.bonn.cs.iv.bonnmotion.models.mine;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.models.mine.Obstacle;
import edu.bonn.cs.iv.util.PositionHashMap;


public abstract class MineArea extends Polygon {
	
	int POSITIONS = 0;
	int ENTRIES = 1;

	private static final long serialVersionUID = -5890360972338115487L;
	
	protected static boolean debug = false;
	long id;
	public Position[] corners = null;
	public Position[] entries = null;
	public Obstacle[] obstacles = null;
	public LinkedList<Position> vertices = new LinkedList<Position>();
	public LinkedList<Serializable> Graph = new LinkedList<Serializable>();
	public LinkedList<Serializable> MinGraph = new LinkedList<Serializable>();
	public PositionHashMap shortestpaths = new PositionHashMap();
	public int type;
	public LinkedList<MineArea> adjAreas = new LinkedList<MineArea>();
	public LinkedList<LinkedList<Position>> allways = new LinkedList<LinkedList<Position>>();

	
	public String getType(){
		switch(this.type){
			case 0: return "ACC";
			case 1: return "EXT";
			case 2: return "MAN";
			default: return "???";
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	public MineArea(double[] Positions, double[] entries, int type){
		super();
		this.type = type;
		this.InitializeSpecificValues(POSITIONS, Positions);
		this.InitializeSpecificValues(ENTRIES, entries);		
		obstacles = makeObstacles();
		Graph= VisibilityGraph(obstacles);
		for(int i = 0; i < ((LinkedList)Graph.get(0)).size(); i++){
			shortestpaths.put(((Position)((LinkedList)Graph.get(0)).get(i)), Dijkstra(Graph, ((Position)((LinkedList)Graph.get(0)).get(i))));
		}
		this.SetDefaultValues();
		
		
	}

	
	/**
	 * Returns a MineArea instance of the correct type for Positions
	 */
	public static MineArea getInstance(double[] Positions, double[] entries, int type) {
		switch (type) {
			case 0:
				return new Access(Positions, entries);
			case 1:
				return new Extraction(Positions, entries);
			case 2:
				return new Maintenance(Positions, entries);
			default:
				throw new IllegalArgumentException("Tipo desconocido");
		}
	}


	/* positions: list of x,y values */
	protected void InitializeSpecificValues(int valtype, double[] values) {

		Position[] temp_p = new Position[values.length/2];
		Position[] temp_e = new Position[values.length/2];

		if(valtype == POSITIONS){ 
			
			for(int i = 0; i < values.length; i = i+2){
				/*polygon*/
				this.addPoint((int)values[i], (int)values[i+1]);
				temp_p[i/2] = new Position(values[i], values[i+1]);
			}
			corners = temp_p;
		}
		else if(valtype == ENTRIES){ 
			for(int i = 0; i < values.length; i = i+2){
				temp_e[i/2] = new Position(values[i], values[i+1]);
			}		
		}
			entries = temp_e;
	}
	
	/**
	 */
	protected abstract void SetDefaultValues();
	protected abstract Obstacle[] makeObstacles();

	public void print(){
		System.out.print("\nCoordinates of MineArea: " + this.getType() + '\n');
		System.out.print("Corners: ["); for(Position corner:corners) System.out.print("(" + corner.x + ", " + corner.y + ")");
		System.out.print("]\n");
		System.out.print("Entries: [");
		for(Position entry:entries)	System.out.print("(" + entry.x + ", " + entry.y + ")");
		System.out.println("]");
		System.out.print("Obstacles: [");
		for(Obstacle obs:obstacles){ System.out.print("("); obs.print(); System.out.println(")");}
		System.out.println("]");		
	}
	
	public Position getClosestEntry(Position p){
		Position min = p;
		double mindist = 100000;
		for(Position entry : entries){
			if(entry.distance(p) < mindist){
				mindist = entry.distance(p);
				min = entry;
			}
		}
		return min;
	}
	

	public boolean intersectObstacles(Position start, Position end){
		for(int i = 0; i < obstacles.length; i++){
			if(obstacles[i].intersectsLine(start.x, start.y, end.x, end.y)){
				return true;
			}
		}
		return false;
	}
	
	public boolean equals(MineArea other) {
		if(type != other.type) {
			return false;
		}
		for(int i = 0; i < corners.length; i++) {
			if(corners[i] != other.corners[i]){
				return false;
			}
		}
		return true;
	}
	
	public LinkedList<Serializable> VisibilityGraph(Obstacle[] Obstacles){
	
		LinkedList<Position> Vertices;
		if(vertices.size() > 0){ 
			System.out.println("previous vertices?! - WEIRD");
			Vertices = vertices;}
		else{ Vertices = new LinkedList<Position>();}
		PositionHashMap Edges = new PositionHashMap();
		LinkedList<Serializable> VisGraph = new LinkedList<Serializable>();

		//add Corners of Obstacles (convex)
		for(int i = 0; i < Obstacles.length; i++){
			Position[] temp = Obstacles[i].getPosVertices();
			for(int j = 0; j < temp.length; j++){
				Vertices.add(temp[j]);
			}
		}
		for(Position corner: this.corners) Vertices.add(corner);
		for(Position entry: this.entries) Vertices.add(entry);

		for(int i = 0; i < Vertices.size(); i++){
			LinkedList<Line2D.Double> VisEdges = new LinkedList<Line2D.Double>();
			LinkedList<Position> VisVert = new LinkedList<Position>();
			VisVert = VisibleVertices(Vertices.get(i), Vertices, Obstacles);
			for(int j = 0; j < VisVert.size(); j++){
				Line2D.Double line = new Line2D.Double(Vertices.get(i).x, Vertices.get(i).y, VisVert.get(j).x, VisVert.get(j).y);
				VisEdges.add(line);
				
			}
			Edges.put(Vertices.get(i), VisEdges);
		}
		VisGraph.add(Vertices);
		VisGraph.add(Edges);
		vertices = Vertices;
		return VisGraph;
	}
	
	public LinkedList<Position> VisibleVertices(Position Vertex, LinkedList<Position> Vertices, Obstacle[] Obstacles){
		boolean visible = false;
		boolean sameObstacle = false;
		boolean StartOnObstacle = false;
		boolean StopOnObstacle = false;
		Obstacle obstacle = null;
		int numintersections = 0;
		LinkedList<Position> VisVert = new LinkedList<Position>();
		for(int i = 0; i < Vertices.size(); i++){
			numintersections = 0;
			sameObstacle = false;
			StartOnObstacle = false;
			StopOnObstacle = false;
			for(int j = 0; j < Obstacles.length; j++){
				if(Obstacles[j].contains(Vertex.x, Vertex.y)){
					//StartOnObstacle = true;
				}
				if(Obstacles[j].contains(Vertices.get(i).x, Vertices.get(i).y)){
					StopOnObstacle = true;
				}
				if(Obstacles[j].isVertice(Vertex)){
					StartOnObstacle = true;
				}
				if(Obstacles[j].isVertice((Vertices.get(i)))){
					StopOnObstacle = true;
				}
				if(Obstacles[j].sameObstacle(Vertex, Vertices.get(i))) {
					sameObstacle = true;
					obstacle = Obstacles[j];
				}
				if(Obstacles[j].intersectsLine(Vertex.x, Vertex.y, Vertices.get(i).x, Vertices.get(i).y)) {
					numintersections = numintersections + Obstacles[j].intersectsObstacle(Vertex.x, Vertex.y, Vertices.get(i).x, Vertices.get(i).y);
				}
			}
			if((numintersections == 2) && (StartOnObstacle || StopOnObstacle) && !Vertex.equals(Vertices.get(i))){
				VisVert.add(Vertices.get(i));
			}
			if((numintersections == 0) && !StartOnObstacle && !StopOnObstacle && !Vertex.equals(Vertices.get(i))){
				VisVert.add(Vertices.get(i));
			}
			if(sameObstacle){
				if(numintersections == 3){
					VisVert.add(Vertices.get(i));
				}
				else{
					if(numintersections == 4) {
						if(!obstacle.throughObstacle(Vertex.x, Vertex.y, Vertices.get(i).x, Vertices.get(i).y)){
							VisVert.add(Vertices.get(i));
						}
					}
				}
			}
			if(numintersections == 4 && !sameObstacle && StartOnObstacle && StopOnObstacle){
				visible = true;
			}
			else{
				visible = false;
			}
			if(visible){
				VisVert.add(Vertices.get(i));
			}
		}
		return VisVert;
	}

	@SuppressWarnings("rawtypes")
	public Position ClosestEntry(Position pos){
		Position p = entries[0]; 
		double min_dist = Double.MAX_VALUE;
		for(int i = 0; i < this.entries.length; i++){
			if(pos.distance(entries[i]) < min_dist){
				p = entries[i];
			}
		}
		return p;
		
	}
	
	
	//calculates the Algorithm of Dijkstra for given Graph and starting point
	@SuppressWarnings("rawtypes")
	static public PositionHashMap Dijkstra(LinkedList Graph, Position start){
		PositionHashMap weights = new PositionHashMap();
		Double min = new Double(Double.MAX_VALUE);
		Position minpos = null;
		PositionHashMap ways = new PositionHashMap();
		for(int i = 0; i < ((LinkedList)Graph.get(0)).size(); i++){
			if(((Position)((LinkedList)Graph.get(0)).get(i)).equals(start)){
				Double value = new Double(0.0);
				weights.put(start, value);
				LinkedList<Position> computeway = new LinkedList<Position>();
				computeway.add(start);
				ways.put(start, computeway);
			}
			else{
				Double value = new Double(Double.MAX_VALUE);
				weights.put(((Position)((LinkedList)Graph.get(0)).get(i)), value);
			}
		}
		PositionHashMap vertices = new PositionHashMap();
		vertices = ((PositionHashMap)weights.clone());
		while(vertices.size() != 0){
			minpos = null;
			min = new Double(Double.MAX_VALUE);
			Iterator it = vertices.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry entry = (Map.Entry)it.next();
				if(((Double)entry.getValue()).doubleValue() < min.doubleValue()){
					min = ((Double)entry.getValue());
					minpos = ((Position)entry.getKey());
				}
			}
			if(minpos == null){
				return ways;
			}
			vertices.remove(minpos);
			for(int i = 0; i < ((LinkedList)((PositionHashMap)Graph.get(1)).get(minpos)).size(); i++){
				Position endpoint1 = new Position(((Line2D.Double)((LinkedList)((PositionHashMap)Graph.get(1)).get(minpos)).get(i)).x1, ((Line2D.Double)((LinkedList)((PositionHashMap)Graph.get(1)).get(minpos)).get(i)).y1);
				Position endpoint2 = new Position(((Line2D.Double)((LinkedList)((PositionHashMap)Graph.get(1)).get(minpos)).get(i)).x2, ((Line2D.Double)((LinkedList)((PositionHashMap)Graph.get(1)).get(minpos)).get(i)).y2);
				if(!(endpoint1.equals(minpos))){
					double help1 = ((Double)weights.get(endpoint1)).doubleValue();
					double help2 = ((Double)weights.get(minpos)).doubleValue();
					if(help1 > help2 + minpos.distance(endpoint1)){
						Double value = new Double(help2 + minpos.distance(endpoint1));
						weights.changeto(endpoint1, value);
						vertices.changeto(endpoint1, value);
						LinkedList<Position> computeway = new LinkedList<Position>();
						for(int j = 0; j < ((LinkedList)ways.get(minpos)).size(); j++){
							computeway.add(((Position)((LinkedList)ways.get(minpos)).get(j)));
						}
						computeway.add(endpoint1);
						ways.changeto(endpoint1, computeway);
					} 
				}
				if(!(endpoint2.equals(minpos))){
					double help1 = ((Double)weights.get(endpoint2)).doubleValue();
					double help2 = ((Double)weights.get(minpos)).doubleValue();
					if(help1 > help2 + minpos.distance(endpoint2)){
						Double value = new Double(help2 + minpos.distance(endpoint2));
						weights.changeto(endpoint2, value);
						vertices.changeto(endpoint2, value);
						LinkedList<Position> computeway = new LinkedList<Position>();
						for(int j = 0; j < ((LinkedList)ways.get(minpos)).size(); j++){
							computeway.add(((Position)((LinkedList)ways.get(minpos)).get(j)));
						}
						computeway.add(endpoint2);
						ways.changeto(endpoint2, computeway);
					} 
				}
			}
		}

		return ways;
	}

	/*returns a random valid position in the area*/
	public Position getRandomPosition(){
		/*get width and height*/
		int i = 0;
		double x, y;
		Position p = new Position(0,0);
		double width = Math.abs(corners[0].x-corners[2].x);
		double height = Math.abs(corners[0].y-corners[2].y);
		double start_x = Math.min(corners[0].x, corners[2].x);
		double start_y = Math.min(corners[0].x, corners[2].y);
		
		while(i<1000){
			Random r = new Random();
			x = r.nextDouble()*width + start_x;
			y = r.nextDouble()*height + start_y;
			p.x = x; p.y=y;
			if(!isInObstacle(p)) return p;
			i++;
		}
		System.err.println("1000 positions within obstacles --aborting");
		System.exit(1);
		return p;
	}
	
	/*cost is high, don't use it often!*/
	public boolean isInObstacle(Position p){
		for(Obstacle o:this.obstacles)
			if(o.contains(p.x, p.y)) return true;
		return false;
	}
	
	public static int[] findArea(int type, MineArea[] m){
		ArrayList<Integer> l = new ArrayList<Integer>();
		/*check which indexes are the type I want*/
		for(int i = 0; i < m.length; i++){
			if(m[i].type == type) l.add(i);
		}
		int[] ret =  l.stream().mapToInt(i->i).toArray();
		return ret;
	}
	
}
