package trn.duke.experiments;

import trn.Sector;

/**
 * Like wall prefab, but for sectors.
 * @author Dave
 *
 */
public class SectorPrefab {

	Integer floorTexture = null;
	Integer ceilingTexture = null;
	Short floorShade = null;
	Short ceilingShade = null;
	
	public SectorPrefab(int floorTex, int ceilingTex){
		this.floorTexture = floorTex;
		this.ceilingTexture = ceilingTex;
	}
	
	public SectorPrefab(SectorPrefab copyMe){
		this.floorTexture = copyMe.floorTexture;
		this.ceilingTexture = copyMe.ceilingTexture;
	}
	
	public SectorPrefab setFloorShade(short floorShade){
		this.floorShade = (short)floorShade;
		return this;
	}
	
	public SectorPrefab setCeilingShade(short ceilingShade){
		this.ceilingShade = ceilingShade;
		return this;
	}
	
	public void writeTo(Sector sector){
		if(floorTexture != null){
			sector.setFloorTexture(floorTexture);
		}
		
		if(floorShade != null){
			sector.setFloorShade(floorShade);
		}
		
		if(ceilingTexture != null){
			sector.setCeilingTexture(ceilingTexture);
		}
		
		if(ceilingShade != null){
			sector.setCeilingShade(ceilingShade);
		}
		
	}
}
