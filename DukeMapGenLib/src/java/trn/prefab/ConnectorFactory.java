package trn.prefab;

import trn.*;
import trn.Map;
import trn.duke.MapErrorException;

import java.util.*;

public class ConnectorFactory {
		
	public static Connector create(Map map, Sprite markerSprite) throws MapErrorException {
		Sprite s = markerSprite;
		
		Sector sector = map.getSector(s.getSectorId());

		if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.SIMPLE_CONNECTOR) {

			if (map.findSprites(null, DukeConstants.SE_LOTAGS.TELEPORT, (int) s.getSectorId()).size() > 0) {
				// TODO - delete this code when I'm confident I dont use it anywhere
			    throw new SpriteLogicException("not supported anymore", s);
				//return new TeleportConnector(s, sector.getLotag());
			} else if (ElevatorConnector.isElevatorMarker(map, s)) {
				// TODO - delete this code when I'm confident I dont use it anywhere
				throw new SpriteLogicException("not supported anymore lotag=" + s.getLotag(), s);
			    //return new ElevatorConnector(s);
			} else {

				// TODO - this code has a bug that causes it to run forever if every wall in the sector is marked with 1

				List<Integer> linkWallIds = getLinkWallIds(map, sector);

				if (linkWallIds.size() == 0) {
					throw new SpriteLogicException("missing link wall for marker sprite at " + s.getLocation().toString());
				}

				// how many different groups of contiguous lotag-1 walls are there?
				List<List<Integer>> partitions = partitionWalls(linkWallIds, map);

				if (partitions.size() < 1) {
					throw new RuntimeException("programming error");
				} else if (partitions.size() == 1) {
					// only one wall segment, assume the connector matches
                    // TODO get rid of this ability (valid connector without pointing the sprite at the wall) ?
					return redWallConn(s, sector, partitions.get(0), map);
				} else {
					// figure out which group the  marker sprite is pointing to
					for (int partIdx = 0; partIdx < partitions.size(); ++partIdx) {
						if (matches(s, partitions.get(partIdx), map)) {
							return redWallConn(s, sector, partitions.get(partIdx), map);
						}
					}
					throw new SpriteLogicException("Cannot match connector to its walls (point the sprite at the correct wall(s)");
				}
			}

		}else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR) {
			return new TeleportConnector(s, sector.getLotag());
		}else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR){
			if(sector.getLotag() != 15){
				throw new SpriteLogicException("elevector connector in sector with lotag != 15");
			}
			return new ElevatorConnector(s);
		}else{
			return null;
		}
	}

	private static RedwallConnector redWallConn(Sprite s, Sector sector, List<Integer> wallIds, Map map) throws MapErrorException {
		List<Integer> wallIds2 = MapUtil.sortWallSection(wallIds, map);
		List<WallView> walls = MapUtil.getWallViews(wallIds2, map);
		return RedwallConnector.create(s, sector, wallIds, walls, map);
	}

	private static boolean matches(Sprite marker, List<Integer> walls, Map map){
	    //System.out.println("entering matches()");
		for(int i = 0; i < walls.size(); ++i){
			//System.out.println("wall id=" + walls.get(i) + " location: " + map.getWall(walls.get(i)).getLocation());
			if(MapUtil.isSpritePointedAtWall(marker, walls.get(i), map)){
				return true;
			}else{
			}
		}
		return false;
	}

	// TODO - compare to MapUtil.sortWallSection()
	// TODO - compare to ConnectorScanner.sortContinuousWalls() which should become the canonical form of this algorithm
	static List<List<Integer>> partitionWalls(List<Integer> wallIds, WallContainer map){
		//System.out.print("parition input: " );
		//for(int i = 0; i < wallIds.size(); ++i){
		//	System.out.print("" + wallIds.get(i) + ", ");
		//}
		//System.out.println();

		// TODO - this is a classic topological, so optimize this enough to not be embarassing
		if(wallIds == null) throw new IllegalArgumentException();
		java.util.Map<Integer, Integer> walls = new TreeMap<>();
		java.util.Map<Integer, Integer> wallsReversed = new TreeMap<>();
		for(Integer wallId : wallIds) {
			Wall w = map.getWall(wallId);
			if (walls.containsKey(wallId)) {
				throw new IllegalArgumentException("list of wall ids contains duplicate");
			} else {
				walls.put(wallId, w.getPoint2Id());
				wallsReversed.put(w.getPoint2Id(), wallId);
			}
		}
		Set<Integer> openList = new TreeSet<>(wallIds);

		List<List<Integer>> results = new ArrayList<>(wallIds.size());
		while(!openList.isEmpty()){
			LinkedList<Integer> segment = new LinkedList<>();
			Iterator<Integer> it = openList.iterator();
			segment.add(it.next());
			it.remove();
			while(wallsReversed.containsKey(segment.getFirst())){
				segment.addFirst(wallsReversed.get(segment.getFirst()));
				openList.remove(segment.getFirst());
			}
			while(walls.containsKey(segment.getLast())){
				segment.addLast(walls.get(segment.getLast()));
				openList.remove(segment.getLast());
			}
			segment.removeLast();
			results.add(segment);
		}

		return results;
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
