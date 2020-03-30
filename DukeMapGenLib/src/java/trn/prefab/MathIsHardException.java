package trn.prefab;

/**
 * Using this to mark all of the places I've been too lazy to do the math right.
 *
 * TODO:  remove all of these.
 */
public class MathIsHardException extends RuntimeException {

    public MathIsHardException(String message){
        super(message);
    }
}
