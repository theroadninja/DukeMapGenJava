package trn.duke;

import java.io.IOException;

@SuppressWarnings("serial")
public class MapErrorException extends IOException {

	public MapErrorException(String message){
		super(message);
	}
}
