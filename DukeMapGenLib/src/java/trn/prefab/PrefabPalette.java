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
 * TODO move this to the scala code as soon as I get compilation working again
 */
public class PrefabPalette {

    /** sector groups that have ids (set with a marker sprite) */
	private final java.util.Map<Integer, SectorGroup> numberedSectorGroups;

	/**
	 * Sector groups that are each a "child" group of a sector group, but which are not connected by a redwall.
	 * Because they can be pasted anywhere (and probably not next to their parent) they need to remain separate
	 * objects.
	 */
	private final java.util.Map<Integer, List<SectorGroup>> teleportChildGroups;

	/** sector groups that dont have ids */
	private final List<SectorGroup> anonymousSectorGroups;

	public PrefabPalette(
			java.util.Map<Integer, SectorGroup> numberedSectorGroups,
			java.util.Map<Integer, List<SectorGroup>> teleportChildGroups,
			List<SectorGroup> anonymousSectorGroups
	){
		this.numberedSectorGroups = numberedSectorGroups;
		this.teleportChildGroups = teleportChildGroups;
		this.anonymousSectorGroups = anonymousSectorGroups;
	}

	public final java.util.Set<Integer> numberedSectorGroupIds(){
		return numberedSectorGroups.keySet();
	}

	public final Iterable<SectorGroup> anonSectorGroups(){
		return anonymousSectorGroups;
	}

	// NOTE: you can do asScala on this
	public Iterable<SectorGroup> allSectorGroups(){
		return new MultiIterable<SectorGroup>(numberedSectorGroups.values(), anonymousSectorGroups);
	}

	public static PrefabPalette fromMap(Map map) throws MapErrorException {
		return fromMap(map, false);
	}

