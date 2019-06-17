package trn.prefab;

import java.util.*;

import trn.Map;
import trn.MapUtil;
import trn.duke.MapErrorException;


/**
 * Tracks info about a sector group that was pasted onto a map.
 * 
 * So you can find connectors, etc.
 * 
 * NOTE:  this is different than a normal sector group, because this one
 * only owns a subset of the sector ids in the map
 * 
 * @author Dave
 *
 */
public class PastedSectorGroup extends PastedSectorGroupS implements ISectorGroup {
	
	/** map the sectors were pasted to */
	public final Map destMap;
	
	//public final List<Connector> connectors = new ArrayList<Connector>();
	private List<Connector> connectors_(){
		return super.connectors();
	}

	public final MapUtil.CopyState copystate;

	public PastedSectorGroup(Map map, MapUtil.CopyState copystate) throws MapErrorException {
	    super(map, copystate.destSectorIds());
		this.destMap = map;
		this.copystate = copystate;
		final Set<Short> destSectorIds = this.copystate.destSectorIds();
		
		ConnectorFilter cf = new ConnectorFilter(){
			@Override
			public boolean matches(Connector c) {
				return destSectorIds.contains(c.getSectorId());
			}
		};
		
		this.connectors_().addAll(SimpleConnector.findConnectors(map, cf));
	}

	@Override
	public Map getMap(){
		return destMap;
	}


	// public boolean isConnectorLinked(Connector c){ // TODO - replace with version in MapBuilderOld
	// 	return c.isLinked(destMap);
	// 	//// TODO - will not work with teleporer connectors, etc
	// 	//return this.destMap.getWall(c.wallId).isRedWall();
	// }

	// public Connector getConnector(int connectorId){
	// 	if(connectorId < 0) throw new IllegalArgumentException();
	//
	// 	for(Connector c: connectors_()){
	// 		if(c.getConnectorId() == connectorId){
	// 			return c;
	// 		}
	// 	}
	//
	// 	throw new IllegalArgumentException();
	// }

	// TODO - these functions are duplicated in SectorGroup
	public boolean hasConnector(int connectorId){
		if(connectorId < 0) throw new IllegalArgumentException();
		for(Connector c: connectors_()){
			if(c.getConnectorId() == connectorId){
				return true;
			}
		}
		return false;
	}


	public ElevatorConnector getFirstElevatorConnector(){
		for(Connector c: connectors_()){
			if(c.getConnectorType() == ConnectorType.ELEVATOR){
				return (ElevatorConnector)c;
			}
		}
		throw new NoSuchElementException();
	}

	public List<Connector> findConnectorsByType(int connectorType){
		List<Connector> results = new ArrayList<>(connectors_().size());
		for(Connector c : connectors_()){
			if(c.getConnectorType() == connectorType){
				results.add(c);
			}
		}
		return results;
	}
	
	public Connector findFirstConnector(ConnectorFilter cf){
		Iterator<Connector> it = Connector.findConnectors(this.connectors_(), cf).iterator();
		return it.hasNext() ? it.next() : null;
	}

	// public List<Connector> unlinkedConnectors(){
	// 	LinkedList<Connector> list = new LinkedList<Connector>();
	// 	for(Connector c : this.connectors_()){
	// 		if(! isConnectorLinked(c)){
	// 			list.add(c);
	// 		}
	// 	}
	// 	return list;
	// }

	public List<RedwallConnector> getRedwallConnectorsById(int connectorId){
		List<RedwallConnector> results = new ArrayList<>();
		for(Connector c: this.connectors_()){
			if(c.getConnectorId() == connectorId){
				if(ConnectorType.isRedwallType(c.getConnectorType())){
					results.add((RedwallConnector)c);
				}else{
					throw new IllegalArgumentException("connector with id " + connectorId + " is not a redwall connector");
				}
			}
		}
		return results;
	}



}
