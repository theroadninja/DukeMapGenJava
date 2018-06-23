package trn.duke.experiments.prefab;

import java.util.List;

import trn.ISpriteFilter;
import trn.Map;
import trn.PlayerStart;
import trn.PointXYZ;
import trn.Sprite;
import trn.SpriteFilter;
import trn.prefab.Connector;
import trn.prefab.PastedSectorGroup;
import trn.prefab.PrefabPalette;
import trn.prefab.PrefabUtils;

public class PrefabExperiment {
	
	
	public static void copytest3(Map fromMap, Map outMap){
		

		PrefabPalette palette = new PrefabPalette();
		palette.loadAllGroups(fromMap);
		
		// group 10 = plus sign
		// group 11 = player start
		// group 12 = round horizontal corridor
		
		
		PastedSectorGroup psg1 = palette.pasteSectorGroup(10, outMap, new PointXYZ(-1024*30, -1024*50, 0));
		
		
		PastedSectorGroup psg2 = null;
		{
			Connector conn2 = psg1.getConnector(123);
			psg2 = palette.pasteAndLink(12, 456, outMap, conn2);
		}

		
		// add a third group!
		PastedSectorGroup psg3 = null;
		{
			Connector leftEdge = psg2.getConnector(123);
			
			//Connector rightEdge = palette.getConnector(10, 456);
			//PointXYZ cdelta = rightEdge.getTransformTo(leftEdge);
			psg3 = palette.pasteAndLink(10, 456, outMap, leftEdge);
			
			//PastedSectorGroup psg3 = palette.pasteSectorGroup(10, outMap, cdelta);
			
		}
		
		
		
		
		// now try to add the player start group - 11
		{
			//Connector leftEdge = psg3.getConnector(123);
			Connector leftEdge = psg3.findFirstConnector(Connector.WestConnector);
			
			PastedSectorGroup sgPlayerStart = palette.pasteAndLink(11, Connector.EastConnector, outMap, leftEdge);
			
			
		}
		
		
		//try adding a group(s) to the north of psg3
		{
			Connector north = psg3.findFirstConnector(Connector.NorthConnector);
			PastedSectorGroup sgNorth = palette.pasteAndLink(10, Connector.SouthConnector, outMap, north);
			
			
			north = sgNorth.findFirstConnector(Connector.NorthConnector);
			PastedSectorGroup sgNorth2 = palette.pasteAndLink(13, Connector.SouthConnector, outMap, north);
			
		}
		
		
		ISpriteFilter psfilter = SpriteFilter.playerstart();
		List<Sprite> sprites = outMap.findSprites(psfilter);
		
		System.out.println("filter matches = " + psfilter.matches(sprites.get(0)));
		System.out.println("sprite is: " + sprites.get(0));
		
		if(sprites.size() != 1){
			throw new RuntimeException("wft? sprite count is " + sprites.size());
		}
		Sprite pstart = outMap.findSprites(psfilter).iterator().next();
		
		
		outMap.setPlayerStart(new PlayerStart(pstart.getLocation(),0));
		
		
		
		
		outMap.deleteSprites(SpriteFilter.texture(PrefabUtils.MARKER_SPRITE_TEX));
	}

}
