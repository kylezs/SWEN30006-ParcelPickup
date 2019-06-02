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
	
	private ArrayList<Coordinate> currPath = new ArrayList<>();
	private boolean isHeadingToFinish = false;
	MyAutoController control;
	
	// Init at same position, find next place to go
	Coordinate currPos;
	// The end point to find a path towards
	Coordinate currGoingTo = null;
	String currTileToType = "road";

	public ConserveHealthStrategy(MyAutoController myAutoController) {
		this.control = myAutoController;
	}

	@Override
	public void move() {
		
		currPos = new Coordinate(control.getPosition());
		
		Set<Map.Entry<Coordinate, MapTile>> viewSet = control.getView().entrySet();
		
		// If these are the same, find a new path. 
		String trapType = "";
		System.out.println("CurrPath: " + currPath);
		if (currPos.equals(currGoingTo) || currGoingTo == null) {
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
					currPath = findPath(currPos, coord);
					currGoingTo = currPath.get(0);
					currTileToType = "water";
					break;
				} else if (trapType.equals("parcel")) {
					System.out.println("Setting path to parcel at " + coord);
					currPath = findPath(currPos, coord);
					currGoingTo = currPath.get(0);
					currTileToType = "parcel";
					break;
				}
			}
			
			if (currPath.size() == 0 || currPath == null) {
				System.out.println("Setting new road path");
				// if there are no priority tiles found, just find a path
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
				currPath = bestPath;
				currGoingTo = currPath.get(0);
				currTileToType = "road";
			}

		}
		
		
		// TODO can be optimised
		if (control.numParcelsFound() >= control.numParcels()) {
			System.out.println("Heading to exit, have enough parcels!");
			isHeadingToFinish = true;
			currPath = findPath(currPos, control.finish.get(0));
		}
		
		// Take next step on the path that's been set
		control.moveTowards(currPath.get(currPath.size() - 1));
		currPath.remove(currPath.size() - 1);
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


}
