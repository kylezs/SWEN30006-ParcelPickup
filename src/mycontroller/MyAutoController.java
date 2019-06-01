package mycontroller;

import controller.CarController;
import controller.SimpleAutoController;
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
		private int wallSensitivity = 2;
		
		private boolean isFollowingWall = false; // This is set to true when the car starts sticking to a wall.
		
		// stores the locations of the exit tiles
		protected ArrayList<Coordinate> finish = new ArrayList<Coordinate>();
		
		// Car Speed to move at
		private final int CAR_MAX_SPEED = 1;
		private IMovementStrategy movementStrategy;
		
		public MyAutoController(Car car) {
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
				System.out.println("Please select a valid strategy in Driving.Properties");
				System.exit(1);
				break;
			}
			
			// find finish tiles
			HashMap<Coordinate,MapTile> map = this.getMap();
			for(Coordinate coord : map.keySet()){
				if (map.get(coord).isType(MapTile.Type.FINISH)) {
					finish.add(coord);
				}
			}
		}
		
		// Coordinate initialGuess;
		// boolean notSouth = true;
		@Override
		public void update() {
			this.movementStrategy.move();
		}
		
		// assumes there is a one dimensional straight line path between currentPos and coord
		protected void moveTowards(Coordinate dest) {
			Coordinate coord = new Coordinate(getPosition());
			if (dest.x == coord.x && dest.y != coord.y 
					|| dest.x != coord.x && dest.y == coord.y) {
				// face the right direction
				WorldSpatial.Direction requiredDir;
				if (dest.y > coord.y) {
					requiredDir = WorldSpatial.Direction.NORTH;
				} else if (dest.y < coord.y) {
					requiredDir = WorldSpatial.Direction.SOUTH;
				} else if (dest.x > coord.x) {
					requiredDir = WorldSpatial.Direction.EAST;
				} else if (dest.x < coord.x) {
					requiredDir = WorldSpatial.Direction.WEST;
				} else {
					requiredDir = WorldSpatial.Direction.NORTH; // default, should never happen
				}

				
				// if there is not a wall ahead
				if (!checkWallAhead(this.getOrientation(), this.getView())) {
					this.applyForwardAcceleration();
				} else {
					System.out.println("Wall ahead");
					this.applyBrake();
				}
				
				if (! getOrientation().equals(requiredDir)) {
					turnTowards(requiredDir);
				}
			}
		}
		
		private void turnTowards(WorldSpatial.Direction requiredDir) {
			WorldSpatial.Direction rotatedLeft = WorldSpatial.changeDirection(getOrientation(), WorldSpatial.RelativeDirection.LEFT);
			if (requiredDir.equals(rotatedLeft)){
				this.applyForwardAcceleration();
				turnLeft();
			} else {
				// for at most 2 rotations
				for (int i = 0; i < 2 && !requiredDir.equals(getOrientation()); i++) {
					this.applyForwardAcceleration();
					turnRight();
				}
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
	}
