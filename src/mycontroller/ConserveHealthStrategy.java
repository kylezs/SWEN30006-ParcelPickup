package mycontroller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import tiles.MapTile;
import tiles.TrapTile;
import utilities.Coordinate;
import world.World;

public class ConserveHealthStrategy implements IMovementStrategy {
	
	private boolean isHeadingToFinish = false;
	MyAutoController control;
	
	// Init at same position, find next place to go
	Coordinate currPos;
	// The end point to find a path towards
	String currTileToType = "road";

	public ConserveHealthStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {
		
		if (control.currentPath != null) {
			// did we make it to the control.dest?
			if (currPos.equals(control.dest)) {
				control.nextInPath++;
			}
			
			// have we finished the path?
			if (control.nextInPath == control.currentPath.size()) {
				System.out.println("Made it to control.destination");
				// reset the path and stop
				control.resetPath();
			} else {
				control.updateDest();
			}
		}
		
		currPos = new Coordinate(control.getPosition());
		
		Set<Map.Entry<Coordinate, MapTile>> viewSet = control.getView().entrySet();

		

		String trapType = "";
		System.out.println("control.currentPath: " + control.currentPath);
		// If these are the same, find a new path. 
		if ((currPos.equals(control.dest) || control.dest == null) && !isHeadingToFinish) {
			System.out.println("Add a path");
			for (Map.Entry<Coordinate, MapTile> item : viewSet) {
				Coordinate coord = item.getKey();
				MapTile mapTile = item.getValue();
				boolean isTrap = mapTile.getType() == MapTile.Type.TRAP;
				if (isTrap) {
					trapType = (String) ((TrapTile) mapTile).getTrap();
				}
				// Go for health first
				if (trapType.equals("water")) {
					System.out.println("Setting path to water tile at " + coord);
					control.currentPath = control.findPath(currPos, coord, control.hazardsMap.keySet());
					currTileToType = "water";
					break;
				} else if (trapType.equals("parcel")) {
					System.out.println("Setting path to parcel at " + coord);
					control.currentPath = control.findPath(currPos, coord, control.hazardsMap.keySet());
					currTileToType = "parcel";
					break;
				}
			}
			
			if (control.currentPath == null || control.currentPath.size() == 0) {
				System.out.println("Setting new road path");
				// advance in spiral around currPos until "close enough" point is found
				// currently just looks at all points and finds closest, room for optimisation
				ArrayList<Coordinate> path;
				ArrayList<Coordinate> bestPath = null;
				for (Coordinate coord: generateSpiral(currPos)) {
					if (control.unseenCoords.contains(coord)) {
						path = control.findPath(currPos, coord, control.hazardsMap.keySet());
						if (path.size() > 0) {
							if (bestPath == null || path.size() < bestPath.size()) {
								bestPath = path;
							}
						}
					}
				}
				control.setPath(bestPath);
			}

		} else {
			// Path has already been computed. Recompute, in case it's stuck
			if (!(control.currentPath == null || control.currentPath.isEmpty())) {
				control.currentPath = control.findPath(currPos, control.currentPath.get(0), control.hazardsMap.keySet());
			}
			
		}
		
//		// TODO can be optimised
//		if (control.numParcelsFound() >= control.numParcels()) {
//			System.out.println("Heading to exit, have enough parcels!");
//			isHeadingToFinish = true;
//			control.currentPath = control.findPath(currPos, control.finish.get(0), control.hazardsMap.keySet());
//		}
		
		// did we make it to the dest?
		if (currPos.equals(control.dest)) {
			control.nextInPath++;
		}
		
		// have we finished the path?
		if (control.nextInPath == control.currentPath.size()) {
			System.out.println("Made it to destination");
			// reset the path and stop
			control.resetPath();
			control.applyBrake();
		}
		
		// if we didn't just reset the path update the destination coord
		if (control.currentPath != null) {
			control.updateDest();
			// move towards the path
			control.moveTowards(control.dest);
		}
		
	}
	
	// generates a spiral of Coordinates around a specified start in the anticlockwise direction
	// NOTE: many points in the output array will not be valid Coordinates in the map
	private ArrayList<Coordinate> generateSpiral(Coordinate start) {
		ArrayList<Coordinate> spiral = new ArrayList<>();
		int dx = 1;
		int signX = 1;
		int dy = 1;
		int signY = 1;
		Coordinate temp = new Coordinate(start.toString());
		
		while(start.x + dx <= World.MAP_WIDTH || start.x - dx >= 0 ||
				start.y + dy <= World.MAP_HEIGHT || start.y - dy >= 0) {
			for (int i = 0; i < dx; i++) {
				temp.x += signX;
				spiral.add(new Coordinate(temp.toString()));
			}
			for (int i = 0; i < dy; i++) {
				temp.y += signY;
				spiral.add(new Coordinate(temp.toString()));
			}
			dx++;
			signX *= -1;
			dy++;
			signY *= -1;
		}
		
		return spiral;
	}


}
