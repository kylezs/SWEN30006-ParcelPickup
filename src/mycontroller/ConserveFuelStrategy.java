package mycontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import tiles.HealthTrap;
import tiles.MapTile;
import tiles.ParcelTrap;
import tiles.TrapTile;

import java.util.Queue;
import java.util.AbstractMap;
import java.util.ArrayDeque;

import utilities.Coordinate;
import world.World;

public class ConserveFuelStrategy implements IMovementStrategy {
	
	MyAutoController control;
	HashSet<Coordinate> emptySet = new HashSet<Coordinate>();
	
	private static final int LEAVE_HEALTH_TRAP_THRESHOLD = 200;
	private static final int CRITICAL_HEALTH_THRESH = 150;
	private static final int PRIORITISE_HEALTH_PATH_THRESH = 2;

	public ConserveFuelStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {
		// general strategy is go to the nearest unseen tile and at any point deviate if a
		// package is seen. As soon as the correct number of packages are found start
		// moving towards exit
		Coordinate currPos = new Coordinate(control.getPosition());
		
		// update path and associated variables 
		control.updatePathVariables();
		
		// if we have enough packages head to the exit
		if (control.numParcels() <= control.numParcelsFound()) {
			// if we don't have a path to the exit
			if (control.currentPath == null || !control.isHeadingToFinish) {	
				// find path to exit
//				System.out.println("Finding path to exit");
				control.setPath(control.findPath(currPos, control.finish.get(0), emptySet));
				control.isHeadingToFinish = true;
			}
		}
		
		// move towards any visible packages if we're not heading to the finish
		moveTowardsParcels();
		
		// if we still don't have a path, head to the closest unseen tile (according to path length)
		if (control.currentPath == null) {
			moveTowardsUnseen();
		}
		
		// if currently on a health trap, sit there until we have plenty of health
		boolean isHealth = false;
		if (control.getView().get(currPos) instanceof HealthTrap) {
			isHealth = true;
		}
		if (isHealth && control.getHealth() < LEAVE_HEALTH_TRAP_THRESHOLD) {
			// do nothing
			control.applyBrake();
		} else {
			control.moveTowards(control.dest);
		}
	}
	
	private void moveTowardsParcels() {
		Coordinate currPos = new Coordinate(control.getPosition());
		HashMap<Coordinate,MapTile> view = control.getView();
		
		if (!control.isHeadingToFinish) {
			// if we don't have a path to a parcel
			if (control.currentPath == null || !( view.get(control.currentPath.get(0)) instanceof ParcelTrap )) {
				for (Coordinate coord: view.keySet()) {
					// if the tile in view is a parcel make a path to it
					if (view.get(coord) instanceof ParcelTrap) {
						ArrayList<Coordinate> tempPath = control.findPath(currPos, coord, emptySet);
						if (tempPath.size() > 0) {
//							System.out.println("Deviating towards parcel");
							control.setPath(tempPath);
							break;
						}
					} else if (view.get(coord) instanceof HealthTrap
							&& control.getHealth() < CRITICAL_HEALTH_THRESH) {
						ArrayList<Coordinate> tempPath = control.findPath(currPos, coord, emptySet);
						if (tempPath.size() > 0) {
							control.setPath(tempPath);
						}
					}
				}
			}
		}
	}
	
	private void moveTowardsUnseen() {
		Coordinate currPos = new Coordinate(control.getPosition());
		
		ArrayList<Coordinate> path;
		ArrayList<Coordinate> bestPath = null;
		ArrayList<Coordinate> bestHazardFreePath = null;
		for (Coordinate coord: control.unseenCoords) {
			path = control.findPath(currPos, coord, emptySet);
			if (path.size() > 0) {
				if (bestPath == null || path.size() < bestPath.size()) {
					bestPath = path;
				}
			}
			path = control.findPath(currPos, coord, control.hazardsMap.keySet());
			if (path.size() > 0) {
				if (bestHazardFreePath == null || path.size() < bestHazardFreePath.size()) {
					bestHazardFreePath = path;
				}
			}
		}
		
		control.setPath(bestPath);
		if (bestHazardFreePath != null) {
			int sizeDiff = bestHazardFreePath.size() - bestPath.size();
			if (control.getHealth() < CRITICAL_HEALTH_THRESH ||  sizeDiff <= PRIORITISE_HEALTH_PATH_THRESH) {
				control.setPath(bestHazardFreePath);
			}
		}
	}
}
