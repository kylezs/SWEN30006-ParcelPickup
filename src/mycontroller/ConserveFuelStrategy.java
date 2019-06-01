package mycontroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import tiles.MapTile;

import java.util.Queue;
import java.util.AbstractMap;
import java.util.ArrayDeque;

import utilities.Coordinate;

public class ConserveFuelStrategy implements IMovementStrategy {
	
	MyAutoController control;
	private ArrayList<Coordinate> currentPath = null;
	private int nextInPath = 0;

	public ConserveFuelStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {
		// general strategy is
		Coordinate currPos = new Coordinate(control.getPosition());
		Coordinate dest;
		Coordinate nextDest;
		
		// find path to exit
		if (currentPath == null) {
			//currentPath = findPath(currPos, control.finish.get(3));
			currentPath = findPath(currPos, new Coordinate(2, 5));
			nextInPath = 0;
			System.out.println(currentPath.toString());	
		}
		
		
		
		if (currentPath != null) {
			// follow the path
			dest = currentPath.get(currentPath.size() - nextInPath - 1);
			if (currentPath.size() - nextInPath - 2 >= 0) {
				nextDest = currentPath.get(currentPath.size() - nextInPath - 2);
			} else {
				nextDest = null;
			}
			// move towards the path
			control.moveTowards(dest, nextDest);
			if (currPos.equals(dest)) {
				nextInPath++;
			}
			if (nextInPath == currentPath.size()) {
				// we're done, reset the path
				nextInPath = 0;
				currentPath = null;
			}
			
		} else {
			if (control.numParcels() > control.numParcelsFound()) {
				
			} else {
				// find path to exit
				currentPath = findPath(currPos, control.finish.get(0));
				// System.out.println(pathToExit.toString());	
			}
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
				while (curr.getFrom() != null) {
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
	 

}
