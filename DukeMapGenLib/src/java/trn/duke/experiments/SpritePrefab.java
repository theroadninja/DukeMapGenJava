package trn.duke.experiments;

import trn.Sprite;

/**
 * TODO get rid of this
 */
public class SpritePrefab {

	Integer texture;
	
	Short pallette;
	
	public SpritePrefab(int texture){
		this.texture = Integer.valueOf(texture);
	}
	
	public SpritePrefab setPal(short pallette){
		this.pallette = Short.valueOf(pallette);
		return this;
	}
	
	public void writeTo(Sprite s){
		if(texture != null){
			s.setTexture(texture);
		}
		
		if(pallette != null){
			s.setPal(pallette);
		}
	}
}
