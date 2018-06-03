package trn.prefab;


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

}
