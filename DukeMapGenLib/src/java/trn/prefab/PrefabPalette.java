package trn.prefab;

import java.util.*;

import duchy.sg.SectorGroupScanner$;
import duchy.sg.SgPaletteScala;
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

	/**
	 * This class is meant to be replaced by SgPaletteScala.  This is a hack to enable
	 * that transition.
	 */
	public final SgPaletteScala scalaObj;

	public PrefabPalette(
			java.util.Map<Integer, SectorGroup> numberedSectorGroups,
			java.util.Map<Integer, List<SectorGroup>> teleportChildGroups,
			List<SectorGroup> anonymousSectorGroups,
			SgPaletteScala scala
	){
		this.numberedSectorGroups = numberedSectorGroups;
		this.teleportChildGroups = teleportChildGroups;
		this.anonymousSectorGroups = anonymousSectorGroups;
		this.scalaObj = scala;
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

		TagGenerator tagGenerator = new SimpleTagGenerator(500); // TODO - should be passed in
		SgPaletteScala scala = SectorGroupScanner$.MODULE$.scanMap(cfg, tagGenerator, map);
		return new PrefabPalette(
				scala.getNumberedGroupsAsJava(),
				scala.getTeleportChildrenAsJava(),
				scala.getAnonymousAsJava(),
				scala
		);

	}

	public SectorGroup getSectorGroup(int sectorGroupId){
	    if(!this.numberedSectorGroups.containsKey(sectorGroupId)) throw new NoSuchElementException(String.format("sg id=%s", sectorGroupId));
		return this.numberedSectorGroups.get(sectorGroupId);
	}

	public SectorGroup getSG(int sectorGroupId){
		return this.getSectorGroup(sectorGroupId);
	}

	public boolean hasSG(int sectorGroupId){
		return this.numberedSectorGroups.containsKey(sectorGroupId);
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
		if(this.teleportChildGroups.containsKey(parentSectorGroupId)){
			return this.teleportChildGroups.get(parentSectorGroupId);
		}else{
			return Collections.emptyList();
		}
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