	/**
	 * @deprecated - GameConfig needs to be passed in
	 */
	public static PrefabPalette fromMap(Map map, boolean strict) throws MapErrorException {
		GameConfig cfg = DukeConfig.loadHardCodedVersion();
		return fromMap(cfg, map, strict);

	}
	public static PrefabPalette fromMap(GameConfig cfg, Map map, boolean strict) throws MapErrorException {
	    final List<SectorGroup> sectorGroupsThatStay = new ArrayList<>();
		final java.util.Map<Integer, SectorGroup> numberedSectorGroups = new java.util.TreeMap<>();
		final java.util.Map<Integer, List<SectorGroup>> redwallChildren = new java.util.TreeMap<>();
		final java.util.Map<Integer, List<SectorGroup>> teleportChildren = new java.util.TreeMap<>();

		final List<SectorGroup> anonymousSectorGroups = new ArrayList<>();

		Set<Short> processedSectorIds = new TreeSet<Short>();
		
		short sector = 0;
		while(sector < map.getSectorCount()){
			//short sector = nextSectorId(-1, processedSectorIds, map.getSectorCount());
			
			if(processedSectorIds.contains(sector)){
				sector++;
				continue;
			}

			// copy the next entire sector group
			Map clipboard = Map.createNew();
			CopyState cpstate = MapUtil.copySectorGroup(cfg, map, clipboard, sector, new PointXYZ(0, 0, 0));
			SectorGroupProperties props = SectorGroupProperties.scanMap(clipboard);
			SectorGroupHints hints = SectorGroupHints.apply(clipboard); // NOTE: these need to get re-applied after children
			for(int i = 0; i < clipboard.getSpriteCount(); ++i){
				Sprite s = clipboard.getSprite(i);
				PrefabUtils.checkValid(s);
			}
			processedSectorIds.addAll(cpstate.sourceSectorIds());
			// process z adjust
			if(props.zAdjust().isDefined()){
				System.out.println("doing z transform");
			    // TODO - unit test this feature!
				clipboard = new MapImplicits.MapExtended(clipboard).translated(props.zAdjustTrx());
			}

			List<Sprite> idSprite = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.GROUP_ID, null);
			List<Sprite> teleChildPointer = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.TELEPORT_CHILD, null);
			List<Sprite> childPointer = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.REDWALL_CHILD, null);
			if(idSprite.size() + childPointer.size() + teleChildPointer.size() > 1){
				throw new SpriteLogicException("too many group id, redwall child, or teleport child sprites in sector group");
			}else if(idSprite.size() == 1) {
				int groupId = idSprite.get(0).getHiTag();
				if (numberedSectorGroups.containsKey(groupId)) {
					throw new SpriteLogicException("more than one sector group with id " + groupId);
				}
				SectorGroup sg = SectorGroupBuilder.createSectorGroup(clipboard, groupId, props, hints);
				numberedSectorGroups.put(groupId, sg);

			}else if(childPointer.size() == 1) {

				SectorGroup childGroup = SectorGroupBuilder.createSectorGroup(clipboard, props, hints); // new SectorGroup(clipboard);
				int groupId = childPointer.get(0).getHiTag();
				// make sure the sector with the child Id sprite also has a redwall connector marker
				// Connector conn = childGroup.findFirstConnector(c -> c.getSectorId() == childPointer.get(0).getSectorId()
				// 		&& ConnectorType.isRedwallType(c.getConnectorType()));
				ChildPointer childPtr = childGroup.getChildPointer();
				if (childPtr.connectorId() == 0) {
					throw new SpriteLogicException("child pointer connector must have a connector ID");
				}
				redwallChildren.putIfAbsent(groupId, new LinkedList<>());
				redwallChildren.get(groupId).add(childGroup);
			}else if(teleChildPointer.size() == 1){
			    SectorGroup teleChildGroup = SectorGroupBuilder.createSectorGroup(clipboard, props, hints);
			    int groupId = teleChildPointer.get(0).getHiTag();

			    // TODO - need equialent of stuff in getChildPoint(), checking connectors, etc
				// TODO - somewhere, need to verify that the parent exists

				teleportChildren.putIfAbsent(groupId, new LinkedList<>());
				teleportChildren.get(groupId).add(teleChildGroup);

			}else{
				List<Sprite> mistakes = clipboard.findSprites(0, PrefabUtils.MarkerSpriteLoTags.GROUP_ID, null);
				if(mistakes.size() > 0){
					throw new SpriteLogicException("Sector group has no ID marker sprite but it DOES have a sprite with texture 0");
				}
				anonymousSectorGroups.add(SectorGroupBuilder.createSectorGroup(clipboard, props, hints)); //new SectorGroup(clipboard));
			}

			if(strict){ // TODO - get rid of this strict thing
				List<Sprite> anchorSprites = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.ANCHOR, null);
				if(anchorSprites.size() > 1){
					throw new SpriteLogicException("more than one anchor sprite in group");
				}
			}
			// // make sure all children have parents
			// for(Integer parentId: redwallChildren.keySet()){
			// 	if(! numberedSectorGroups.containsKey(parentId)){
			// 		int count = redwallChildren.get(parentId).size();
			// 		throw new SpriteLogicException("There is no sector group with ID " + parentId + ", referenced by " + count + " child sectors");
			// 	}
			// }
			
			sector++;
		} // while

		// make sure all children have parents
		for(Integer parentId: redwallChildren.keySet()){
			if(! numberedSectorGroups.containsKey(parentId)){
				int count = redwallChildren.get(parentId).size();
				throw new SpriteLogicException("There is no sector group with ID " + parentId + ", referenced by " + count + " child sectors");
			}
		}
		for(Integer parentId: teleportChildren.keySet()){
			if(! numberedSectorGroups.containsKey(parentId)){
				throw new SpriteLogicException("No sector group with ID " + parentId + " referenced by teleport child group");
			}
		}

		TagGenerator tagGenerator = new SimpleTagGenerator(500); // TODO - should be passed in
		// now process the children
		final java.util.Map<Integer, SectorGroup> numberedGroups2 = new java.util.TreeMap<>();
		for(Integer groupId : numberedSectorGroups.keySet()){
			SectorGroup sg = numberedSectorGroups.get(groupId);
			if(redwallChildren.containsKey(groupId)){
			    numberedGroups2.put(groupId, sg.connectedToChildren2(redwallChildren.get(groupId), tagGenerator, cfg));
			}else{
				numberedGroups2.put(groupId, sg);
			}
		}
		return new PrefabPalette(numberedGroups2, teleportChildren, anonymousSectorGroups);

	}

	public SectorGroup getSectorGroup(int sectorGroupId){
	    if(!this.numberedSectorGroups.containsKey(sectorGroupId)) throw new NoSuchElementException();
		return this.numberedSectorGroups.get(sectorGroupId);
	}

	public SectorGroup getSG(int sectorGroupId){
		return this.getSectorGroup(sectorGroupId);
	}

	/**
	 * get a sector group AND all of its "teleport children" -- sectors that belong to it logically (and
	 * which share unique tag values with it) but are not connected via redwalls.
	 *
	 * @param sectorGroupId
	 * @return
	 */
	public CompoundGroup getCompoundGroup(int sectorGroupId){
	    return CompoundGroup$.MODULE$.apply(getSG(sectorGroupId), getTeleChildren(sectorGroupId));
	}

	public CompoundGroup getCompoundSG(int sectorGroupId){
		return this.getCompoundGroup(sectorGroupId);
	}

	// will return an empty list if the sector group does not have teleport children
	public List<SectorGroup> getTeleChildren(int parentSectorGroupId){
	    this.teleportChildGroups.putIfAbsent(parentSectorGroupId, Collections.emptyList());
		return this.teleportChildGroups.get(parentSectorGroupId);
	}

	public int numberedSectorGroupCount(){
		return this.numberedSectorGroups.size();
	}
	public int anonymousSectorGroupCount(){
		return this. anonymousSectorGroups.size();
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
}
