package trn;

import trn.prefab.PrefabUtils;

import java.util.List;

public class SpriteFilter implements ISpriteFilter {
	
	public static final int TEXTURE = 0;
	public static final int LOTAG = 1;
	public static final int HITAG = 2;
	public static final int SECTOR_ID = 3;
	
	private int filterType;
	private int filterValue;
	
	public SpriteFilter(int filterType, int filterValue){
		this.filterType = filterType;
		this.filterValue = filterValue;
	}
	
	public boolean matches(Sprite sprite){
		if(this.filterType == TEXTURE){
			return sprite.picnum == this.filterValue;
		}else if(this.filterType == LOTAG){
			return sprite.lotag == this.filterValue;
		}else if(this.filterType == HITAG){
			return sprite.hitag == this.filterValue;
		}else if(this.filterType == SECTOR_ID){
			return sprite.sectnum == this.filterValue;
		}else{
			throw new IllegalStateException("bad filter type");
		}
		
	}
	
	public static ISpriteFilter playerstart(){
		return new ISpriteFilter(){
			public boolean matches(Sprite sprite){
				return sprite.picnum == PrefabUtils.MARKER_SPRITE_TEX
						&& sprite.lotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START;
			}
		};
	}
	
	public static ISpriteFilter loTag(int filterValue){
		return new SpriteFilter(LOTAG, filterValue);
	}
	
	public static ISpriteFilter hiTag(int filterValue){
		return new SpriteFilter(HITAG, filterValue);
	}
	
	public static ISpriteFilter texture(int texture){
		return new SpriteFilter(TEXTURE, texture);
	}
	
	public static boolean matchAll(Sprite sprite, ISpriteFilter ... filters){
		for(ISpriteFilter sf: filters){
			if(! sf.matches(sprite)){
				return false;
			}
		}
		return true;
	}

	// TODO - dry
	public static boolean matchAll(Sprite sprite, List<ISpriteFilter> filters){
		for(ISpriteFilter sf: filters){
			if(! sf.matches(sprite)){
				return false;
			}
		}
		return true;
	}


}