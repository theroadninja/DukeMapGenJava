package trn.prefab;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import trn.Map;
import trn.MapUtil;
import trn.PointXYZ;
import trn.Sprite;

/**
 * Stores sector groups for use.
 * 
 * @author Dave
 *
 */
public class PrefabPalette {
	
	private java.util.Map<Integer, SectorGroup> numberedSectorGroups = new TreeMap<Integer, SectorGroup>();
	
	/** sector groups that dont have ids */
	private List<SectorGroup> anonymousSectorGroups = new ArrayList<SectorGroup>();

	
	public void loadAllGroups(Map map){
		
		Set<Short> processedSectorIds = new TreeSet<Short>();
		
		short sector = 0;
		while(sector < map.getSectorCount()){
			//short sector = nextSectorId(-1, processedSectorIds, map.getSectorCount());
			
			if(processedSectorIds.contains(sector)){
				sector++;
				continue;
			}
			
			Map clipboard = Map.createNew();
			MapUtil.CopyState cpstate = MapUtil.copySectorGroup(map, clipboard, sector, new PointXYZ(0, 0, 0));
			
			processedSectorIds.addAll(cpstate.sourceSectorIds());
			
			List<Sprite> idSprite = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.SpriteLoTags.GROUP_ID, null);
			if(idSprite.size() > 1){
				throw new SpriteLogicException("too many group id sprites in sector group");
			}else if(idSprite.size() == 1){
				int groupId = idSprite.get(0).getHiTag();
				if(numberedSectorGroups.containsKey(groupId)){
					throw new SpriteLogicException("more than one sector group with id " + groupId);
				}
				numberedSectorGroups.put(groupId, new SectorGroup(clipboard));
				
			}else{
				anonymousSectorGroups.add(new SectorGroup(clipboard));
			}
			
			sector++;
		} // while

	}
	
	
	
	public PastedSectorGroup pasteAndLink(
			int sectorGroupId, 
			int paletteConnectorId, 
			Map destMap, 
			Connector destConnector){
		
		Connector paletteConnector = this.getConnector(sectorGroupId, paletteConnectorId);
		
		PointXYZ cdelta = paletteConnector.getTransformTo(destConnector);
				
		PastedSectorGroup result = this.pasteSectorGroup(sectorGroupId, destMap, cdelta);
		
		paletteConnector = result.getConnector(paletteConnectorId);
		//destMap.linkRedWalls(sectorIndex, wallIndex, sectorIndex2, wallIndex2)
		
		PrefabUtils.joinWalls(destMap, paletteConnector, destConnector);
		
		return result;
	}
	
	public PastedSectorGroup pasteAndLink(
			int sectorGroupId,
			ConnectorFilter paletteConnectorFilter,
			Map destMap,
			Connector destConnector){
		
		if(destConnector == null){
			throw new IllegalArgumentException("destConnector is null");
		}
		
		Connector paletteConnector = getSectorGroup(sectorGroupId).findFirstConnector(paletteConnectorFilter);
		if(paletteConnector == null){
			throw new IllegalArgumentException("cant find connector: " + paletteConnectorFilter);
		}
		
		PointXYZ cdelta = paletteConnector.getTransformTo(destConnector);
		PastedSectorGroup result = this.pasteSectorGroup(sectorGroupId, destMap, cdelta);
		
		
		paletteConnector.translateIds(result.copystate.idmap);
		//paletteConnector = result.getConnector(paletteConnectorId);
		//destMap.linkRedWalls(sectorIndex, wallIndex, sectorIndex2, wallIndex2)
		
		PrefabUtils.joinWalls(destMap, paletteConnector, destConnector);
		
		return result;
		
	}
	
	private SectorGroup getSectorGroup(int sectorGroupId){
		return this.numberedSectorGroups.get(sectorGroupId);
	}
	
	/**
	 * 
	 * TODO - need to zero out the groups as we read them in
	 * 
	 * @param sectorGroupId
	 * @param destMap
	 * @param rawTrasform
	 */
	public PastedSectorGroup pasteSectorGroup(int sectorGroupId, Map destMap, PointXYZ rawTrasform){
		Map fromMap = this.numberedSectorGroups.get(sectorGroupId).map;
		
		PastedSectorGroup psg = new PastedSectorGroup(
				destMap, 
				MapUtil.copySectorGroup(fromMap, destMap, 0, rawTrasform));

		
		return psg;
	}
	
	public List<Connector> findConnectors(int sectorGroupId, ConnectorFilter... filters){
		//PrefabUtils.findConnector(outMap, PrefabUtils.JoinType.VERTICAL_JOIN, 1);
		//Map map = numberedSectorGroups.get(sectorGroupId).map;
		//return Connector.findConnectors(map, filters);

		return Connector.matchConnectors(numberedSectorGroups.get(sectorGroupId).connectors, filters);
	}
	
	
	
	
	public Connector getConnector(int sectorGroupId, int connectorId){
		return numberedSectorGroups.get(sectorGroupId).getConnector(connectorId);
	}
	
	
	private static short nextSectorId(int currentSector, Set<Short> processedSectorIds, int sectorCount){
		short i = 0;
		if(currentSector > 0){
			i = (short)currentSector; 
		}
		while(i < sectorCount){
			
			if(processedSectorIds.contains(i)){
				i++;
			}else{
				return i;
			}
		}
		
		return -1;
	}
}
