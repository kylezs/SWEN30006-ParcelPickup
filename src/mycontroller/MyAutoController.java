package mycontroller;

import controller.CarController;
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
		
		// stores the locations of the exit tiles
		private static ArrayList<Coordinate> finish = new ArrayList<Coordinate>();
		
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

		/**
		 * Check if you have a wall in front of you!
		 * @param orientation the orientation we are in based on WorldSpatial
		 * @param currentView what the car can currently see
		 * @return
		 */
		private MapTile.Type checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView){
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
		 * Check if the wall is on your left hand side given your orientation
		 * @param orientation
		 * @param currentView
		 * @return
		 */
		private MapTile.Type checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
			
			switch(orientation){
			case EAST:
				return checkNorth(currentView);
			case NORTH:
				return checkWest(currentView);
			case SOUTH:
				return checkEast(currentView);
			case WEST:
				return checkSouth(currentView);
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
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
				return tile.getType();
			}
			return null;
		}
		
		public MapTile.Type checkWest(HashMap<Coordinate,MapTile> currentView){
			// Check tiles to my left
			Coordinate currentPosition = new Coordinate(getPosition());
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
				return tile.getType();
			}
			return null;
		}
		
		public MapTile.Type checkNorth(HashMap<Coordinate,MapTile> currentView){
			// Check tiles to towards the top
			Coordinate currentPosition = new Coordinate(getPosition());
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
				return tile.getType();
			}
			return null;
		}
		
		public MapTile.Type checkSouth(HashMap<Coordinate,MapTile> currentView){
			// Check tiles towards the bottom
			Coordinate currentPosition = new Coordinate(getPosition());
			for(int i = 0; i <= wallSensitivity; i++){
				MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
				return tile.getType();
			}
			return null;
		}
		
	}
