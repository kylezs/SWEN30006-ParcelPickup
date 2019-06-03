package mycontroller;

import controller.CarController;
import exceptions.UnsupportedModeException;
import swen30006.driving.Simulation;
import world.Car;
import world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;

import tiles.LavaTrap;
import tiles.MapTile;
import utilities.Coordinate;
import world.WorldSpatial;

public class MyAutoController extends CarController {		
		// How many minimum units the wall is away from the player.

		private int wallSensitivity = 1;
		
		private boolean isFollowingWall = false; // This is set to true when the car starts sticking to a wall.
		private boolean isGoingForward = false;
		private boolean isBackingFromWall = false;
		private boolean isDrivingFromWall = false;

		protected ArrayList<Coordinate> currentPath = null;
		protected int nextInPath = 0;
		protected Coordinate dest;
		protected boolean isHeadingToFinish = false;
		
		// stores the locations of the exit tiles
		protected ArrayList<Coordinate> finish = new ArrayList<Coordinate>();
		protected ArrayList<Coordinate> unseenCoords = new ArrayList<Coordinate>();
		protected HashMap<Coordinate,MapTile> map;
		protected HashMap<Coordinate,MapTile> hazardsMap = new HashMap<>();
		
		// Car Speed to move at
		private final int CAR_MAX_SPEED = 1;
		private IMovementStrategy movementStrategy;
		
		public MyAutoController(Car car) throws UnsupportedModeException {
			super(car);
			System.out.println("Conserving: " + Simulation.toConserve());
			switch (Simulation.toConserve()) {
			case HEALTH:
				this.movementStrategy = new ConserveHealthStrategy(this);
				break;
			case FUEL:
				this.movementStrategy = new ConserveFuelStrategy(this);
				break;
			default:
				throw new UnsupportedModeException();
			}
			
			// find finish tiles
			map = this.getMap();
			for(Coordinate coord : map.keySet()){
				if (map.get(coord).isType(MapTile.Type.FINISH)) {
					finish.add(coord);
				}
				if (!map.get(coord).isType(MapTile.Type.WALL)) {
					unseenCoords.add(coord);
				}
			}
		}
		
		// Coordinate initialGuess;
		// boolean notSouth = true;
		@Override
		public void update() {
			// mark all tiles in view as seen
			HashMap<Coordinate,MapTile> view = getView();
			for (Coordinate coord: view.keySet()) {
				if (unseenCoords.contains(coord)) {
					unseenCoords.remove(coord);
					// update the tile in our map
					if (! view.get(coord).equals(map.get(coord))) {
						map.remove(coord);
						map.put(coord, view.get(coord));
					}
				}
				if (view.get(coord) instanceof LavaTrap) {
					hazardsMap.put(coord, view.get(coord));
				}
			}
			
			this.movementStrategy.move();
		}
		
		// assumes dest is one step in a cardinal direction away from current position
		protected void moveTowards(Coordinate dest) {
			Coordinate coord = new Coordinate(getPosition());
			
			// for when a path ended right in front of a wall facing it and the new path required
			// an immediate turn
			if (isBackingFromWall || isDrivingFromWall) {
				System.out.println("is backing from wall or driving from wall");
				if (getSpeed() != 0) {
					 this.applyBrake();
				} else if (isBackingFromWall){
					this.applyForwardAcceleration();
					isBackingFromWall = false;	
				} else if (isDrivingFromWall){
					this.applyReverseAcceleration();
					isDrivingFromWall = false;	
				}
				return;
			}
			
			// if the dest is one step away from currPos
			if (Math.abs(coord.x - dest.x) + Math.abs(coord.y - dest.y) == 1) {
				System.out.println("If the dest is one step away");
				myRelativeDirection reqRelDir = requiredRelativeDirection(dest);
				if (reqRelDir == myRelativeDirection.LEFT || reqRelDir == myRelativeDirection.RIGHT) {
					// if we need to turn but are not moving
					if (getSpeed() == 0) {
						// if there is a wall ahead, move backwards and indicate this happened
						// note: this assumes there cannot be both a wall in front and behind
						if(checkWallAhead(this.getOrientation(), this.getView())) {
							this.applyReverseAcceleration();
							isBackingFromWall = true;
						} else {
							this.applyForwardAcceleration();
							isDrivingFromWall = true;
						}
					} else {
						turnTowards(reqRelDir);
					}
				} else if (reqRelDir == myRelativeDirection.FORWARD) {
					this.applyForwardAcceleration();
				} else if (reqRelDir == myRelativeDirection.BACKWARD) {
					System.out.println("Applying reverse acceleration");
					this.applyReverseAcceleration();
				} else {
					System.out.println("Movetowards couldn't move the car");
				}
			}
		}
		
		private void turnTowards(myRelativeDirection requiredDir) {
			if (requiredDir == myRelativeDirection.LEFT) {
				turnLeft();
			} else if (requiredDir == myRelativeDirection.RIGHT) {
				turnRight();
			}
		}
		
