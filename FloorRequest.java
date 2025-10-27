/**
 * An enum that represents the current state of the elevator.
 * Idle: the elevator is waiting instructions
 * Moving: the elevator is currently moving
 * Out of service: the elevator is out of service
 */
enum ElevatorState{
	IDLE,			//waiting for instructions
	MOVING,			//moving either up or down
	OUT_OF_SERVICE	//out of service
}

/**
 * An enum representing the directions up and down
 */
enum Direction{
	UP,
	DOWN
}

/**
 * A class holding information about a floor and a direction the elevator
 * must move to reach that floor. This is used to store floor requests when people try and summon the elevator.
 */
public class FloorRequest {
	private int floor;
	private Direction dir;

	public FloorRequest(int floor, Direction dir) {
		this.floor = floor;
		this.dir = dir;
	}

	//Getter methods
	public int getFloor() {return floor;}
	public Direction getDir() {return dir;}
}
