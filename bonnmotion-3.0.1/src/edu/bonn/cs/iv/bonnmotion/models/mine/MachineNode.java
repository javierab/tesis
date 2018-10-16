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

import edu.bonn.cs.iv.bonnmotion.Position;
import edu.bonn.cs.iv.bonnmotion.Waypoint;
import java.util.LinkedList;
import java.util.HashMap;

/** Mine node */

public class MachineNode extends MineNode {
	public Position dump;
	public Position ext;
	public int rep;
	
	int EXTRACTION = 0;
	int DUMP = 1;
	
	/* * * * * * * * * * *
	 * area:
	 * 0: access 
	 * 1: extraction
	 * 2: maintenance
	 * * * * * * * * * * *
	 * type:
	 * 0: maquina: movimiento circular, solo en area de extraccion
	 * 1: mantenimiento: movimiento entre areas de mantencion
	 * 2: supervisor: se mueve por todas las areas
	 * * * * * * * * * * */
	
	public MachineNode(Position start, MineArea area) {
		super(start);
		this.type = 0;
		if(area.type == 1){
			this.current_area = area;
		}
		else{
			System.out.println("Machines can only be located in Extraction areas");
			System.exit(1);
		}
		/*values for machines*/
//		this.min_speed = 2.0;
//		this.max_speed = 3.0;
//		this.pause_dump = 10;
//		this.pause = 12;
//		
		/*set points and repetitions for extraction*/
		this.dump = ((Extraction)this.current_area).getDump();
		this.ext = current_area.getRandomPosition();
		this.repetitions = 5 + r.nextInt(3) -1;
		
		/*set values for route for time */
		this.pause = getPause();
		this.state = EXTRACTION;
		this.step = 0;
		this.step_time = 0;
		//this.route = getRoute();

	}
	
	public void add(Position start) {
		this.start = start;
	}
	
	public void print() {
		System.out.println("MachineNode: \n --position:" + dest_position.toString() + "\n --dump:" + dump.toString());
	}
	
	public HashMap<Double, Position> getNextStep(){
		//System.out.println("--nextstep-- state: " + state + ", pause: " + pause + ", timeout: " + timeout + ", step: " + step + ", current_position: " + current_position + ", dest_position: " + dest_position);
		/**/
		HashMap<Double, Position> rt = new HashMap<Double, Position>();
		if(state == EXTRACTION){ /*pause and define destination to dump */
			step_time+= pause;
			rep++;
			current_position = ext;
			dest_position = dump;
			rt = getRoute();
			state = DUMP;
		}
		if(state == DUMP){ /*pause, see if enough extractions and go to ext*/
			step_time+=pause;
			current_position = dump;
			if(rep == repetitions){ /*find new ext*/
				ext = current_area.getRandomPosition();
			}
			dest_position = ext;
			rt = getRoute();
			state = EXTRACTION;
		}
		return rt;
	}
//		if(state == START){
//			step_time += pause;
//			rep = 0;
//			state = PAUSE_EXT;
//		}
//		else if(state == TO_DUMP){
//			current_position = dest_position;
//			rep++;
//		}
//		
//		
//		if(state == START){ /*start. timeout = pause-1, 0 reps.*/
//			state = PAUSE_EXT;
//			timeout = pause -1;
//			step = 0;
//			rep_t = 0;
//		}
//		else if(state == TO_DUMP){ /*going to dump*/
//			step++;
//			if(step >= route.size()-1){/*i'm here*/
//				step = 0;
//				timeout = 0;
//				current_position = dest_position;
//				state = PAUSE_DUMP;
//			}
//			else{
//				current_position =  route.get(step);
//			}
//		}
//		else if(state == TO_EXT){
//			step++;
//			if(step >= route.size()-1){/*i'm here*/
//				step = 0;
//				timeout = 0;
//				current_position = dest_position;
//				state = PAUSE_EXT;
//			}
//			else{
//				current_position =  route.get(step);
//			}
//		}
//		else if(state == PAUSE_DUMP || state == PAUSE_EXT){
//			timeout++;
//			if(timeout == pause){
//				timeout = 0;
//				getNextDestination();
//			}
//		}
//		return current_position;
//	}	
//	
//	public void getNextDestination(){
//		if(state == PAUSE_EXT){
//			current_position = dest_position;
//			dest_position = dump;
//			step = 0;
//			state = TO_DUMP;
//		}
//		else if(state == PAUSE_DUMP){
//			rep_t++;
//			if(rep_t == repetitions){ /*find new ext point to go*/
//				ext = current_area.getRandomPosition();
//				rep_t = 0;
//			}
//			current_position = dest_position;
//			dest_position = ext;
//			step = 0;
//			state = TO_EXT;
//		}
//		else{
//			System.out.println("shouldn't find new destination while on the way");
//			System.exit(0);
//		}
//		route = getRoute();
//	}
//	
		
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

}
