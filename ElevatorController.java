import java.util.ArrayList;

/**
 * A class that represents the central control for multiple elevators in a building
 */
public class ElevatorController {
	
//---------------------- Variables ----------------------------------	
	//the number of floors the elevators service
	//note that floors are zero-indexed so numFloors is exclusive  
	//(there is no floor number numFloor)
	private int numFloors;
	//the number of elevators this elevator controller tracks
	private int numElevators;
	//the maximum distance the elevator will service at a time
	private int maxDistanceThreshold;
	
	//boolean arrays representing floors with elevator requests.
	//ex: if upSummons[i], then floor i has someone wanting to go up
	private boolean[] upSummons;
	private boolean[] downSummons;

	//The array of elevators that this elevator controller controls
	private Elevator[] elevators;
	//Floors with pending elevator requests that have not been claimed by an elevator yet
	private ArrayList<FloorRequest> pendingRequests = new ArrayList<FloorRequest>();
		
//---------------------- GETTERS & Setters----------------------------------
	public boolean[] getUpSummons() {return upSummons;}
	public boolean[] getDownSummons() {return downSummons;}
	public ArrayList<FloorRequest> getPendingRequests() {return pendingRequests;}
	public int getNumFloors() {return numFloors;}
	public int getMaxDistanceThreshold() {return maxDistanceThreshold;}
	
	/**
	 * Set the maxDistanceThreshold for elevators in case the client does not
	 * want to use the default of numFloors/numElevators.
	 * @param maxFloors the maximum distance the elevator will service at a time in terms of
	 * claiming floors to service
	 */
	public void setmaxDistanceThreshold(int maxFloors){
		maxDistanceThreshold = Math.max(Math.min(numFloors, maxFloors), 0);
	}
//---------------------- CONSTRUCTORS----------------------------------
	/**
	 * Constructor that takes in the number of floors and number of elevators
	 * @param numFloors the number of floors the elevators service
	 * @param numElevators the number of elevators this elevator controller controls
	 */
	public ElevatorController(int numFloors, int numElevators) {
		this.numFloors = numFloors;
		this.numElevators = numElevators;
		maxDistanceThreshold = numFloors/numElevators;
		this.elevators = new Elevator[numElevators];
		
		upSummons = new boolean[numFloors];
		downSummons = new boolean[numFloors];
		
		for (int i = 0; i < numElevators; i++) {
			//each elevator is instantiated to have its elevator controller be this
			elevators[i] = new Elevator(this);
		}
	}
	
	/**
	 * Constructor that takes in the number of floors and array of elevators
	 * so each elevator can be instantiated differently and then controlled by this elevator controller
	 * @param numFloors the number of floors the elevators service
	 * @param elevators the elevators this elevator controller should control
	 */
	public ElevatorController(int numFloors, Elevator[] elevators) {
		this.numFloors = numFloors;
		this.numElevators = elevators.length;
		maxDistanceThreshold = numFloors/numElevators;
		this.elevators = elevators;
		
		upSummons = new boolean[numFloors];
		downSummons = new boolean[numFloors];
		
		for (Elevator e : elevators) {
			//Set each elevator to have this as its elevator controller
			e.setInstance(this);
		}
	}
//---------------------- METHODS----------------------------------
	/**
	 * This method is called when a button is pressed to call the elevator to a floor. It recieves a direction
	 * and a floor and determines which elevator should respond to the call.
	 * It will try to send the closest elevator going in the compatible direction within the max distance threshold.
	 * If no such elevator exists, it will try to send an idle elevator.
	 * If no elevators are available, it will add the floor request to the pending list.
	 * @param fromFloor the floor the call came from
	 * @param dir the direction the user wants to go
	 */
	public void ElevatorSummon(int floor, Direction dir) {
		if (floor < 0 || floor >= numFloors) throw new FloorRangeException("The requested floor is out of range");
		
		boolean isGoingUp = dir==Direction.UP;
		//get the relevant summons array depending on direction
		boolean[] summons = isGoingUp ? upSummons : downSummons;
		//indicate that this floor is being summoned in the corresponding direction
		summons[floor] = true;
		
		FloorRequest floorDir = new FloorRequest(floor, dir);
		
		//calculate which elevator should be handling this request
		Elevator elevator = elevatorOnTheWay(floorDir);
		
		//if no such elevator is available, add floor request to pending requests list
		if (elevator == null) pendingRequests.add(floorDir);
		//otherwise, make sure the elevator goes to the floor eventually
		else {
			//if the elevator is going up, the target floor should be the max of the current target and the pending floor
			//if the elevator is going down, the target floor should be the min of the current target and the pending floor
			int newTargetFloor = elevator.getDir() == Direction.UP ? 
									Math.max(elevator.getTargetFloor(), floor) :
										Math.min(elevator.getTargetFloor(), floor);
			
			//if the elevator is already in motion, just update the target floor
			if (elevator.getState() == ElevatorState.MOVING) {
				elevator.setTargetFloor(newTargetFloor);
			}
			//if the elevator is not already moving, tell it to start moving to the floor
			else {
				if (elevator.getState() == ElevatorState.OUT_OF_SERVICE) {
					//This exception should never be thrown with the current code
					throw new ElevatorStateException("Tried to move an out of service elevator");
				}
				assert(elevator.getState() == ElevatorState.IDLE);
				
				//tell the elevator to start heading to the new target floor
				elevator.GoToFloor(newTargetFloor);
			}
		}
	}
	
