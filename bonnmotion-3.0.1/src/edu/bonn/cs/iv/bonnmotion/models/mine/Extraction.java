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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.bonn.cs.iv.bonnmotion.Position;

public class Extraction extends MineArea {
	private static final long serialVersionUID = 7750320765927287614L;
	int ndumps;
	public int n_machines = 0;
	Position[] dumps;
	

	public Extraction(double[] val, double[] entries) {
		super(val, entries, 1);
		makeObstacles();
		//System.out.println ("AreaType: Extraction");
	}

	public Extraction(double[] val, double[] entries, int ndumps, Position[] dumps) {
		super(val, entries, 1);
		this.ndumps = ndumps;
		this.dumps = dumps;
		if (debug) System.out.println ("AreaType: Extraction");
	}
	
	protected void SetDefaultValues() {
		//TODO
	}
	
	public void setDumps(int ndumps, Position[] dumps){
		this.ndumps = ndumps;
		this.dumps = dumps;
	}
	
	public Position getDump(){

		Random ran = new Random();
		int x = ran.nextInt(ndumps);
		return dumps[x];
	}
	
	protected Obstacle[] makeObstacles(){
		
		/* it's a rectangle:*/
		double height = this.getBounds().height;
		double width = this.getBounds().width;
		List<Obstacle> obstacles = new ArrayList<Obstacle>();
		/*TODO: fixed number? should i scale?*/
		
		double h_step = height/11;
		double w_step = width/15;
		
		for (int i = 0 ; i < 15; i++){
			if((i & 1) == 0){
				; /*even numbers are roads*/
			}
			else{
				new Random();
				int[] breaks = new int[11];
				for(int j=1; j < 10; j++){ /*borders are empty*/
					if((j & 1) == 0){ /*always occupied*/
						breaks[j] = 0;
					}
					else{
						double m = Math.random();
						if(m > 0.5) 
							breaks[j] = 1;
						else 
							breaks[j] = 0;
					}
				}
				breaks[0] = 1; breaks[10] = 1;
				obstacles.addAll(buildObstacles(h_step, w_step, w_step*i, breaks));
			}
		}
		/*return as array*/
		Obstacle[] ret = new Obstacle[obstacles.size()];
		obstacles.toArray(ret);
		return ret;
		
	}
	
	public List<Obstacle> buildObstacles(double h_step, double w_step, double w_start, int[] breaks){
		boolean state = false;
		double h_start = 0;
		List<Obstacle> obs = new ArrayList<Obstacle>();
		
		for(int i = 0; i< breaks.length ; i++){
			//System.out.println("State for i: " + i + " = " + state);
			if(breaks[i] == 0 && state == false){ h_start = i; state = true;}
			else if(breaks[i] == 1 && state == true){
				/*create obstacle*/
				double[] vertices = {h_start*h_step, w_start, i*h_step, w_start, i*h_step, w_start+w_step, h_start*h_step, w_start+w_step};
				Obstacle o = new Obstacle(vertices);
				/*append to return*/
				obs.add(o);
				/*reset*/
				state = false;
			}
		}
		return obs;
	}
	
	public String toString(){
		return super.toString();
	}
	
	public static int getAreaZone(){

		Random ran = new Random();
		return ran.nextInt(2);
	}
//	
//	
//	public static void main(String[] args){
//		
//		/*M1*/
//		double[] m1_c = {0.0, 350.0, 0.0, 800.0, 300.0, 800.0, 300.0, 350.0};
//		double[] m1_e = {200.0, 800.0};
//		/*M2*/
//		double[] m2_c = {950.0, 500.0, 950.0, 800.0, 1500.0, 800.0, 1500.0, 500.0};
//		double[] m2_e = {1000.0, 800.0, 1100.0, 800.0};
//		/*M3*/
//		double[] m3_c = {2500.0, 600.0, 2500.0, 600.0, 2800.0, 600.0, 2800.0, 600.0};
//		double[] m3_e = {2600.0, 800.0, 2780.0, 800.0};
//		
//		/*E1*/
//		double[] e1_c = {300.0, 0.0, 300.0, 800.0, 900.0, 800.0, 900.0, 0.0};
//		double[] e1_e = {400.0, 800.0, 700.0, 800.0};
//		/*E2*/
//		double[] e2_c = {1500.0, 300.0, 1500.0, 800.0, 2400.0, 800.0, 1500.0, 800.0 };
//		double[] e2_e = {2000.0, 800.0};
//		
//		/*A1*/
//		double[] a1_c = {0.0, 800.0, 0.0, 1000.0, 3400.0, 1000.0, 3400.0, 800.0};
//		double[] a1_e = {200.0, 800.0, 1000.0, 800.0, 1100.0, 800.0, 2600.0, 800.0, 2780.0, 800.0, 400.0, 800.0, 700.0, 800.0, 2000.0, 800.0};
//		
//		
//		MineArea e1 = new Extraction(e1_c, e1_e);
//		MineArea e2 = new Extraction(e2_c, e2_e);
//
//		MineArea m1 = new Maintenance(m1_c, m1_e);
//		MineArea m2 = new Maintenance(m2_c, m2_e);
//		MineArea m3 = new Maintenance(m3_c, m3_e);
//
//		MineArea a1 = new Access(a1_c, a1_e);
//
//		System.out.println("--------EXT---------");
//		e1.print();
//		System.out.println("--------------------");
//		e2.print();
//		System.out.println("--------MAN---------");
//		m1.print();
//		System.out.println("--------------------");
//		m2.print();
//		System.out.println("--------------------");
//		m3.print();
//		System.out.println("---------ACC--------");
//		a1.print();
//		System.out.println("--------------------");}
//	
}