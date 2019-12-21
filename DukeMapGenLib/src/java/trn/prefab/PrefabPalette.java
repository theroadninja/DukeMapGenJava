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

    /** sector groups that have ids (set with a marker sprite) */
	private final java.util.Map<Integer, SectorGroup> numberedSectorGroups;

	/** sector groups that dont have ids */
	private final List<SectorGroup> anonymousSectorGroups;

	public PrefabPalette(
			java.util.Map<Integer, SectorGroup> numberedSectorGroups,
			List<SectorGroup> anonymousSectorGroups
	){
		this.numberedSectorGroups = numberedSectorGroups;
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
			SectorGroupHints hints = SectorGroupHints.apply(clipboard); // NOTE: these need to get re-applied after children

			for(int i = 0; i < clipboard.getSpriteCount(); ++i){
				Sprite s = clipboard.getSprite(i);
				PrefabUtils.checkValid(s);

			}

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
				SectorGroup sg = SectorGroupBuilder.createSectorGroup(clipboard, groupId, props, hints);
				//numberedSectorGroups.put(groupId, SectorGroupBuilder.createSectorGroup(clipboard, groupId));
				numberedSectorGroups.put(groupId, sg);

			}else if(childPointer.size() == 1){

				SectorGroup childGroup = SectorGroupBuilder.createSectorGroup(clipboard, props, hints); // new SectorGroup(clipboard);
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


		TagGenerator tagGenerator = new SimpleTagGenerator(500);
		// now process the children
		final java.util.Map<Integer, SectorGroup> numberedGroups2 = new java.util.TreeMap<>();
		for(Integer groupId : numberedSectorGroups.keySet()){
			SectorGroup sg = numberedSectorGroups.get(groupId);
			if(redwallChildren.containsKey(groupId)){
			    numberedGroups2.put(groupId, sg.connectedToChildren2(redwallChildren.get(groupId), tagGenerator));
			}else{
				numberedGroups2.put(groupId, sg);
			}
		}
		return new PrefabPalette(numberedGroups2, anonymousSectorGroups);

	}

	public SectorGroup getSectorGroup(int sectorGroupId){
	    if(!this.numberedSectorGroups.containsKey(sectorGroupId)) throw new NoSuchElementException();
		return this.numberedSectorGroups.get(sectorGroupId);
	}

	public SectorGroup getSG(int sectorGroupId){
		return this.getSectorGroup(sectorGroupId);
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
