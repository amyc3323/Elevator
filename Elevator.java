import java.util.ArrayList;

/**
 * A class that represents an elevator
 */
public class Elevator {	
//---------------------- Variables ----------------------------------
	//The time the elevator should leave the doors open
	private final float DOOR_WAIT_TIME = 7f;
	//The time the elevator should wait between moving and opening/closing doors
	private final float INTERMEDIATE_WAIT_TIME = 2f;
	
	private int currentFloor = 0; //the current floor the elevator is on
	private int targetFloor = 0; //the target floor the elevator is trying to get to
		
	private Direction dir = Direction.UP;
	private ElevatorState state = ElevatorState.IDLE;
		
	private ElevatorController controller;
	
	//The next floor the elevator should go to after reaching the current target floor
	//This variable will only be used if a user inside the elevator pushes a floor
	//That is in the opposite direction the elevator is currently moving in
	//-1 means there is no queued target floor
	private int queuedTargetFloor = -1;

//---------------------- GETTERS & SETTERS ----------------------------------
	public Direction getDir(){return dir;}
	public void setDir(Direction dir){this.dir = dir;}
		
	public ElevatorState getState(){return state;}
	public void setState(ElevatorState state){this.state = state;}
		
	public int getCurrentFloor(){return currentFloor;}
		
	public int getTargetFloor(){return targetFloor;}
	public void setTargetFloor(int targetFloor){this.targetFloor = targetFloor;}

	public void setInstance(ElevatorController controller) {
		this.controller = controller;
	}
	
//---------------------- CONSTRUCTORS----------------------------------	
	public Elevator(ElevatorController controller) {
		this.controller = controller;
	}
//---------------------- METHODS----------------------------------
	/**
	 * This method is called when a button is pressed within the elevator. 
	 * If the person in the elevator selects a floor in the same direction as the elevator, update the target 
	 * floor if needed. Otherwise the floor must be in the opposite direction, so queue the next target floor.
	 * This ensures that the elevator finishes requests in the current direction before switching directions.
	 * @param floorRequested the floor the person requests (0-indexed) by pushing the button
	 */
	public void FloorButtonPressed(int floorRequested) {
		if(floorRequested < 0 || floorRequested >= controller.getNumFloors()) {
			throw new FloorRangeException("The requested floor is out of range");
		}
		
		if (dir == Direction.UP) {
			if (floorRequested >= currentFloor) targetFloor = Math.max(targetFloor, floorRequested);
			else if (queuedTargetFloor == -1)queuedTargetFloor = floorRequested;
			else queuedTargetFloor = Math.min(queuedTargetFloor, floorRequested);
		}
		else {
			assert(dir == Direction.DOWN);
			if (floorRequested <= currentFloor) targetFloor = Math.min(targetFloor, floorRequested);
			else if (queuedTargetFloor == -1)queuedTargetFloor = floorRequested;
			else queuedTargetFloor = Math.max(queuedTargetFloor, floorRequested);
		}	
	}
	
