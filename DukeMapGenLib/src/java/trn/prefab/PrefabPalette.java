package trn.prefab;

import java.util.*;

import trn.Map;
import trn.MapUtil;
import trn.PointXYZ;
import trn.Sprite;
import trn.duke.MapErrorException;

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

	private Random random = new Random();

	
	public void loadAllGroups(Map map) throws MapErrorException {
		
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
				numberedSectorGroups.put(groupId, new SectorGroup(clipboard, groupId));
				
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
			RedwallConnector destConnector) throws MapErrorException {

	    // TODO - avoid cast here
		RedwallConnector paletteConnector = (RedwallConnector)this.getConnector(sectorGroupId, paletteConnectorId);
		return pasteAndLink(sectorGroupId, paletteConnector, destMap, destConnector);
    }

	public PastedSectorGroup pasteAndLink(
			int sectorGroupId, 
			RedwallConnector paletteConnector,
			Map destMap, 
			RedwallConnector destConnector) throws MapErrorException {

		PointXYZ cdelta = paletteConnector.getTransformTo(destConnector);
				
		PastedSectorGroup result = this.pasteSectorGroup(sectorGroupId, destMap, cdelta);
		
		//paletteConnector = result.getConnector(paletteConnectorId);
		paletteConnector.translateIds(result.copystate.idmap);
		//destMap.linkRedWalls(sectorIndex, wallIndex, sectorIndex2, wallIndex2)
		
		PrefabUtils.joinWalls(destMap, paletteConnector, destConnector);
		
		return result;
	}

	public PastedSectorGroup pasteAndLink(
			SectorGroup sg,
			ConnectorFilter paletteConnectorFilter,
			Map destMap,
			Connector destConnector) throws MapErrorException {

	    // TODO - this method is a hack
		return pasteAndLink(sg, paletteConnectorFilter, destMap, (RedwallConnector)destConnector);
    }

	public PastedSectorGroup pasteAndLink(
			SectorGroup sg,
			ConnectorFilter paletteConnectorFilter,
			Map destMap,
			RedwallConnector destConnector) throws MapErrorException {

		if(destConnector == null){
			throw new IllegalArgumentException("destConnector is null");
		}

		// TODO - avoid cast here
		RedwallConnector paletteConnector = (RedwallConnector)sg.findFirstConnector(paletteConnectorFilter);
		if(paletteConnector == null){
			throw new IllegalArgumentException("cant find connector: " + paletteConnectorFilter);
		}
		
		PointXYZ cdelta = paletteConnector.getTransformTo(destConnector);

		if(paletteConnector.getSectorId() < 0 || paletteConnector.getSectorId() >= sg.getSectorCount() ){
		    throw new RuntimeException("sectorId invalid: " + paletteConnector.getSectorId());
        }

		PastedSectorGroup result = this.pasteSectorGroup(sg, destMap, cdelta);
		
		
		RedwallConnector pastedConnector = paletteConnector.translateIds(result.copystate.idmap);
		//paletteConnector = result.getConnector(paletteConnectorId);
		//destMap.linkRedWalls(sectorIndex, wallIndex, sectorIndex2, wallIndex2)
		
		PrefabUtils.joinWalls(destMap, pastedConnector, destConnector);
		
		return result;
		
	}
	
	public SectorGroup getSectorGroup(int sectorGroupId){
		return this.numberedSectorGroups.get(sectorGroupId);
	}

	public List<SectorGroup> getAllGroupsWith(ConnectorFilter cf){
		// TODO - need source of entropy ...
		List<SectorGroup> results = new ArrayList<SectorGroup>();
		for(SectorGroup sg : this.numberedSectorGroups.values()){
			if(sg.findFirstConnector(cf) != null){
				results.add(sg);
			}
		}
		for(SectorGroup sg : this.anonymousSectorGroups){
			if(sg.findFirstConnector(cf) != null){
				results.add(sg);
			}
		}
		return results;
	}
    public SectorGroup getRandomGroupWith(ConnectorFilter cf){
        List<SectorGroup> results = getAllGroupsWith(cf);
        return results.get(random.nextInt(results.size()));
    }

	/**
	 * 
	 * TODO - need to zero out the groups as we read them in
	 * 
	 * @param sectorGroupId
	 * @param destMap
	 * @param rawTrasform
	 */
	public PastedSectorGroup pasteSectorGroup(int sectorGroupId, Map destMap, PointXYZ rawTrasform) throws MapErrorException {
		return pasteSectorGroup(this.numberedSectorGroups.get(sectorGroupId), destMap, rawTrasform);
	}
	public PastedSectorGroup pasteSectorGroup(SectorGroup sg, Map destMap, PointXYZ rawTrasform) throws MapErrorException {
		Map fromMap = sg.map;
		
		PastedSectorGroup psg = new PastedSectorGroup(
				destMap, 
				MapUtil.copySectorGroup(fromMap, destMap, 0, rawTrasform));

		
		return psg;
	}
	
	public List<Connector> findConnectors(int sectorGroupId, ConnectorFilter... filters){
		//PrefabUtils.findConnector(outMap, PrefabUtils.JoinType.VERTICAL_JOIN, 1);
		//Map map = numberedSectorGroups.get(sectorGroupId).map;
		//return SimpleConnector.findConnectors(map, filters);

		return Connector.matchConnectors(numberedSectorGroups.get(sectorGroupId).connectors, filters);
	}
	
	
	
	
	public Connector getConnector(int sectorGroupId, int connectorId){
		// TODO - shouldnt cast here ...
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
