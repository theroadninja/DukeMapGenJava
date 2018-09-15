package trn.duke.experiments.prefab;

import trn.Map;
import trn.PointXYZ;
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
			//SimpleConnector conn2 = psg1.getConnector(123);
			//psg2 = mb.pasteAndLink(12, conn2);

			Connector conn2 = psg1.findFirstConnector(SimpleConnector.WestConnector);
			psg2 = mb.pasteAndLink(12, SimpleConnector.EastConnector, conn2);

		}

		// add a third group!
		PastedSectorGroup psg3 = mb.pasteAndLink(10, SimpleConnector.EastConnector, psg2.findFirstConnector(SimpleConnector.WestConnector));

		// add exit
		{
			//SimpleConnector c = psg3.findFirstConnector(SimpleConnector.EastConnector);
			Connector c = mb.findFirstUnlinkedConnector(SimpleConnector.EastConnector);
            mb.pasteAndLink(14, SimpleConnector.WestConnector, c);
		}
		
		// now try to add the player start group - 11
		{
			Connector leftEdge = psg3.findFirstConnector(SimpleConnector.WestConnector);
            mb.pasteAndLink(11, SimpleConnector.EastConnector, leftEdge);
		}
		
		//try adding a group(s) to the north of psg3
		{
			Connector north = psg3.findFirstConnector(SimpleConnector.NorthConnector);
            PastedSectorGroup sgNorth = mb.pasteAndLink(10, SimpleConnector.SouthConnector, north);

			north = sgNorth.findFirstConnector(SimpleConnector.NorthConnector);
            PastedSectorGroup sgNorth2 = mb.pasteAndLink(13, SimpleConnector.SouthConnector, north);
		}

		//some random groups to the south of something

        mb.pasteAndLink(10, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));
        SectorGroup sg = palette.getRandomGroupWith(SimpleConnector.NorthConnector);
        mb.pasteAndLink(sg, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));

        mb.pasteAndLink(sg, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));
        mb.pasteAndLink(sg, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));
        mb.pasteAndLink(sg, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));
        mb.pasteAndLink(sg, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));


		
		mb.selectPlayerStart();
		mb.clearMarkers();
		return outMap;
	}

}
