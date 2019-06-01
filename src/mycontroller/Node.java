package mycontroller;

import utilities.Coordinate;

public class Node {
	private Coordinate coord;
	private Node from;
	private boolean discovered = false;
	
	public Node(Coordinate coord, Node from) {
		this.coord = coord;
		this.setFrom(from);
	}
	
	public boolean equals(Node other) {
		return this.getCoord().equals(other.getCoord());
	}

	public Coordinate getCoord() {
		return coord;
	}

	public boolean isDiscovered() {
		return discovered;
	}

	public void setDiscovered(boolean discovered) {
		this.discovered = discovered;
	}

	public Node getFrom() {
		return from;
	}

	public void setFrom(Node from) {
		this.from = from;
	}
}
