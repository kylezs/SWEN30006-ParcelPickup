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

public class ConserveHealthStrategy implements IMovementStrategy {
	
	
	
	MyAutoController control;

	public ConserveHealthStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {

		// Init at same position, find next place to go
		Coordinate currPos = new Coordinate(control.getPosition());
		Coordinate currGoing = new Coordinate(control.getPosition());
		
		
		ArrayList<Coordinate> currPath = new ArrayList<>();
		
		
		Set<Map.Entry<Coordinate, MapTile>> viewSet = control.getView().entrySet();
		
		// Set the next go to
		if (currGoing.equals(currPos)) {
			// Go through all tiles, see if there's a parcel in the current view that we can collect
			for (Map.Entry<Coordinate, MapTile> item : viewSet) {
				Coordinate tileCoord = item.getKey();
				MapTile tile = item.getValue();
				System.out.println(tileCoord + ", " + item.getValue().getType());
				if (tile.getType() == MapTile.Type.TRAP) {
					TrapTile trapTile = (TrapTile) item.getValue();
					String trapType = trapTile.getTrap();
					if (trapType.equals("parcel")) {
						System.out.println("Got to the trap analysis");
						currGoing = tileCoord;
						currPath = findPath(currPos, tileCoord);
					} else if (trapType.equals("health")) {
						currGoing = tileCoord;
						currPath = findPath(currPos, tileCoord);
						System.out.println("Health trap");
					}
				}
			}
		}
		
		for (Coordinate nextCoord : currPath) {
			System.out.println("Hello");
			control.moveTowards(currPos, nextCoord);
		}
		
		
		
		System.out.println(control.getView());
//		control.applyForwardAcceleration();
		
		if (control.numParcelsFound() >= control.numParcels()) {
			System.out.println("Heading to exit, have enough parcels!");
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
