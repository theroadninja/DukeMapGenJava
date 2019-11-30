package trn.prefab;

import java.util.*;

import trn.ISpriteFilter;
import trn.Map;
import trn.MapUtil;
import trn.Sprite;
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
public class PastedSectorGroup extends PastedSectorGroupS
{

	public PastedSectorGroup(Map map, MapUtil.CopyState copystate) throws MapErrorException {
		super(map, copystate.destSectorIds(), copystate, SimpleConnector.findConnectorsInPsg(map, copystate));
	}

}
