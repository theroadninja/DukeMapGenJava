package trn.prefab;


import trn.PointXY;
import trn.Sprite;

/**
 * Exception for logic errors arising from sprite/lotag/hitag mistakes in a map. 
 * @author Dave
 *
 */
@SuppressWarnings("serial")
public class SpriteLogicException extends RuntimeException {
	public static void throwIf(boolean condition, String message) {
		if(condition){
			throw new SpriteLogicException(message);
		}
	}
	public static void requireSprite(boolean condition, String message, Sprite s){
		throwIf(condition, message + " at " + s.getLocation().asXY());
	}
	
	public SpriteLogicException(String message){
		super(message);
	}

	public SpriteLogicException(String message, PointXY location){
		super(message + " near " + location.toString());
	}

	public SpriteLogicException(String message, Sprite location){
		this(message, location.getLocation().asXY());
	}

	public SpriteLogicException(String message, Exception cause){
		super(message, cause);
	}
	
	public SpriteLogicException(){
		super();
	}

}
