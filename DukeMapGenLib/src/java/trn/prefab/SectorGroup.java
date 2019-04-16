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
	
	public Connector getConnector(int connectorId){
		if(connectorId < 0) throw new IllegalArgumentException();
		for(Connector c: connectors_()){
			if(c.getConnectorId() == connectorId){
				return c;
			}
		}
		throw new IllegalArgumentException();
	}

	public SectorGroup connectedTo(int connectorId, SectorGroup sg){
		if(sg == null) throw new IllegalArgumentException();
		RedwallConnector c1 = getRedwallConnector(connectorId);
		RedwallConnector c2 = sg.getRedwallConnector(connectorId);
		return super.connectedTo(c1, sg, c2);
	}

	/**
	 * For child nodes to return their parent id
	 * @return the sector group id of the parent sector group to this child, or null if this is not a child sector group
	 */
	public Sprite getChildPointer(){
		List<Sprite> childPointer = map().findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.REDWALL_CHILD, null);
		if(childPointer.size() < 1){
			return null;
		}else if(childPointer.size() == 1){
			return childPointer.get(0);
		}else{
			throw new SpriteLogicException("more than one child pointer marker in sector group");
		}
	}

	/**
	 * @return the Connector used to connect this child group to its parent group (or null if this is not a child group)
	 */
	RedwallConnector getChildPointerConnector(int sectorId){
		Iterable<Connector> conns = Connector.findConnectors(this.connectors_(), c -> c.getSectorId() == sectorId
				&& ConnectorType.isRedwallType(c.getConnectorType()));
		List<Connector> list = new LinkedList<>();
		for(Connector c: conns){ list.add(c); } // WTF java
		if(list.size() == 0){
			return null;
		}else if(list.size() == 1){
			return (RedwallConnector)list.get(0);
		}else{
			throw new SpriteLogicException("More than one connector for child pointer");
		}
	}

	/**
	 * Called by the palette loading code to attach sector group children to their parent.
	 */
	public SectorGroup connectedToChildren(List<SectorGroup> children){
		SectorGroup result = this.copy();
		if(children == null || children.size() < 1){
			return result;
		}
        if(this.getGroupId() == -1){
            throw new SpriteLogicException("cannot connect children to parent with no group id");
        }

        Set<Integer> seenConnectorIds = new HashSet<>(children.size());
        for(SectorGroup child : children){
            Sprite childPtr = child.getChildPointer();
            int parentId = (childPtr != null) ? (int)childPtr.getHiTag() : -1;
            // make sure child pointer matches this sector group's ID
            if(parentId == -1 || parentId != this.getGroupId()){
                throw new IllegalArgumentException("child pointer has wrong group id: " + parentId);
            }

            // make sure no children share connector Ids
            RedwallConnector conn = child.getChildPointerConnector(childPtr.getSectorId());
            int connId = conn.getConnectorId();
            if(connId == 0){
            	throw new SpriteLogicException("connector for child pointer has ID 0");
			}else if(seenConnectorIds.contains(connId)){
            	throw new SpriteLogicException("Too many child pointer connectors with ConnectorID " + connId);
			}else{
            	seenConnectorIds.add(connId);
			}

            // TODO - allow child sector groups to connect to other child sector groups with same parent (need to
			//     sort them first, and dedupe all connector IDs, to ensure deterministic behavior)
            if(null == this.findFirstConnector(c -> c.getConnectorId() == connId)){
            	throw new SpriteLogicException("parent sector group is missing connector " + connId);
			}

            result = result.connectedTo(connId, child);
        }

		return result;
	}

	public RedwallConnector getRedwallConnector(int connectorId){
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