	/**
	 * Based on a floor request, calculate which elevator is the best elevator to respond to the call.
	 * The best elevator is based on 2 factors, moving and distance:
	 * An elevator already on the way will be sent over an idle elevator.
	 * Among the already moving and idle elevators, the closest elevator will be sent.
	 * @param request the floor the request was made on and the direction the person wishes to go
	 * @return the elevator that should be sent, or null if all elevators are busy or not in range
	 */
	private Elevator elevatorOnTheWay(FloorRequest request) {
		Elevator elevator = null;
		int bestDistance = Integer.MAX_VALUE;
		int bestIdleDistance = Integer.MAX_VALUE;
		boolean sendingIdleElevator = false;
		
		for (Elevator e : elevators) {
			//do not send an out of service elevator
			if (e.getState() == ElevatorState.OUT_OF_SERVICE) continue;
			
			boolean eIsCompatible = isCompatibleDirection(e, request);
			
			if (e.getState() == ElevatorState.IDLE) {
				//only send an idle elevator if no other closer idle elevator is currently on the way
				if (elevator == null || sendingIdleElevator) {
					sendingIdleElevator = true;
					int currDistance = calculateDistance(e, request);
					if (currDistance < bestIdleDistance) {
						elevator = e;
						bestIdleDistance = currDistance;
					}
				}
			}
			else if (eIsCompatible){
				assert(e.getState()==ElevatorState.MOVING);
				int currDistance = calculateDistance(e, request);
				
				//send the closest elevator on the way already
				//but only if that elevator is within a close enough distance (maxDistanceThreshold)
				if (currDistance < bestDistance && currDistance <= maxDistanceThreshold) {
					elevator = e;
					sendingIdleElevator = false;
					bestDistance = currDistance;
				}
			}
		}
		return elevator;			
	}
	
	/**
	 * A helper function to check if an elevator and floor request are compatible.
	 * They are considered compatible if the floor request is in the same direction the elevator
	 * is currently travelling and if the elevator can reach the requested floor by heading
	 * in its current direction from its current floor
	 * @param e the elevator
	 * @param request the requested floor (0-indexed) and direction
	 * @return if elevator e is compatible with the floor request
	 */
	private boolean isCompatibleDirection(Elevator e, FloorRequest request) {
		if (e.getDir() == Direction.UP) {
			//compatible if requested floor is above and they also want to go up
			return e.getCurrentFloor() <= request.getFloor() && request.getDir() == Direction.UP;
		}
		else {
			assert(e.getDir() == Direction.DOWN);
			//compatible if requested floor is below and they also want to go down
			return e.getCurrentFloor() >= request.getFloor() && request.getDir() == Direction.DOWN;
		}
	}
	
	/**
	 * A helper function to calculate how many floors away an elevator is from a floor request
	 * @param e the elevator
	 * @param request the request
	 * @return the distance (which is nonnegative) the elevator is from the requested floor
	 */
	private int calculateDistance(Elevator e, FloorRequest request) {
		return Math.abs(request.getFloor() - e.getCurrentFloor());
	}
}
