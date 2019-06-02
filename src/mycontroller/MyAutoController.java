package mycontroller;

import controller.CarController;
import controller.SimpleAutoController;
import exceptions.UnsupportedModeException;
import swen30006.driving.Simulation;
import world.Car;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
		
		// stores the locations of the exit tiles
		protected ArrayList<Coordinate> finish = new ArrayList<Coordinate>();
		protected ArrayList<Coordinate> unseenCoords = new ArrayList<Coordinate>();
		protected HashMap<Coordinate,MapTile> map;
		
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
			}
			
			this.movementStrategy.move();
		}
		
		// assumes dest is one step in a cardinal direction away from current position
		protected void moveTowards(Coordinate dest) {
			Coordinate coord = new Coordinate(getPosition());
			
			// for when a path ended right in front of a wall facing it and the new path required
			// an immediate turn
			if (isBackingFromWall || isDrivingFromWall) {
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
					this.applyReverseAcceleration();
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
	}
