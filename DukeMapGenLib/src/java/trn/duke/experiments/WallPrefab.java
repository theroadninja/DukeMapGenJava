package trn.duke.experiments;

import trn.Wall;

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
	Short shade = null;
	
	public WallPrefab(Integer texture){
		this.texture = texture;
		this.xrepeat = 16;
		this.yrepeat = 8;
	}
	
	public WallPrefab(WallPrefab copyMe){
		this.texture = copyMe.texture;
		this.xrepeat = copyMe.xrepeat;
		this.yrepeat = copyMe.yrepeat;
		this.shade = copyMe.shade;
	}
	
	public WallPrefab setTexture(int texture){
		this.texture = texture;
		return this;
	}
	
	public WallPrefab setXRepeat(int xrepeat){
		this.xrepeat = (short)xrepeat;
		return this;
	}
	
	public WallPrefab setYRepeat(int yrepeat){
		this.yrepeat = (short)yrepeat;
		return this;
	}
	
	public WallPrefab setShade(short shade){
		this.shade = shade;
		return this;
	}
	
	public void writeTo(Wall... walls){
		for(Wall w : walls){
			if(texture != null){
				w.setTexture(texture);
			}
			
			if(xrepeat != null){
				w.setXRepeat(xrepeat);
			}
			
			if(yrepeat != null){
				w.setYRepeat(yrepeat);
			}
			
			if(shade != null){
				w.setShade(shade);
			}	
		}
		
	}
	
	
	
	
}
