package trn.duke.experiments.prefab;

import trn.Map;
import trn.PointXYZ;
import trn.duke.MapErrorException;
import trn.prefab.*;

public class PrefabExperiment {

    // group 10 = plus sign
    // group 11 = player start
    // group 12 = round horizontal corridor
    // group 14 = nuke button

	public static Map copytest3(Map fromMap) throws MapErrorException {

        //Map outMap = Map.createNew();

		final PrefabPalette palette = PrefabPalette.fromMap(fromMap);
		MapBuilder mb = new MapBuilder(palette);
        palette.fromMap(fromMap);



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

        //try sector group 15
        mb.pasteAndLink(15, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));

        //try sector group 18 - teleporter
		mb.pasteAndLink(18, SimpleConnector.NorthConnector, mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));

        /* TODO - bring this back
        List<SectorGroup> groups = palette.getAllGroupsWith(SimpleConnector.NorthConnector);
        //if(groups.size() != 3) throw new RuntimeException("something wrong");
        for(SectorGroup sg: groups){

        	if(sg.getGroupId() == 18){
        		// teleport sector
				continue;
			}

        	// TODO  - broken?  NOTE: SimpleConnector constructor maps from 20 to 18
            Connector south = mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector);
            if(south == null){
                throw new RuntimeException("cannot find south connector.  group " + sg.getGroupId());
				// System.out.println("WARNING:  cannot find south connector");
            } else {

				mb.pasteAndLink(sg, SimpleConnector.NorthConnector, south);
			}
        }
        */



        // more random

        //SectorGroup sg = palette.getRandomGroupWith(SimpleConnector.NorthConnector);
        //for(int i = 0; i < 20; ++i){
        //    Connector south = mb.findFirstUnlinkedConnector(SimpleConnector.SouthConnector);
        //    if(south == null){
        //        throw new RuntimeException("cannot find south connector");
        //    }
        //    mb.pasteAndLink(sg, SimpleConnector.NorthConnector, south);
        //}


		
		mb.selectPlayerStart();
		mb.clearMarkers();
		return mb.getOutMap();
	}


	public static Map copytest4(Map fromMap) throws MapErrorException {

		final PrefabPalette palette = PrefabPalette.fromMap(fromMap);
		MapBuilder mb = new MapBuilder(palette);
		//palette.fromMap(fromMap);

		// next ... find all groups with bounding box of a certain size
		for(SectorGroup sg : palette.allSectorGroups()){
			// TODO - add bboxHeight() and bboxWidth() to SectorGroup and print out all bounding box dimensions ...
			System.out.println("sector group " + sg.getGroupId() + " bounding box: " + sg.bbWidth() + " x " + sg.bbHeight());
		}

		//palette.getSectorGroup()

		mb.selectPlayerStart();
		mb.clearMarkers();
		return mb.getOutMap();
    }

}
