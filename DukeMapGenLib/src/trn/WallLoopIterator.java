package trn;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;




/**
 * Iterates over the wall loops of a sector.
 * 
 * Returns each wall loop (not each wall).
 * 
 * Most sectors only have one wall loop, but if they contain sectors
 * they can have more than one.
 * 
 * @author Dave
 *
 */
public class WallLoopIterator implements Iterator<Collection<Integer>> {
	
	private Map map;
	private int sectorId;
	
	
	int nextLoopStart = -1;
	
	
	WallLoopIterator(Map map, int sectorId){
		this.map = map;
		this.sectorId = sectorId;
	}

	@Override
	public boolean hasNext() {
		final Sector sector = this.map.getSector(this.sectorId);
		if(sector.getWallCount() == 0){
			throw new IllegalStateException();
		}
		return nextLoopStart == -1 || nextLoopStart < sector.getFirstWall() + sector.getWallCount();


	}

	@Override
	public Collection<Integer> next() {
		if(! this.hasNext()){
			throw new NoSuchElementException();
		}
		
		final Sector sector = this.map.getSector(this.sectorId);
		if(nextLoopStart == -1){
			nextLoopStart = sector.getFirstWall();
		}
		
		List<Integer> results = this.map.getWallLoop(nextLoopStart);
		if(results.size() < 1){
			throw new IllegalStateException();
		}
		nextLoopStart += results.size();
		return results;
	}

}
