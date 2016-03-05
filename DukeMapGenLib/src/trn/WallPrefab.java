package trn;

/**
 * Spec that describes a wall to create, to make it easier to create multiple walls.
 * 
 * 
 * @author Dave
 *
 */
public class WallPrefab {

	Integer texture = null;
	Short xrepeat = null;
	Short yrepeat = null;
	
	public WallPrefab(Integer texture){
		this.texture = texture;
	}
	
	public WallPrefab setXRepeat(int xrepeat){
		this.xrepeat = (short)xrepeat;
		return this;
	}
	
	public WallPrefab setYRepeat(int yrepeat){
		this.yrepeat = (short)yrepeat;
		return this;
	}
	
	public void writeTo(Wall w){
		if(texture != null){
			w.setTexture(texture);
		}
		
		if(xrepeat != null){
			w.setXRepeat(xrepeat);
		}
		
		if(yrepeat != null){
			w.setYRepeat(yrepeat);
		}
	}
	
	
	
}
