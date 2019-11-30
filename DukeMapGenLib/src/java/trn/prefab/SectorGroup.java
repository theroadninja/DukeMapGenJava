package trn.prefab;

import trn.Map;
import trn.duke.MapErrorException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SectorGroup extends SectorGroupS
{
	SectorGroup(Map map, int sectorGroupId, SectorGroupProperties props, List<Connector> connectors) throws MapErrorException {
	    super(map, sectorGroupId, props, connectors);
	}

}