package trn.prefab;

import java.util.*;

import trn.*;
import trn.Map;
import trn.duke.MapErrorException;
import trn.javax.MultiIterable;

/**
 * Stores sector groups for use.
 * 
 * @author Dave
 *
 */
public class PrefabPalette {

	// TODO - make private again
	public final java.util.Map<Integer, SectorGroup> numberedSectorGroups;
	
	/** sector groups that dont have ids */
	private final List<SectorGroup> anonymousSectorGroups;

	private Random random = new Random();


	public PrefabPalette(
			java.util.Map<Integer, SectorGroup> numberedSectorGroups,
			List<SectorGroup> anonymousSectorGroups
	){
		this.numberedSectorGroups = numberedSectorGroups;
		this.anonymousSectorGroups = anonymousSectorGroups;
	}

	public Iterable<SectorGroup> allSectorGroups(){
		return new MultiIterable<SectorGroup>(numberedSectorGroups.values(), anonymousSectorGroups);
	}

	public static PrefabPalette fromMap(Map map) throws MapErrorException {
		return fromMap(map, false);
	}
	public static PrefabPalette fromMap(Map map, boolean strict) throws MapErrorException {
	    final List<SectorGroup> sectorGroupsThatStay = new ArrayList<>();
		final java.util.Map<Integer, SectorGroup> numberedSectorGroups = new java.util.TreeMap<>();
		final java.util.Map<Integer, List<SectorGroup>> redwallChildren = new java.util.TreeMap<>();
		final List<SectorGroup> anonymousSectorGroups = new ArrayList<>();

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
			SectorGroupProperties props = SectorGroupProperties.scanMap(clipboard);

			processedSectorIds.addAll(cpstate.sourceSectorIds());
			
			List<Sprite> idSprite = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.GROUP_ID, null);
			List<Sprite> childPointer = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.REDWALL_CHILD, null);
			if(idSprite.size() + childPointer.size() > 1){
				throw new SpriteLogicException("too many group id sprites in sector group");
			}else if(idSprite.size() == 1) {
				int groupId = idSprite.get(0).getHiTag();
				if (numberedSectorGroups.containsKey(groupId)) {
					throw new SpriteLogicException("more than one sector group with id " + groupId);
				}
				//numberedSectorGroups.put(groupId, new SectorGroup(clipboard, groupId));
				SectorGroup sg = SectorGroupBuilder.createSectorGroup(clipboard, groupId, props);
				//numberedSectorGroups.put(groupId, SectorGroupBuilder.createSectorGroup(clipboard, groupId));
				numberedSectorGroups.put(groupId, sg);

			}else if(childPointer.size() == 1){

				SectorGroup childGroup = SectorGroupBuilder.createSectorGroup(clipboard, props); // new SectorGroup(clipboard);
				int groupId = childPointer.get(0).getHiTag();
				// make sure the sector with the child Id sprite also has a redwall connector marker
                // Connector conn = childGroup.findFirstConnector(c -> c.getSectorId() == childPointer.get(0).getSectorId()
				// 		&& ConnectorType.isRedwallType(c.getConnectorType()));
				ChildPointer childPtr = childGroup.getChildPointer();
				if(childPtr.connectorId() == 0){
                	throw new SpriteLogicException("child pointer connector must have a connector ID");
				}
				redwallChildren.putIfAbsent(groupId, new LinkedList<>());
				redwallChildren.get(groupId).add(childGroup);
			}else{
				List<Sprite> mistakes = clipboard.findSprites(0, PrefabUtils.MarkerSpriteLoTags.GROUP_ID, null);
				if(mistakes.size() > 0){
					throw new SpriteLogicException("Sector group has no ID marker sprite but it DOES have a sprite with texture 0");
				}
				anonymousSectorGroups.add(SectorGroupBuilder.createSectorGroup(clipboard, props)); //new SectorGroup(clipboard));
			}

			if(strict){ // TODO - get rid of this strict thing
				List<Sprite> anchorSprites = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.ANCHOR, null);
				if(anchorSprites.size() > 1){
					throw new SpriteLogicException("more than one anchor sprite in group");
				}
			}
			// make sure all children have parents
			for(Integer parentId: redwallChildren.keySet()){
				if(! numberedSectorGroups.containsKey(parentId)){
					int count = redwallChildren.get(parentId).size();
					throw new SpriteLogicException("There is no sector group with ID " + parentId + ", referenced by " + count + " child sectors");
				}
			}
			
			sector++;
		} // while

		TagGenerator tagGenerator = new SimpleTagGenerator(500);
		// now process the children
		final java.util.Map<Integer, SectorGroup> numberedGroups2 = new java.util.TreeMap<>();
		for(Integer groupId : numberedSectorGroups.keySet()){
			SectorGroup sg = numberedSectorGroups.get(groupId);
			if(redwallChildren.containsKey(groupId)){
			    numberedGroups2.put(groupId, sg.connectedToChildren(redwallChildren.get(groupId), tagGenerator));
			}else{
				numberedGroups2.put(groupId, sg);
			}
		}
		return new PrefabPalette(numberedGroups2, anonymousSectorGroups);

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
		paletteConnector.translateIds(result.copystate.idmap, cdelta);
		//destMap.linkRedWalls(sectorIndex, wallIndex, sectorIndex2, wallIndex2)
		
		//PrefabUtils.joinWalls(destMap, paletteConnector, destConnector);
		paletteConnector.linkConnectors(destMap, destConnector);
		
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
		
		
		RedwallConnector pastedConnector = paletteConnector.translateIds(result.copystate.idmap, cdelta);
		//paletteConnector = result.getConnector(paletteConnectorId);
		//destMap.linkRedWalls(sectorIndex, wallIndex, sectorIndex2, wallIndex2)
		
		//PrefabUtils.joinWalls(destMap, pastedConnector, destConnector);
		pastedConnector.linkConnectors(destMap, destConnector);

		return result;
		
	}
	
	public SectorGroup getSectorGroup(int sectorGroupId){
	    if(!this.numberedSectorGroups.containsKey(sectorGroupId)) throw new NoSuchElementException();
		return this.numberedSectorGroups.get(sectorGroupId);
	}

	public List<SectorGroup> getStaySectorGroups(){
		List<SectorGroup> results = new ArrayList<>();
		for(SectorGroup sg : this.numberedSectorGroups.values()){
			if(sg.props().stayFlag()){
				results.add(sg);
			}
		}
		for(SectorGroup sg : this.anonymousSectorGroups){
			if(sg.props().stayFlag()){
				results.add(sg);
			}
		}
		return results;

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
		Map fromMap = sg.map();
		
		PastedSectorGroup psg = new PastedSectorGroup(
				destMap, 
				MapUtil.copySectorGroup(fromMap, destMap, 0, rawTrasform));

		
		return psg;
	}
	
	// public List<Connector> findConnectors(int sectorGroupId, ConnectorFilter... filters){
	// 	//PrefabUtils.findConnector(outMap, PrefabUtils.JoinType.VERTICAL_JOIN, 1);
	// 	//Map map = numberedSectorGroups.get(sectorGroupId).map;
	// 	//return SimpleConnector.findConnectors(map, filters);

	// 	return Connector.matchConnectors(numberedSectorGroups.get(sectorGroupId).connectors, filters);
	// }
	
	
	
	
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
