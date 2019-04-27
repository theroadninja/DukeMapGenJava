package trn.prefab;

import trn.*;
import trn.duke.MapErrorException;

import java.util.LinkedList;
import java.util.List;

public class ConnectorFactory {
		
	public static Connector create(Map map, Sprite markerSprite) throws MapErrorException {
		Sprite s = markerSprite;
		
		Sector sector = map.getSector(s.getSectorId());
		
		
		if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.SIMPLE_CONNECTOR) {

			if (map.findSprites(null, DukeConstants.SE_LOTAGS.TELEPORT, (int) s.getSectorId()).size() > 0) {
				//its a teleporter
				return new TeleportConnector(s, sector.getLotag());
			} else if (ElevatorConnector.isElevatorMarker(map, s)) {
				System.out.println("WARNING: auto connector used to create elevator (DEPRECATED - See ConnectorFactory.java");
				return new ElevatorConnector(s);
			} else {
				List<Integer> linkWallIds = getLinkWallIds(map, sector);

				if(linkWallIds.size() == 0){
					throw new SpriteLogicException("missing link wall for marker sprite at " + s.getLocation().toString());
				}else if(linkWallIds.size() == 1) {
					int wallId = linkWallIds.get(0);
					//int wallId = getLinkWallId(map, sector); // TODO - get rid of getLinkWallId
					Wall w1 = map.getWall(wallId);
					Wall w2 = map.getWall(w1.getPoint2Id());
					int z = map.getSector(s.getSectorId()).getFloorZ();
					return new SimpleConnector(s, wallId, w1, w2, z);
				}else{
					return new MultiWallConnector(s, sector, linkWallIds, map);
				}

			}

		}else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR) {
			return new TeleportConnector(s, sector.getLotag());
		}else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR){
			if(sector.getLotag() != 15){
				throw new SpriteLogicException("elevector connector in sector id with lotag != 15");
			}
			return new ElevatorConnector(s);
		}else{
			//throw new SpriteLogicException("sprite lotag=" + s.getLotag())
			return null;
		}
	}
	

	private static int getLinkWallId(Map map, Sector sector){
		final int linkLotag = 1;
		
		Iterable<Integer> wallIds = map.getAllSectorWallIds(sector);
		Integer wallId = null;
		
		for(int i: wallIds){
			Wall w = map.getWall(i);
			if(w.getLotag() == linkLotag){
				if(wallId == null){
					wallId = i;
				}else{
					throw new SpriteLogicException();
				}
			}
		}
		if(wallId == null){
		    // wait... what about teleporting connectors??
			int x = map.getWall(sector.getFirstWall()).getX();
			int y = map.getWall(sector.getFirstWall()).getY();
			throw new SpriteLogicException(String.format("cannot find link wall for sector at %s, %s ", x, y));
		}
		return wallId;
	}

	private static List<Integer> getLinkWallIds(Map map, Sector sector){
		final int linkLotag = 1;
		Iterable<Integer> wallIds = map.getAllSectorWallIds(sector);
		List<Integer> results = new LinkedList<>();
		for(int i: wallIds){
			Wall w = map.getWall(i);
			if(w.getLotag() == linkLotag){
				results.add(i);
			}
		}
		return results;
	}

}
