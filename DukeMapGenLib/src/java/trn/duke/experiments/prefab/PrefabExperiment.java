package trn.duke.experiments.prefab;

import java.util.ArrayList;
import java.util.List;

import trn.ISpriteFilter;
import trn.Map;
import trn.PlayerStart;
import trn.PointXYZ;
import trn.Sprite;
import trn.SpriteFilter;
import trn.prefab.*;

public class PrefabExperiment {

    // group 10 = plus sign
    // group 11 = player start
    // group 12 = round horizontal corridor
    // group 14 = nuke button

	public static Map copytest3(Map fromMap){

        Map outMap = Map.createNew();

        final PrefabPalette palette = new PrefabPalette();

        MapBuilder mb = new MapBuilder(outMap, palette);
        palette.loadAllGroups(fromMap);



		//List<PastedSectorGroup> pastedGroups = new ArrayList<PastedSectorGroup>();
		PastedSectorGroup psg1 = mb.pasteSectorGroup(10, new PointXYZ(-1024*30, -1024*50, 0));
		//pastedGroups.add(psg1);

		PastedSectorGroup psg2 = null;
		{
			//Connector conn2 = psg1.getConnector(123);
			//psg2 = mb.pasteAndLink(12, conn2);

			Connector conn2 = psg1.findFirstConnector(Connector.WestConnector);
			psg2 = mb.pasteAndLink(12, Connector.EastConnector, conn2);

		}

		// add a third group!
		PastedSectorGroup psg3 = mb.pasteAndLink(10, Connector.EastConnector, psg2.findFirstConnector(Connector.WestConnector));

		// add exit
		{
			//Connector c = psg3.findFirstConnector(Connector.EastConnector);
			Connector c = mb.findFirstUnlinkedConnector(Connector.EastConnector);
            mb.pasteAndLink(14, Connector.WestConnector, c);
		}
		
		// now try to add the player start group - 11
		{
			Connector leftEdge = psg3.findFirstConnector(Connector.WestConnector);
            mb.pasteAndLink(11, Connector.EastConnector, leftEdge);
		}
		
		//try adding a group(s) to the north of psg3
		{
			Connector north = psg3.findFirstConnector(Connector.NorthConnector);
            PastedSectorGroup sgNorth = mb.pasteAndLink(10, Connector.SouthConnector, north);

			north = sgNorth.findFirstConnector(Connector.NorthConnector);
            PastedSectorGroup sgNorth2 = mb.pasteAndLink(13, Connector.SouthConnector, north);
		}

		//some random groups to the south of something

        mb.pasteAndLink(10, Connector.NorthConnector, mb.findFirstUnlinkedConnector(Connector.SouthConnector));
        SectorGroup sg = palette.getRandomGroupWith(Connector.NorthConnector);
        mb.pasteAndLink(sg, Connector.NorthConnector, mb.findFirstUnlinkedConnector(Connector.SouthConnector));

        mb.pasteAndLink(sg, Connector.NorthConnector, mb.findFirstUnlinkedConnector(Connector.SouthConnector));
        mb.pasteAndLink(sg, Connector.NorthConnector, mb.findFirstUnlinkedConnector(Connector.SouthConnector));
        mb.pasteAndLink(sg, Connector.NorthConnector, mb.findFirstUnlinkedConnector(Connector.SouthConnector));
        mb.pasteAndLink(sg, Connector.NorthConnector, mb.findFirstUnlinkedConnector(Connector.SouthConnector));


		
		mb.selectPlayerStart();
		mb.clearMarkers();
		return outMap;
	}

}