		private static enum myRelativeDirection { LEFT, RIGHT, FORWARD, BACKWARD};
		
		private myRelativeDirection requiredRelativeDirection(Coordinate dest) {
			WorldSpatial.Direction curDir = getOrientation();
			Coordinate curPos = new Coordinate(getPosition());
			switch (curDir) {
			case EAST:
				if (curPos.x + 1 == dest.x) return myRelativeDirection.FORWARD;
				if (curPos.x - 1 == dest.x) return myRelativeDirection.BACKWARD;
				if (curPos.y + 1 == dest.y) return myRelativeDirection.LEFT;
				if (curPos.y - 1 == dest.y) return myRelativeDirection.RIGHT;
				
			case NORTH:
				if (curPos.x + 1 == dest.x) return myRelativeDirection.RIGHT;
				if (curPos.x - 1 == dest.x) return myRelativeDirection.LEFT;
				if (curPos.y + 1 == dest.y) return myRelativeDirection.FORWARD;
				if (curPos.y - 1 == dest.y) return myRelativeDirection.BACKWARD;
				
			case WEST:
				if (curPos.x + 1 == dest.x) return myRelativeDirection.BACKWARD;
				if (curPos.x - 1 == dest.x) return myRelativeDirection.FORWARD;
				if (curPos.y + 1 == dest.y) return myRelativeDirection.RIGHT;
				if (curPos.y - 1 == dest.y) return myRelativeDirection.LEFT;
				
			case SOUTH:
				if (curPos.x + 1 == dest.x) return myRelativeDirection.LEFT;
				if (curPos.x - 1 == dest.x) return myRelativeDirection.RIGHT;
				if (curPos.y + 1 == dest.y) return myRelativeDirection.BACKWARD;
				if (curPos.y - 1 == dest.y) return myRelativeDirection.FORWARD;
				
			default:
				return myRelativeDirection.FORWARD;
			}
		}
		
		
		/**
		 * Check if you have a wall in front of you!
		 * @param orientation the orientation we are in based on WorldSpatial
		 * @param currentView what the car can currently see
		 * @return
		 */
		private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView){
			switch(orientation){
			case EAST:
				return checkWallEast(currentView);
			case NORTH:
				return checkWallNorth(currentView);
			case SOUTH:
				return checkWallSouth(currentView);
			case WEST:
				return checkWallWest(currentView);
			default:
				return false;
			}
		}
		
		
		/**
		 * Method below just iterates through the list and check in the correct coordinates.
		 * i.e. Given your current position is 10,10
		 * checkEast will check up to wallSensitivity amount of tiles to the right.
		 * checkWest will check up to wallSensitivity amount of tiles to the left.
		 * checkNorth will check up to wallSensitivity amount of tiles to the top.
		 * checkSouth will check up to wallSensitivity amount of tiles below.
		 */
		public boolean checkWallEast(HashMap<Coordinate, MapTile> currentView){
			// Check tiles to my right
			Coordinate currentPosition = new Coordinate(getPosition());
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
				if(tile.isType(MapTile.Type.WALL)){
					return true;
				}
			}
			return false;
		}
		
		public boolean checkWallWest(HashMap<Coordinate,MapTile> currentView){
			// Check tiles to my left
			Coordinate currentPosition = new Coordinate(getPosition());
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
				if(tile.isType(MapTile.Type.WALL)){
					return true;
				}
			}
			return false;
		}
		
		public boolean checkWallNorth(HashMap<Coordinate,MapTile> currentView){
			// Check tiles to towards the top
			Coordinate currentPosition = new Coordinate(getPosition());
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
				if(tile.isType(MapTile.Type.WALL)){
					return true;
				}
			}
			return false;
		}
		
		public boolean checkWallSouth(HashMap<Coordinate,MapTile> currentView){
			// Check tiles towards the bottom
			Coordinate currentPosition = new Coordinate(getPosition());
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
				if(tile.isType(MapTile.Type.WALL)){
					return true;
				}
			}
			return false;
		}
		
		
		/**
		 * Check if you have a wall in front of you!
		 * @param orientation the orientation we are in based on WorldSpatial
		 * @param currentView what the car can currently see
		 * @return
		 */
		private MapTile.Type checkTileAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView){
			switch(orientation){
			case EAST:
				return checkEast(currentView);
			case NORTH:
				return checkNorth(currentView);
			case SOUTH:
				return checkSouth(currentView);
			case WEST:
				return checkWest(currentView);
			default:
				return null;
			}
		}
		
		/**
		 * Method below just iterates through the list and check in the correct coordinates.
		 * i.e. Given your current position is 10,10
		 * checkEast will check up to wallSensitivity amount of tiles to the right.
		 * checkWest will check up to wallSensitivity amount of tiles to the left.
		 * checkNorth will check up to wallSensitivity amount of tiles to the top.
		 * checkSouth will check up to wallSensitivity amount of tiles below.
		 */
		public MapTile.Type checkEast(HashMap<Coordinate, MapTile> currentView){
			// Check tiles to my right
			Coordinate currentPosition = new Coordinate(getPosition());
			MapTile tile = currentView.get(new Coordinate(currentPosition.x+1, currentPosition.y));
			return tile.getType();
		}
		
		public MapTile.Type checkWest(HashMap<Coordinate,MapTile> currentView){
			// Check tiles to my left
			Coordinate currentPosition = new Coordinate(getPosition());
			MapTile tile = currentView.get(new Coordinate(currentPosition.x-1, currentPosition.y));
			return tile.getType();
		}
		
		public MapTile.Type checkNorth(HashMap<Coordinate,MapTile> currentView){
			// Check tiles to towards the top
			Coordinate currentPosition = new Coordinate(getPosition());
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+1));
			return tile.getType();
		}
		
		public MapTile.Type checkSouth(HashMap<Coordinate,MapTile> currentView){
			// Check tiles towards the bottom
			Coordinate currentPosition = new Coordinate(getPosition());
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-1));
			return tile.getType();
		}
		
		@Override
		/**
		 * Speeds the car up in the forward direction
		 */
		public void applyForwardAcceleration(){
			super.applyForwardAcceleration();
			this.isGoingForward = true;
		}
		
		@Override
		/**
		 * Speeds the car up in the backwards direction
		 */
		public void applyReverseAcceleration(){
			super.applyReverseAcceleration();
			this.isGoingForward = false;
		}
		
		@Override
		/**
		 * Slows the car down
		 */
		public void applyBrake(){
			super.applyBrake();
			this.isGoingForward = true;
		}
		
		protected ArrayList<Coordinate> findPath(Coordinate start, Coordinate dest, Set<Coordinate> coordsToAvoid){
			// perform BFS on the map
			ArrayList<Coordinate> path = new ArrayList<>();
			HashMap<Coordinate,MapTile> map = this.getMap();
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
					if (tile != null && !tile.isType(MapTile.Type.WALL)
							&& !coordsToAvoid.contains(next_coord)) {
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
		
		protected void setPath(ArrayList<Coordinate> path) {
			assert(path != null);
			
			this.applyBrake();
			this.currentPath = path;
			this.nextInPath = 0;
			
			
			System.out.println("Path set towards: " + path.get(0));
			System.out.println(path.toString());
			
			updateDest();
		}
		
		protected void resetPath() {
			this.applyBrake();
			this.currentPath = null;
			this.nextInPath = 0;
			System.out.println("Path reset");
		}
		
		protected void updateDest() {
			this.dest = currentPath.get(currentPath.size() - nextInPath - 1);
		}
		
		// generates a spiral of Coordinates around a specified start in the anticlockwise direction
		// NOTE: many points in the output array will not be valid Coordinates in the map
		private ArrayList<Coordinate> generateSpiral(Coordinate start, WorldSpatial.Direction initDir){
			ArrayList<Coordinate> spiral = new ArrayList<>();
			int edgeLen = 1;
			int signX = 1, signY = 1;
			boolean dxFirst = true;
			
			switch(initDir) {
			case NORTH:
				dxFirst = false;
				signX = -1;
				signY = 1;
			case WEST:
				dxFirst = true;
				signX = -1;
				signY = -1;
			case SOUTH:
				dxFirst = false;
				signX = 1;
				signY = -1;
			case EAST:
				dxFirst = true;
				signX = 1;
				signY = 1;
				
			}
			
			Coordinate temp = new Coordinate(start.toString());
			
			// we are done when we complete a full loop and don't pick up any valid points
			// note: two loopings of while correspond to a full spiral loop
			boolean foundValidPoint = true;
			boolean foundValidPointPrev = true;
			while(foundValidPoint || foundValidPointPrev) {
				foundValidPointPrev = foundValidPoint;
				foundValidPoint = false;
			
				for (int i = 0; i < edgeLen; i++) {
					if (dxFirst) {
						temp.x += signX;
					} else {
						temp.y += signY;
					}
					if (isValidCoord(temp)) {
						spiral.add(new Coordinate(temp.toString()));
						foundValidPoint = true;
					}
				}
				
				for (int i = 0; i < edgeLen; i++) {
					if (dxFirst) {
						temp.y += signY;
					} else {
						temp.x += signX;
					}
					if (isValidCoord(temp)) {
						spiral.add(new Coordinate(temp.toString()));
						foundValidPoint = true;
					}
				}
				edgeLen++;
				signX *= -1;
				signY *= -1;
			}
			
			return spiral;
		}
		
		private boolean isValidCoord(Coordinate coord) {
			return coord.x < World.MAP_WIDTH && coord.x >= 0 &&
					coord.y < World.MAP_HEIGHT && coord.y >= 0;
		}
	}
