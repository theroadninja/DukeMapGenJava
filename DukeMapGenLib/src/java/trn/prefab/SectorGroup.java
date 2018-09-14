package trn.prefab;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import trn.Map;
import trn.duke.TextureList;

public class SectorGroup {
	
	/** map object used to store all the sectors, walls and sprites */
	final Map map;
	
	List<Connector> connectors = new ArrayList<Connector>(); 
	
	public SectorGroup(Map map){
		this.map = map;
		//this.connectors.addAll(Connector.findConnectors(map));
		for(Connector c : Connector.findConnectors(map)){
		    addConnector(c);
        }
	}

	private void addConnector(Connector c){
	    if(c.sectorId < 0){
	        throw new RuntimeException("connector has invalid sectorId: " + c.sectorId);
        }
        if(c.sectorId >= map.getSectorCount()){
	        throw new RuntimeException("connector has sectorId " + c.sectorId + " but there are only " + map.getSectorCount() + " sectors in group");
        }
	    this.connectors.add(c);
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

	/** right now this is for debugging */
	public int getSectorCount(){
	    return map.getSectorCount();
    }


	public Connector findFirstConnector(ConnectorFilter cf){
		Iterator<Connector> it = Connector.findConnectors(this.connectors, cf).iterator();
		return it.hasNext() ? it.next() : null;
	}

    /**
     * @param otherConnector
     * @returns the first connect in this group that could match with the given connector
     */
	public Connector findFirstMate(Connector otherConnector){
        if(1==1) throw new RuntimeException("TODO - this doesnt work");
	    for(Connector c : connectors){
	        if(c.canMate(otherConnector)){
	            return c;
            }
        }
        return null;
    }

	// TODO - add:
    // isPlayerStart()
    // isEnd()
    // ...and so on

    public boolean isEndGame(){
	    for(int i = 0; i < map.getSpriteCount(); ++i){
	        if(map.getSprite(i).getTexture() == TextureList.Switches.NUKE_BUTTON){
	            return true;
            }
        }
        return false;
    }
}