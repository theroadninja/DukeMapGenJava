package trn.prefab;

import trn.*;
import trn.duke.MapErrorException;

public class ConnectorFactory {
		
	public static Connector create(Map map, Sprite markerSprite) throws MapErrorException {
		Sprite s = markerSprite;
		
		Sector sector = map.getSector(s.getSectorId());
		
		
		if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.HORIZONTAL_CONNECTOR_EAST
				|| s.getLotag() == PrefabUtils.MarkerSpriteLoTags.HORIZONTAL_CONNECTOR_WEST){
			
			

			int wallId = getLinkWallId(map, sector);
			Wall w = map.getWall(wallId);
			
			SimpleConnector connector = new SimpleConnector(s, wallId, w, sector);
			PointXYZ anchor = SimpleConnector.getHorizontalConnectorAnchor(w, map.getWall(w.getPoint2Id()), sector.getFloorZ());
			connector.setAnchorPoint(anchor);
			return connector;


		}else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.VERTICAL_CONNECTOR_NORTH
				|| s.getLotag() == PrefabUtils.MarkerSpriteLoTags.VERTICAL_CONNECTOR_SOUTH) {

			int wallId = getLinkWallId(map, sector);

			Wall w = map.getWall(wallId);
			SimpleConnector connector = new SimpleConnector(s, wallId, w, sector);

			int z = map.getSector(s.getSectorId()).getFloorZ();

			PointXYZ anchor = SimpleConnector.getVerticalConnectorAnchor(w, map.getWall(w.getPoint2Id()), z);
			connector.setAnchorPoint(anchor);
			return connector;

		}else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.SIMPLE_CONNECTOR) {

			if (map.findSprites(null, DukeConstants.SE_LOTAGS.TELEPORT, (int) s.getSectorId()).size() > 0) {
				//its a teleporter
				return new TeleportConnector(s, sector.getLotag());
			} else if (ElevatorConnector.isElevatorMarker(map, s)) {
				return new ElevatorConnector(s);
			} else {
				int wallId = getLinkWallId(map, sector);
				Wall w1 = map.getWall(wallId);
				Wall w2 = map.getWall(w1.getPoint2Id());
				int z = map.getSector(s.getSectorId()).getFloorZ();
				return new SimpleConnector(s, wallId, w1, w2, z);
			}

		}else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR){
			return new TeleportConnector(s, sector.getLotag());
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

}
