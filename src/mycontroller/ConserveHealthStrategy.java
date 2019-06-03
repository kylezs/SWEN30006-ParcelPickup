package mycontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import tiles.HealthTrap;
import tiles.MapTile;
import tiles.ParcelTrap;
import tiles.TrapTile;
import tiles.WaterTrap;
import utilities.Coordinate;
import world.World;

public class ConserveHealthStrategy implements IMovementStrategy {
	
	MyAutoController control;
	HashSet<Coordinate> emptySet = new HashSet<Coordinate>();
	
	private static final int HEALTH_TRAP_THRESHOLD = 260;
	private static final int WATER_TRAP_THRESHOLD = 150;

	public ConserveHealthStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {
		// general strategy is go to the nearest unseen tile and at any point deviate if a
		// package is seen. As soon as the correct number of packages are found start
		// moving towards exit
		Coordinate currPos = new Coordinate(control.getPosition());
		
		control.updatePathVariables();
		
		// if we have enough packages head to the exit
		if (control.numParcels() <= control.numParcelsFound()) {
			// if we don't have a path to the exit
			System.out.println("Finding path to exit");
			if (control.currentPath == null || !control.isHeadingToFinish) {	
				// find path to exit
				ArrayList<Coordinate> tempPath = null;
				// See if there's a path without needing to go through hazards
				tempPath = control.findPath(currPos, control.finish.get(0), control.hazardsMap.keySet());
				Set<Coordinate> tempHazards = new HashSet<>(control.hazardsMap.keySet());
				// If no path without going through hazards, just go through them
				if (tempPath == null || tempPath.size() == 0) {
					for (Coordinate coord : control.hazardsMap.keySet()) {
						tempHazards.remove(coord);
					}
				}
				// If it's a valid map, there should be a path now
				control.setPath(control.findPath(currPos, control.finish.get(0), tempHazards));
	
				control.isHeadingToFinish = true;
			}
		}
		
		headTowardsPackages(currPos);
		
		exploreUnseenMap(currPos);
		
		MapTile currTile = control.getView().get(currPos);
		boolean isHealth = false;
		if (currTile.getType() == MapTile.Type.TRAP) {
			TrapTile trap = (TrapTile) currTile;
			if (trap.getTrap().equals("health")) {
				isHealth = true;
			}
		}
		if (isHealth && (control.getHealth() < HEALTH_TRAP_THRESHOLD)) {
			// do nothing
			return;
		} else {
			isHealth = false;
			control.moveTowards(control.dest);
		}
		
	}
	
	private void exploreUnseenMap(Coordinate currPos) {
		
		// if we still don't have a path, head to the closest unseen tile (acording to path length)
		if (control.currentPath == null) {
			// advance in spiral around currPos until "close enough" point is found
			// currently just looks at all points and finds closest, room for optimisation
			// advance in spiral around currPos until "close enough" point is found
			// currently just looks at all points and finds closest, room for optimisation
			ArrayList<Coordinate> path = null;
			ArrayList<Coordinate> bestPath = null;
			for (Coordinate coord: control.unseenCoords) {
				Set<Coordinate> tempHazards = new HashSet<>(control.hazardsMap.keySet());
				path = control.findPath(currPos, coord, tempHazards);
				if (path.size() > 0) {
					if (bestPath == null || path.size() < bestPath.size()) {
						bestPath = path;
					}
				}
			}
			
			if (bestPath == null || bestPath.size() == 0) {
				for (Coordinate coord: control.unseenCoords) {
					Set<Coordinate> tempHazards = new HashSet<>(control.hazardsMap.keySet());
					path = control.findPath(currPos, coord, tempHazards);
					for (Coordinate hazard : control.hazardsMap.keySet()) {
						tempHazards.remove(hazard);
						path = control.findPath(currPos, coord, tempHazards);
						if (path.size() > 0) {
							if (bestPath == null || path.size() < bestPath.size()) {
								bestPath = path;
							}
						}
					}

				}
			}
			
			control.setPath(bestPath);
		}
	}
	
	private void headTowardsPackages(Coordinate currPos) {
		// move towards any visible packages if we're not heading to the finish
		HashMap<Coordinate,MapTile> view = control.getView();
		if (!control.isHeadingToFinish) {
			// if we don't have a path to a parcel
			if (control.currentPath == null || !( view.get(control.currentPath.get(0)) instanceof ParcelTrap )) {
				for (Coordinate coord: view.keySet()) {
					// if the tile in view is a parcel make a path to it
					if (view.get(coord) instanceof ParcelTrap) {
						hazardFindPath(currPos, coord);
						return;
					}
				}
				for (Coordinate coord3 : view.keySet()) {
					if (view.get(coord3) instanceof HealthTrap && control.getHealth() < WATER_TRAP_THRESHOLD) {
						hazardFindPath(currPos, coord3);
						return;
					}
				}
				for (Coordinate coord2 : view.keySet()) {
					if (view.get(coord2) instanceof WaterTrap && control.getHealth() < WATER_TRAP_THRESHOLD) {
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
		} else if (tempPath == null || tempPath.size() == 0) {
			
			if (control.getHealth() < 120) {
				// Can be optimised, coordinate-wise removal
				for (Coordinate hazard : control.hazardsMap.keySet()) {
					tempHazards.remove(hazard);
					tempPath = control.findPath(currPos, to, tempHazards);
					if (tempPath.size() > 0) {
						break;
					}
				}
			} else {
				tempPath = control.findPath(currPos, to, new HashSet<Coordinate>());
			}
			tempPath = control.findPath(currPos, to, new HashSet<Coordinate>());
			if (tempPath.size() > 0) {
				control.setPath(tempPath);
			}
		}
	}
}
