package trn.prefab;

import java.util.*;

import duchy.sg.SectorGroupFragment;
import duchy.sg.SectorGroupScanner$;
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
	 *
	 * The KEY of the map is the parent id
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

		List<SectorGroupFragment> fragments = SectorGroupScanner$.MODULE$.scanFragmentsAsJava(map, cfg);

		// Set<Short> processedSectorIds = new TreeSet<Short>();
		// List<SectorGroupFragment> fragments = new LinkedList<>();
		// short sector = 0;
		// while(sector < map.getSectorCount()){
		// 	if(processedSectorIds.contains(sector)){
		// 		sector++;
		// 		continue;
		// 	}

		// 	SectorGroupFragment fragment = SectorGroupScanner$.MODULE$.scanFragment(map, cfg, sector);
		// 	processedSectorIds.addAll(fragment.copyState().sourceSectorIds());
		// 	fragments.add(fragment);

		// 	sector++;
		// } // while

		for(SectorGroupFragment fragment: fragments) {
			if(fragment.groupIdSprite().isDefined()){
				int groupId = fragment.getGroupId();
				if (numberedSectorGroups.containsKey(groupId)) {
					throw new SpriteLogicException("more than one sector group with id " + groupId);
				}
				numberedSectorGroups.put(groupId, fragment.sectorGroup());

			}else if(fragment.childPointerSprite().isDefined()){
				int groupId = fragment.childPointerSprite().get().getHiTag();
				fragment.checkChildPointer();
				redwallChildren.putIfAbsent(groupId, new LinkedList<>());
				redwallChildren.get(groupId).add(fragment.sectorGroup());
			}else if(fragment.teleChildSprite().isDefined()){
				int groupId = fragment.teleChildSprite().get().getHiTag();
				teleportChildren.putIfAbsent(groupId, new LinkedList<>());
				teleportChildren.get(groupId).add(fragment.sectorGroup());
			}else{
				anonymousSectorGroups.add(fragment.sectorGroup()); //new SectorGroup(clipboard));
			}

			if(strict){ // TODO - get rid of this strict thing
				fragment.requireAtMostOneAnchor();
			}
		}

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

		// now process the children
		TagGenerator tagGenerator = new SimpleTagGenerator(500); // TODO - should be passed in
		final java.util.Map<Integer, SectorGroup> numberedGroups2 = SectorGroupScanner$.MODULE$.connectRedwallChildrenJava(
				tagGenerator,
				cfg,
				numberedSectorGroups,
				redwallChildren
		);
		// final java.util.Map<Integer, SectorGroup> numberedGroups2 = new java.util.TreeMap<>();
		// for(Integer groupId : numberedSectorGroups.keySet()){
		// 	SectorGroup sg = numberedSectorGroups.get(groupId);
		// 	if(redwallChildren.containsKey(groupId)){
		// 	    numberedGroups2.put(groupId, sg.connectedToChildren2(redwallChildren.get(groupId), tagGenerator, cfg));
		// 	}else{
		// 		numberedGroups2.put(groupId, sg);
		// 	}
		// }
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
