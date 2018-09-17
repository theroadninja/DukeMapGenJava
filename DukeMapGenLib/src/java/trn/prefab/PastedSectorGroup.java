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
public class PastedSectorGroup {
	
	/** map the sectors were pasted to */
	public final Map destMap;
	
	public final List<Connector> connectors = new ArrayList<Connector>();
	
	//should this be here?
	public final MapUtil.CopyState copystate;
	

	public PastedSectorGroup(Map map, MapUtil.CopyState copystate) throws MapErrorException {
		this.destMap = map;
		this.copystate = copystate;
		final Set<Short> destSectorIds = this.copystate.destSectorIds();
		
		ConnectorFilter cf = new ConnectorFilter(){
			@Override
			public boolean matches(Connector c) {
				return destSectorIds.contains(c.getSectorId());
			}
		};
		
		this.connectors.addAll(SimpleConnector.findConnectors(map, cf));
	}


	public boolean isConnectorLinked(Connector c){ // TODO - replace with version in MapBuilder
		return c.isLinked(destMap);
		//// TODO - will not work with teleporer connectors, etc
		//return this.destMap.getWall(c.wallId).isRedWall();
	}

	public Connector getConnector(int connectorId){
		if(connectorId < 0) throw new IllegalArgumentException();
		
		for(Connector c: connectors){
			if(c.getConnectorId() == connectorId){
				return c;
			}
		}
		
		throw new IllegalArgumentException();
	}
	
	public Connector findFirstConnector(ConnectorFilter cf){
		Iterator<Connector> it = Connector.findConnectors(this.connectors, cf).iterator();
		return it.hasNext() ? it.next() : null;
	}

	public List<Connector> unlinkedConnectors(){
		LinkedList<Connector> list = new LinkedList<Connector>();
		for(Connector c : this.connectors){
			if(! isConnectorLinked(c)){
				list.add(c);
			}
		}
		return list;
	}
	

	/*
	public Iterable<SimpleConnector> findConnectors(ConnectorFilter filter){
		return SimpleConnector.findConnectors(destMap, new ConnectorFilter() {
			@Override
			public boolean matches(SimpleConnector c) {
				// TODO Auto-generated method stub
				return copystate.sourceSectorIds().contains(c.getSectorId());
			}
		}, filter);
		//return SimpleConnector.findConnectors(destMap, copystate.sourceSectorIds());
	}*/

}
