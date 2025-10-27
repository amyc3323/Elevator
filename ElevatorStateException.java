/**
 * An exception to be thrown when an elevator is not in the proper state
 */
public class ElevatorStateException extends RuntimeException{
	public ElevatorStateException(String message) {
		super(message);
	}
}
