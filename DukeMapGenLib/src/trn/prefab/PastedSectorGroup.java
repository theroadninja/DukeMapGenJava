package trn.prefab;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import trn.Map;
import trn.MapUtil;


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
	
	final List<Connector> connectors = new ArrayList<Connector>();
	
	//should this be here?
	public final MapUtil.CopyState copystate;
	

	public PastedSectorGroup(Map map, MapUtil.CopyState copystate) {
		this.destMap = map;
		this.copystate = copystate;
		final Set<Short> destSectorIds = this.copystate.destSectorIds();
		
		ConnectorFilter cf = new ConnectorFilter(){
			@Override
			public boolean matches(Connector c) {
				return destSectorIds.contains((short)c.sectorId);
			}
		};
		
		this.connectors.addAll(Connector.findConnectors(map, cf));
	}

	public Connector getConnector(int connectorId){
		if(connectorId < 0) throw new IllegalArgumentException();
		
		for(Connector c: connectors){
			if(c.connectorId == connectorId){
				return c;
			}
		}
		
		throw new IllegalArgumentException();
	}
	

	/*
	public Iterable<Connector> findConnectors(ConnectorFilter filter){
		return Connector.findConnectors(destMap, new ConnectorFilter() {
			@Override
			public boolean matches(Connector c) {
				// TODO Auto-generated method stub
				return copystate.sourceSectorIds().contains(c.getSectorId());
			}
		}, filter);
		//return Connector.findConnectors(destMap, copystate.sourceSectorIds());
	}*/

}