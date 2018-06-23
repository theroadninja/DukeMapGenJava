package trn.prefab;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import trn.Map;

public class SectorGroup {
	
	/** map object used to store all the sectors, walls and sprites */
	final Map map;
	
	List<Connector> connectors = new ArrayList<Connector>(); 
	
	public SectorGroup(Map map){
		this.map = map;
		this.connectors.addAll(Connector.findConnectors(map));
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
	
	public Connector findFirstConnector(ConnectorFilter cf){
		Iterator<Connector> it = Connector.findConnectors(this.connectors, cf).iterator();
		return it.hasNext() ? it.next() : null;
	}
}