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

import java.util.Random;

public class Maintenance extends MineArea {
	private static final long serialVersionUID = -2167557823937697002L;

	public Maintenance(double[] val, double[] entries) {
		super(val, entries, 2);
		this.type = 2;
		makeObstacles();
		//System.out.println ("AreaType: Maintenance");
	}
	
	protected void SetDefaultValues() {
		//TODO
	}
	
	public int[] RandomArray(int total, int trues){
		
		int[] r = new int[total];
		int i=0;
		Random ran = new Random();
		while(i < trues){
			int x = ran.nextInt(total);
			if(r[x] == 1){ /*already written*/
				continue;
			}
			else{ /*we add it*/
				r[x] = 1;
				i++;
			}
		}
		return r;
	}
	
	
	protected Obstacle[] makeObstacles(){
		
		double h_step = this.getBounds().height/10.0;
		double w_step = this.getBounds().width/10.0;
		
		/*random number of obstacles: 2-4*/
		
		Random ran = new Random();
		int x = ran.nextInt(3) + 2;
		//System.out.println(x + " obstáculos");
		
		int[] randarr = RandomArray(8, x);
		System.out.println();
		Obstacle[] ret = new Obstacle[x];
		int ret_i = 0;
		for(int i = 0; i < randarr.length; i++){
			/*create obstacle*/
			if(randarr[i]==1){
				
				int start = ran.nextInt(6) + 1;
				int end = ran.nextInt(9-start) + start + 1;

				//System.out.println(i + ", " + randarr[i] + "; start: " + start + ", end:" + end);

				
				double[] vertices = {
						i*w_step, start*h_step, 
						i*w_step, end*h_step, 
						i*w_step+w_step, end*h_step, 
						i*w_step+w_step, start*h_step};
				Obstacle o = new Obstacle(vertices);
				ret[ret_i] = o;
				ret_i++;
			}
		}
		return ret;
		
	}
}