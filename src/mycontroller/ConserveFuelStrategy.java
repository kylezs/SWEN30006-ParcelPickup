package mycontroller;

import java.util.ArrayList;
import java.util.HashMap;
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
	private ArrayList<Coordinate> currentPath = null;
	private int nextInPath = 0;
	private Coordinate dest;
	private boolean isHeadingToFinish = false;

	public ConserveFuelStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {
		// general strategy is go to the nearest unseen tile and at any point deviate if a
		// package is seen. As soon as the correct number of packages are found start
		// moving towards exit
		Coordinate currPos = new Coordinate(control.getPosition());
		
		// if we have enough packages head to the exit
		if (control.numParcels() <= control.numParcelsFound()) {
			// if we don't have a path to the exit
			if (currentPath == null || !isHeadingToFinish) {	
				// find path to exit
				System.out.println("Finding path to exit");
				setPath(findPath(currPos, control.finish.get(0)));
				// System.out.println(pathToExit.toString());
				isHeadingToFinish = true;
			}
		}
		
		// move towards any visible packages if we're not heading to the finish
		HashMap<Coordinate,MapTile> view = control.getView();
		if (!isHeadingToFinish) {
			// if we don't have a path to a parcel
			if (currentPath == null || !( view.get(currentPath.get(0)) instanceof ParcelTrap )) {
				for (Coordinate coord: view.keySet()) {
					// if the tile in view is a parcel make a path to it
					if (view.get(coord) instanceof ParcelTrap) {
						System.out.println("Deviating towards parcel");
						setPath(findPath(currPos, coord));
						break;
					}
				}
			}
		}
		
		// if we still don't have a path, head to the closest unseen tile (acording to path length)
		if (currentPath == null) {
			// advance in spiral around currPos until "close enough" point is found
			// currently just looks at all points and finds closest, room for optimisation
			ArrayList<Coordinate> path;
			ArrayList<Coordinate> bestPath = null;
			for (Coordinate coord: generateSpiral(currPos)) {
				if (control.unseenCoords.contains(coord)) {
					path = findPath(currPos, coord);
					if (path.size() > 0) {
						if (bestPath == null || path.size() < bestPath.size()) {
							bestPath = path;
						}
					}
				}
			}
			setPath(bestPath);
		}
		
		// now we will certainly have a path to follow
		
		// did we make it to the dest?
		if (currPos.equals(dest)) {
			nextInPath++;
		}
		
		// have we finished the path?
		if (nextInPath == currentPath.size()) {
			System.out.println("Made it to destination");
			// reset the path and stop
			resetPath();
			control.applyBrake();
		}
		
		// if we didn't just reset the path update the destination coord
		if (currentPath != null) {
			updateDest();
			// move towards the path
			control.moveTowards(dest);
		}
		
	}
	
	private ArrayList<Coordinate> findPath(Coordinate start, Coordinate dest){
		// perform BFS on the map
		ArrayList<Coordinate> path = new ArrayList<>();
		HashMap<Coordinate,MapTile> map = control.getMap();
		HashMap<Coordinate, Node> node_map = new HashMap<>();
		for (Coordinate coord: map.keySet()) {
			node_map.put(coord, new Node(coord, null));
		}
		
		Queue<Node> queue = new ArrayDeque<>();
		Node start_node = node_map.get(start);
		start_node.setDiscovered(true);
		queue.add(start_node);
		
		while(queue.size() > 0) {
			Node curr = queue.remove();
			if (curr.getCoord().equals(dest)) {
				// build path
				path.add(curr.getCoord());
				while (curr.getFrom() != null && !curr.getFrom().getCoord().equals(start)) {
					path.add(curr.getFrom().getCoord());
					curr = curr.getFrom();
				}
				// we're done, break and return path
				break;
			}
			
			// find all valid adjacent tiles to curr
			int x = curr.getCoord().x;
			int y = curr.getCoord().y;
			
			int[][] xy_changes = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
			for (int[] dxdy: xy_changes) {
				Coordinate next_coord = new Coordinate(x + dxdy[0], y + dxdy[1]);
				MapTile tile = map.get(next_coord);
				
				// if the coord corresponds to a non-wall tile
				if (tile != null && !tile.isType(MapTile.Type.WALL)) {
					Node next_node = node_map.get(next_coord);
					// if undiscovered and is a drivable tile
					if (! next_node.isDiscovered()) {
						next_node.setDiscovered(true);
						next_node.setFrom(curr);
						queue.add(next_node);
					}
				}
			}
			
		}
		
		return path;
	}
	
	private void setPath(ArrayList<Coordinate> path) {
		assert(path != null);
		
		control.applyBrake();
		this.currentPath = path;
		this.nextInPath = 0;
		
		
		System.out.println("Path set towards: " + path.get(0));
		System.out.println(path.toString());
		
		updateDest();
	}
	
	private void resetPath() {
		control.applyBrake();
		this.currentPath = null;
		this.nextInPath = 0;
		System.out.println("Path reset");
	}
	
	private void updateDest() {
		this.dest = currentPath.get(currentPath.size() - nextInPath - 1);
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
