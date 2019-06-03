package mycontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import tiles.MapTile;
import tiles.ParcelTrap;
import tiles.WaterTrap;
import utilities.Coordinate;
import world.World;

public class ConserveHealthStrategy implements IMovementStrategy {
	
	MyAutoController control;
	HashSet<Coordinate> emptySet = new HashSet<Coordinate>();

	public ConserveHealthStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {
		// general strategy is go to the nearest unseen tile and at any point deviate if a
		// package is seen. As soon as the correct number of packages are found start
		// moving towards exit
		Coordinate currPos = new Coordinate(control.getPosition());
		
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
		
		// if we have enough packages head to the exit
		if (control.numParcels() <= control.numParcelsFound()) {
			// if we don't have a path to the exit
			System.out.println("Finding path to exit");
			if (control.currentPath == null || !control.isHeadingToFinish) {	
				// find path to exit
				ArrayList<Coordinate> tempPath = null;
				tempPath = control.findPath(currPos, control.finish.get(0), control.hazardsMap.keySet());
				System.out.println("Size of map: " + control.hazardsMap.keySet().size());
				Set<Coordinate> tempHazards = new HashSet<>(control.hazardsMap.keySet());
				ArrayList<Coordinate> toBeRemoved = new ArrayList<>();
				// TODO: Can be optimized
				if (tempPath == null || tempPath.size() == 0) {
					for (Coordinate coord : control.hazardsMap.keySet()) {
						toBeRemoved.add(coord);
					}
					for (Coordinate rem : toBeRemoved) {
						tempHazards.remove(rem);
					}
				}
				// If it's a valid map, there should be a path now
				control.setPath(control.findPath(currPos, control.finish.get(0), tempHazards));

				// System.out.println(pathToExit.toString());	
				control.isHeadingToFinish = true;
			}
		}
		
		headTowardsPackages(currPos);
		
		// if we still don't have a path, head to the closest unseen tile (acording to path length)
		if (control.currentPath == null) {
			// advance in spiral around currPos until "close enough" point is found
			// currently just looks at all points and finds closest, room for optimisation
			for (Coordinate coord: control.unseenCoords) {
				if (control.unseenCoords.contains(coord)) {
					hazardFindPath(currPos, coord);
				}
			}
		}
		
		control.moveTowards(control.dest);
	}
	
	private void headTowardsPackages(Coordinate currPos) {
		// move towards any visible packages if we're not heading to the finish
		HashMap<Coordinate,MapTile> view = control.getView();
		if (!control.isHeadingToFinish) {
			// if we don't have a path to a parcel
			if (control.currentPath == null || !( view.get(control.currentPath.get(0)) instanceof ParcelTrap )) {
				for (Coordinate coord: view.keySet()) {
					// if the tile in view is a parcel make a path to it
					Set<Coordinate> tempHazards = new HashSet<>(control.hazardsMap.keySet());
					if (view.get(coord) instanceof ParcelTrap) {
						hazardFindPath(currPos, coord);
						return;
					}
				}
				for (Coordinate coord2 : view.keySet()) {
					if (view.get(coord2) instanceof WaterTrap) {
						hazardFindPath(currPos, coord2);
						return;
					}
				}
			}
		}
	}
	
	private void hazardFindPath(Coordinate currPos, Coordinate to) {
		ArrayList<Coordinate> tempPath = new ArrayList<>();
		Set<Coordinate> tempHazards = new HashSet<>(control.hazardsMap.keySet());
		tempPath = control.findPath(currPos, to, tempHazards);
		//Find a path will all hazards in it
		if (tempPath != null && tempPath.size() > 0) {
			control.setPath(tempPath);
			return;
		} else if (tempPath == null || tempPath.size() == 0){
			// Can be optimised, coordinate-wise removal
			ArrayList<Coordinate> bestPath = new ArrayList<>();
			for (Coordinate hazard : control.hazardsMap.keySet()) {
				tempHazards.remove(hazard);
				tempPath = control.findPath(currPos, to, tempHazards);
				
				if (tempPath.size() > 0) {
					if (bestPath == null || tempPath.size() < bestPath.size()) {
						bestPath = tempPath;
					}
				}
			}
			if (bestPath != null && bestPath.size() > 0) {
				control.setPath(tempPath);
				return;
			}
		}
	}
	
	
	// generates a spiral of Coordinates around a specified start in the anticlockwise direction
	// NOTE: many points in the output array will not be valid Coordinates in the map
	private ArrayList<Coordinate> generateSpiral(Coordinate start){
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
