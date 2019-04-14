package trn.prefab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import trn.Map;
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
		RedwallConnector c1 = getRedwallConnector(connectorId);
		RedwallConnector c2 = sg.getRedwallConnector(connectorId);
		return super.connectedTo(c1, sg, c2);
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