package trn.prefab;

import duchy.sg.SimpleConnectorScanner$;
import trn.*;
import trn.Map;
import trn.duke.MapErrorException;

import java.util.*;

/**
 * Note:  the MAIN code for scanning a map is currently PrefabPallete.fromMap()
 */
public class ConnectorFactory {


	// public static List<Connector> findConnectors(Map map) throws MapErrorException {
	// 	return findConnectors(map, null);
	// }

	// public static Iterable<Connector> findConnectors(Iterable<Connector> connectors, ConnectorFilter ... cf){
	// 	List<Connector> results = new ArrayList<Connector>();
	// 	for(Connector c: connectors){
	// 		if(cf == null ||  ConnectorFilter.allMatch(c, cf)){
	// 			results.add(c);
	// 		}
	// 	}
	// 	return results;
	// }

	// // called by SectorGroup.newSG() during sector group scan
	// private static List<Connector> findConnectors(Map map, ConnectorFilter ... cf) throws MapErrorException {
	// 	List<Connector> results = new ArrayList<Connector>();
	// 	for(Sprite s: map.findSprites(
	// 			PrefabUtils.MARKER_SPRITE)){

	// 		Sector sector = map.getSector(s.getSectorId());

	// 		Connector connector = ConnectorFactory2.create(new MapView(map), s);

	// 		// WARNING:  this logic doesnt work when called from scala! (something goes wrong with varargs mapping)
	// 		if(connector != null && (cf == null || cf.length == 0 || ConnectorFilter.allMatch(connector, cf))){
	// 			results.add(connector);
	// 		}
	// 	} // for sprite

	// 	List<Connector> multiSectorResults = ConnectorScanner.findMultiSectorConnectors(new MapView(map));
	// 	results.addAll(multiSectorResults);

	// 	return results;
	// }

	// // this is called from PastedSectorGroup in the scala code.
	// public static List<Connector> findConnectorsInPsg(Map map, CopyState copystate) throws MapErrorException {
	// 	ConnectorFilter cf = new ConnectorFilter(){
	// 		@Override
	// 		public boolean matches(Connector c) {
	// 			return copystate.destSectorIds().contains(c.getSectorId());
	// 		}
	// 	};

	// 	// TODO find or create a good unit test that covers this, and then refactor this to
	// 	// TODO be something like findConnectors(map, null).filter(cf)
	// 	return findConnectors(map, cf);
	// }

	// public static List<Connector> findConnectorsInPsg(Map map, IdMap idmap) throws MapErrorException {
	// 	ConnectorFilter cf = new ConnectorFilter(){
	// 		@Override
	// 		public boolean matches(Connector c) {
	// 			return idmap.hasDestSectorId(c.getSectorId());
	// 		}
	// 	};

	// 	// TODO find or create a good unit test that covers this, and then refactor this to
	// 	// TODO be something like findConnectors(map, null).filter(cf)
	// 	return findConnectors(map, cf);

	// }

	/* **************************************************************************** */

	// private static int getLinkWallId(Map map, Sector sector){
	// 	final int linkLotag = 1;
	//
	// 	Iterable<Integer> wallIds = map.getAllSectorWallIds(sector);
	// 	Integer wallId = null;
	//
	// 	for(int i: wallIds){
	// 		Wall w = map.getWall(i);
	// 		if(w.getLotag() == linkLotag){
	// 			if(wallId == null){
	// 				wallId = i;
	// 			}else{
	// 				throw new SpriteLogicException();
	// 			}
	// 		}
	// 	}
	// 	if(wallId == null){
	// 	    // wait... what about teleporting connectors??
	// 		int x = map.getWall(sector.getFirstWall()).getX();
	// 		int y = map.getWall(sector.getFirstWall()).getY();
	// 		throw new SpriteLogicException(String.format("cannot find link wall for sector at %s, %s ", x, y));
	// 	}
	// 	return wallId;
	// }

	// private static List<Integer> getLinkWallIds(MapView map, Sector sector){
	// 	final int linkLotag = 1;
	// 	Iterable<Integer> wallIds = map.getAllSectorWallIds(sector);
	// 	List<Integer> results = new LinkedList<>();
	// 	for(int i: wallIds){
	// 		Wall w = map.getWall(i);
	// 		if(w.getLotag() == linkLotag){
	// 			results.add(i);
	// 		}
	// 	}
	// 	return results;
	// }

}
