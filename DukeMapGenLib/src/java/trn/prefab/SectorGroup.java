package trn.prefab;

import java.util.*;

import trn.Map;
import trn.Sprite;
import trn.duke.MapErrorException;
import trn.duke.TextureList;

public class SectorGroup extends SectorGroupS
	implements ISectorGroup
{

	/** optional id that can be added to group by user to manually identify it
	 * -1 means no id
	 */
	final int sectorGroupId;
	


	// TODO - make this not public (need iterator for scala code?)
	//public List<Connector> connectors = new ArrayList<Connector>();
	
	public SectorGroup(Map map, int sectorGroupId) throws MapErrorException {
	    super(map, sectorGroupId);
		this.sectorGroupId = sectorGroupId;
		//this.map = map;
		//this.connectors.addAll(SimpleConnector.findConnectors(map));
        try{
			for(Connector c : Connector.findConnectors(map)){
				addConnector(c);
			}
		}catch(SpriteLogicException ex){
        	throw new SpriteLogicException("exception while scanning connectors in sector group.  id=" + sectorGroupId, ex);
		}
	}

	public SectorGroup(Map map) throws MapErrorException {
		this(map, -1);
	}

	private List<Connector> connectors_(){
		return super.connectors();
	}

	@Override
	public void updateConnectors() throws MapErrorException {
	    super.connectors().clear();
		for(Connector c : Connector.findConnectors(super.map())){
			addConnector(c);
		}
	}

	@Override
	public Map getMap(){
		return super.map();
	}

	public int getGroupId(){
		return this.sectorGroupId;
	}

	/**
	 *
	 * @returns a read only list of all connecters that need the whole sector group to be in a certain place.
	 */
	public List<SimpleConnector> connectorsWithXYRequrements(){
		// TODO - shouldnt be casting
		List<SimpleConnector> list = new ArrayList(this.connectors_().size());
		for(Connector c : connectors_()){
			if(c.hasXYRequirements()){
				list.add((SimpleConnector)c);
			}
		}
		return Collections.unmodifiableList(list);
	}

	private void addConnector(Connector c){
		Map map = super.map();
	    if(c.getSectorId() < 0){
	        throw new RuntimeException("connector has invalid sectorId: " + c.getSectorId());
        }
        if(c.getSectorId() >= map.getSectorCount()){
	        throw new RuntimeException("connector has sectorId " + c.getSectorId() + " but there are only " + map.getSectorCount() + " sectors in group");
        }
	    this.connectors_().add(c);
    }
	
	public SectorGroup connectedTo(int connectorId, SectorGroup sg){
		if(sg == null) throw new IllegalArgumentException();
		RedwallConnector c1 = getRedwallConnector(connectorId);
		RedwallConnector c2 = sg.getRedwallConnector(connectorId);
		return super.connectedTo(c1, sg, c2);
	}

	/**
	 * Called by the palette loading code to attach sector group children to their parent.
	 * THIS object is the parent.
     *
	 * 1. the child sector's marker must match THIS parent's sector group ID.
     * 2. more than one child cannot use the same connector id
	 * 3. the connectorId the child uses to connect must match the parent's in quantity
	 * 		(e.g. if a child has two redwall connectors with ID 123, the parent must have exactly two)
	 */
	public SectorGroup connectedToChildren(List<SectorGroup> children, TagGenerator tagGenerator){
		SectorGroup result = this.copy();
		if(children == null || children.size() < 1){
			return result;
		}
        if(this.getGroupId() == -1){
            throw new SpriteLogicException("cannot connect children to parent with no group id");
        }

        Set<Integer> seenConnectorIds = new HashSet<>(children.size());
        for(SectorGroup child : children){
			// 1. make sure child pointer matches this sector group's ID
            trn.prefab.ChildPointer childPtr = child.getChildPointer();
            if(this.getGroupId() != childPtr.childMarker().getHiTag()){
				throw new IllegalArgumentException("child pointer has wrong group id");
			}

            // 2. make sure no children share connector Ids
            if(seenConnectorIds.contains(childPtr.connectorId())){
            	throw new SpriteLogicException("more than one child sector group is trying to use connector " + childPtr.connectorId());
			}else{
            	seenConnectorIds.add(childPtr.connectorId());
            	seenConnectorIds.addAll(child.allConnectorIds());
			}

            // 3. parent and child must have the same number of connectors
            if(this.getRedwallConnectorsById(childPtr.connectorId()).size() != childPtr.connectorsJava().size()){
            	throw new SpriteLogicException("parent and child sector groups have different count of connectors with ID " + childPtr.connectorId());
			}

			// TODO - allow child sector groups to connect to other child sector groups with same parent (need to
			//     sort them first, and dedupe all connector IDs, to ensure deterministic behavior)

            if(childPtr.connectorsJava().size() != 1){
            	throw new RuntimeException("more than one child connector not implemented yet");
			}else{
            	//result = result.connectedTo(childPtr.connectorId(), child);
				result = result.connectedToChild(childPtr, child, tagGenerator);

				// TODO - sort children by connector id first! (so that at least results are deterministic)

            	// TODO - connect elevators and water

				// TODO - a good unit test (integration test?) with everything
				//		create a map file called "SectorGroupTest1.map" ?
			}
        }
		return result;
	}

	public RedwallConnector getRedwallConnector(int connectorId){ // TODO - move to scala
		return (RedwallConnector)getConnector(connectorId);
	}

	public boolean hasConnector(int connectorId){
		if(connectorId < 0) throw new IllegalArgumentException();
		for(Connector c: connectors_()){
			if(c.getConnectorId() == connectorId){
				return true;
			}
		}
		return false;
	}

	/** right now this is for debugging */
	public int getSectorCount(){
	    return super.map().getSectorCount();
    }


	public Connector findFirstConnector(ConnectorFilter cf){
		Iterator<Connector> it = Connector.findConnectors(this.connectors_(), cf).iterator();
		return it.hasNext() ? it.next() : null;
	}



}