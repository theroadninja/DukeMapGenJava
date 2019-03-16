package trn.prefab;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import trn.Map;
import trn.duke.MapErrorException;
import trn.duke.TextureList;

public class SectorGroup extends SectorGroupS {

	/** optional id that can be added to group by user to manually identify it
	 * -1 means no id
	 */
	final int sectorGroupId;
	
	/** map object used to store all the sectors, walls and sprites */
	//final Map map;


	// TODO - make this not public (need iterator for scala code?)
	public List<Connector> connectors = new ArrayList<Connector>();
	
	public SectorGroup(Map map, int sectorGroupId) throws MapErrorException {
	    super(map);
		this.sectorGroupId = sectorGroupId;
		//this.map = map;
		//this.connectors.addAll(SimpleConnector.findConnectors(map));
		for(Connector c : Connector.findConnectors(map)){
		    addConnector(c);
        }
	}

	public SectorGroup(Map map) throws MapErrorException {
		this(map, -1);
	}

	public int getGroupId(){
		return this.sectorGroupId;
	}

	public int bbHeight(){


	    Map map = super.map();
		if(map.getWallCount() < 1){
			return 0;
		}
		int minY = map.getWall(0).getY();
		int maxY = minY;
		for(int i = 1; i < map.getWallCount(); ++i){
			int y = map.getWall(i).getY();
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		return maxY - minY;
	}

	private void addConnector(Connector c){
		Map map = super.map();
	    if(c.getSectorId() < 0){
	        throw new RuntimeException("connector has invalid sectorId: " + c.getSectorId());
        }
        if(c.getSectorId() >= map.getSectorCount()){
	        throw new RuntimeException("connector has sectorId " + c.getSectorId() + " but there are only " + map.getSectorCount() + " sectors in group");
        }
	    this.connectors.add(c);
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

	/** right now this is for debugging */
	public int getSectorCount(){
	    return super.map().getSectorCount();
    }


	public Connector findFirstConnector(ConnectorFilter cf){
		Iterator<Connector> it = Connector.findConnectors(this.connectors, cf).iterator();
		return it.hasNext() ? it.next() : null;
	}

    // **
    //  * @param otherConnector
    //  * @returns the first connect in this group that could match with the given connector
    //  */
	// public Connector findFirstMate(Connector otherConnector){
    //     if(1==1) throw new RuntimeException("TODO - this doesnt work");
	//     for(Connector c : connectors){
	//         if(c.canMate(otherConnector)){
	//             return c;
    //         }
    //     }
    //     return null;
    // }

	// TODO - add:
    // isPlayerStart()
    // isEnd()
    // ...and so on

    public boolean isEndGame(){
	    for(int i = 0; i < map().getSpriteCount(); ++i){
	        if(map().getSprite(i).getTexture() == TextureList.Switches.NUKE_BUTTON){
	            return true;
            }
        }
        return false;
    }

}