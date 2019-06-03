package mycontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import tiles.MapTile;
import tiles.ParcelTrap;

import java.util.Queue;
import java.util.AbstractMap;
import java.util.ArrayDeque;

import utilities.Coordinate;
import world.World;

public class ConserveFuelStrategy implements IMovementStrategy {
	
	MyAutoController control;
	HashSet<Coordinate> emptySet = new HashSet<Coordinate>();

	public ConserveFuelStrategy(MyAutoController myAutoController) {
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
			if (control.currentPath == null || !control.isHeadingToFinish) {	
				// find path to exit
				System.out.println("Finding path to exit");
				control.setPath(control.findPath(currPos, control.finish.get(0), emptySet));
				// System.out.println(pathToExit.toString());	
				control.isHeadingToFinish = true;
			}
		}
		
		// move towards any visible packages if we're not heading to the finish
		HashMap<Coordinate,MapTile> view = control.getView();
		if (!control.isHeadingToFinish) {
			// if we don't have a path to a parcel
			if (control.currentPath == null || !( view.get(control.currentPath.get(0)) instanceof ParcelTrap )) {
				for (Coordinate coord: view.keySet()) {
					// if the tile in view is a parcel make a path to it
					if (view.get(coord) instanceof ParcelTrap) {
						ArrayList<Coordinate> tempPath = control.findPath(currPos, coord, emptySet);
						if (tempPath.size() > 0) {
							System.out.println("Deviating towards parcel");
							control.setPath(tempPath);
							break;
						}
					}
				}
			}
		}
		
		// if we still don't have a path, head to the closest unseen tile (acording to path length)
		if (control.currentPath == null) {
			// advance in spiral around currPos until "close enough" point is found
			// currently just looks at all points and finds closest, room for optimisation
			ArrayList<Coordinate> path;
			ArrayList<Coordinate> bestPath = null;
			for (Coordinate coord: control.unseenCoords) {
				path = control.findPath(currPos, coord, emptySet);
				if (path.size() > 0) {
					if (bestPath == null || path.size() < bestPath.size()) {
						bestPath = path;
					}
				}
			}
			control.setPath(bestPath);
		}
		
		control.moveTowards(control.dest);
	}
}
