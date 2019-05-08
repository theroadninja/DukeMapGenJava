package trn.prefab;


import trn.PointXY;
import trn.PointXYZ;
import trn.Sprite;

/**
 * Exception for logic errors arising from sprite/lotag/hitag mistakes in a map. 
 * @author Dave
 *
 */
@SuppressWarnings("serial")
public class SpriteLogicException extends RuntimeException {
	
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
