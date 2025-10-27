/**
 * An exception to be thrown when a requested floor is not within range
 */
public class FloorRangeException extends RuntimeException{
	public FloorRangeException(String message) {
		super(message);
	}
}
