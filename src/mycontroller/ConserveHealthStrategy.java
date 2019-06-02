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

		Coordinate currPos = new Coordinate(control.getPosition());
		
		ArrayList<Coordinate> path = findPath(currPos, control.finish.get(0));
//		System.out.println(path);
		
		Set<Map.Entry<Coordinate, MapTile>> viewSet = control.getView().entrySet();
		for (Map.Entry<Coordinate, MapTile> item : viewSet) {
			System.out.println(item.getKey() + ", " + item.getValue().getType());
//			if (item.getValue().getType() == MapTile.Type.TRAP) {
//				TrapTile trapTile = (TrapTile) item.getValue();
//				System.out.println(item.getKey().x + ", " + item.getKey().y);
//				System.out.println(trapTile.getTrap());
//			}
			
		}
		System.out.println(control.getView());
//		control.applyForwardAcceleration();
		
		if (control.numParcelsFound() >= control.numParcels()) {
			System.out.println("Heading to exit, have enough parcels!");
		}
	}
	
	// TODO: To be removed later and put somewhere useful
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