	/**
	 * This method sends an idle elevator to a target floor. All of the elevator floor logic happens here.
	 * Once an elevator is sent moving, it will continue to run until it runs out of people to service, in which
	 * it will return to idle. If an elevator finishes servicing the people on board but there are people waiting in 
	 * the pending queue, the elevator will go service the person who has been waiting the longest in order
	 * to try and reduce wait time.
	 * @param floor the floor the elevator is being sent to
	 */
	public void GoToFloor(int floor) {
		if(floor < 0 || floor >= controller.getNumFloors()) {
			throw new FloorRangeException("The requested floor is out of range");
		}
		
		//This should never happen with current code
		if (state != ElevatorState.IDLE) throw new ElevatorStateException("Tried to move a non-idle elevator");
		
		//since GoToFloor called when elevator is idle, targetFloor is unset so we directly override it
		targetFloor = floor;
		state = ElevatorState.MOVING;
		
		boolean isGoingUp = dir == Direction.UP;
		dir = targetFloor >= currentFloor ? Direction.UP : Direction.DOWN;
		boolean[] summons = isGoingUp ? controller.getUpSummons() : controller.getDownSummons();
		int increment = isGoingUp ? 1 : -1;
		
		for (int i = currentFloor; isGoingUp ? i <= targetFloor : i >= targetFloor; i+=increment) {
			if (summons[i]) {
				PickUpPassenger();
			}
			
			currentFloor = i;
			summons[i] = false;
			
			//if this is the last iteration of the loop
			if (i == (isGoingUp ? targetFloor-1 : targetFloor+1)) {
				ArrayList<FloorRequest> pendingRequests = controller.getPendingRequests();
				
				if (queuedTargetFloor == -1 && pendingRequests.size() == 0) {
					//nothing else to do, go idle
					state = ElevatorState.IDLE;
				}
				else{
					if (queuedTargetFloor >= 0) {
						targetFloor = queuedTargetFloor;
					}
					else {
						//Design choice: choose next request based on queue time, not distance
						targetFloor = pendingRequests.get(0).getFloor();
						pendingRequests.remove(0);
					}
					
					//reset variables based on new target floor
					dir = currentFloor > targetFloor ? Direction.DOWN : Direction.UP;
					i = currentFloor;
					isGoingUp = dir == Direction.UP;
					increment = isGoingUp ? 1 : -1;
					summons = isGoingUp ? controller.getUpSummons() : controller.getDownSummons();
					
					//Figure out which pending requests the elevator will see along the way
					claimPendingRequests(pendingRequests);
				}
			}
		}
	}
	
	/**
	 * Remove any floor requests from pendingRequests that are either within the max distance threshold
	 * from the current floor, or will be visited along the way to the target floor
	 * @param pendingRequests the array of pending requests
	 */
	private void claimPendingRequests(ArrayList<FloorRequest> pendingRequests) {		
		boolean isGoingUp = targetFloor >= currentFloor;
		
		for (FloorRequest request : pendingRequests) {
			if (isRequestInRange(request)) {
				if (isGoingUp) targetFloor = Math.max(targetFloor, request.getFloor());
				else targetFloor = Math.min(targetFloor, request.getFloor());
				
				//O(n) in theory but O(1) in practice since buildings don't have more than 100 floors
				pendingRequests.remove(request);
			}
		}
	}
	
	/**
	 * Checks to see if a request is within the range of the elevator.
	 * The request is within range if it is along the way in the current direction and before the targetFloor,
	 * or if it is after the targetFloor in the current direction but still within the max distance threshold
	 * @param request the request to check if its within range
	 * @return true if the request is within range and false otherwise
	 */
	private boolean isRequestInRange(FloorRequest request) {
		int maxDistanceThreshold = controller.getMaxDistanceThreshold();
		
		if(dir == Direction.DOWN) {
			boolean isCompatibleDirection = request.getDir() == Direction.DOWN && request.getFloor() <= currentFloor;
			
			int lowestFloor = Math.min(targetFloor, currentFloor-maxDistanceThreshold);
			return isCompatibleDirection && request.getFloor() >= lowestFloor;
		} 
		else {
			assert(dir == Direction.UP);
			boolean isCompatibleDirection = request.getDir() == Direction.UP && request.getFloor() >= currentFloor;
			
			int highestFloor = Math.max(targetFloor, currentFloor+maxDistanceThreshold);
			return isCompatibleDirection && request.getFloor() <= highestFloor;
		}
	}
	
	/**
	 * Method to open and close doors to pick up a passenger
	 */
	private void PickUpPassenger() {
		//ensure safely stopped
		Wait(INTERMEDIATE_WAIT_TIME);
		OpenDoor();
		//wait for people to get in and out of elevator
		Wait(DOOR_WAIT_TIME);
		CloseDoor();
		//ensure doors are safely closed and allow for time button presses
		Wait(INTERMEDIATE_WAIT_TIME);
	}
	
	/**
	 * Method called to open the elevator door
	 */
	private void OpenDoor() {
		//mechanical call to open door
	}
	
	/**
	 * Method called to close the elevator door
	 */
	private void CloseDoor() {
		//mechanical call to close door
	}
	
	/**
	 * Method called to wait
	 * @param time the amount of time to wait in seconds
	 */
	private void Wait(float time) {
		//mechanical wait for seconds
	}
}
