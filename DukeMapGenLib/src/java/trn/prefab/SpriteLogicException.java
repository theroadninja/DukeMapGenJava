package trn.prefab;


import trn.PointXY;
import trn.Sprite;

import java.util.List;

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

	public static void throwIf(boolean condition, String message, Sprite location){
	    if(condition){
	    	throw new SpriteLogicException(message, location);
		}
	}



	// TODO - delete this one in favor of throwIfSprite
	public static void requireSprite(boolean condition, String message, Sprite s){
		throwIf(condition, message + " at " + s.getLocation().asXY());
	}
	public static void throwIfSprite(boolean condition, String message, Sprite s){
		throwIf(condition, message + " at " + s.getLocation().asXY());
	}

	public static String locationsToString(List<PointXY> locations) {
		StringBuilder sb = new StringBuilder();
		for(PointXY loc : locations){
			sb.append(loc.toString());
			sb.append(",");
		}
		return sb.toString();
	}

	public SpriteLogicException(String message){
		super(message);
	}

	public SpriteLogicException(String message, PointXY location){
		super(message + " near " + location.toString());
	}

	public SpriteLogicException(String message, List<PointXY> locations) {
		this(message + " near " + locationsToString(locations));
	}

	// TODO document somewhere that in mapster32 you can hold single quote and press J to jump to coordinates!
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
