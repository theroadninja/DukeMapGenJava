package trn.prefab;

import java.util.List;

import trn.DukeConstants;
import trn.IdMap;
import trn.Map;
import trn.PointXY;
import trn.PointXYZ;
import trn.Sector;
import trn.Sprite;
import trn.Wall;

public class PrefabUtils {
	
	public static int MARKER = DukeConstants.TEXTURES.CONSTRUCTION_SPRITE;
	
	public static int SIMPLE_JOIN = 1;

	
	public void go(){
		
	}
	
	public static class Connector {
		public Sprite sprite;
		int sectorId;
		int wallId;
		
		//Sector sector;
		Wall wall;
		//PointXY p1;
		//PointXY p2;
		int x;
		int ymin;
		int ymax;
		int z;
		
		public void setPoints(PointXY p1, PointXY p2){
			if(p1.x != p2.x){
				throw new IllegalArgumentException();
			}
			this.x = p1.x;
			this.ymin = Math.min(p1.y, p2.y);
			this.ymax = Math.max(p1.y, p2.y);
			if(this.ymin == this.ymax){
				throw new IllegalArgumentException();
			}
			
		}
		
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("{ connector\n");
			sb.append("  x: ").append(x).append("\n");
			sb.append(" ymin: ").append(ymin).append("\n");
			sb.append(" ymax: ").append(ymax).append("\n");
			return sb.toString();
		}
		
		public PointXYZ getTransformTo(Connector c2){
			return new PointXYZ(
					c2.x - this.x, 
					c2.ymin - this.ymin, 
					c2.z - this.z);
					//c2.sector.getFloorZ() - this.sector.getFloorZ());
			//return new PointXYZ(-1024*5, c2.ymin - this.ymin, c2.sector.getFloorZ() - this.sector.getFloorZ());
		}
		
		public void translateIds(final IdMap idmap){
			//TODO:  also do sprite ...
			this.sectorId = idmap.sector(this.sectorId);
			this.wallId = idmap.wall(this.wallId);
			
		}
	}
	
	public static Connector findConnector(Map map, int connectorType, int wallLotag){
		//List<Sprite> sprites = new LinkedList<Sprite>();
		
		
		for(Sprite s: map.findSprites(MARKER, connectorType, null)){
			
			Sector sector = map.getSector(s.getSectorId());
			List<Integer> walls = map.getAllSectorWallIds(sector);
			for(int i: walls){
				Wall w = map.getWall(i); 
				if(wallLotag == w.getLotag()){
					Connector connector = new Connector();
					connector.sprite = s;
					connector.sectorId = s.getSectorId();
					//connector.sector = sector;
					connector.wall = w;
					connector.wallId = i;
					connector.z = sector.getFloorZ();
					
					//connector.p1 = new PointXY(w);
					//connector.p2 = new PointXY(map.getWall(w.getPoint2()));
					connector.setPoints(new PointXY(w), new PointXY(map.getWall(w.getPoint2())));
					
					return connector;
				}
			}
		}
		
		throw new RuntimeException("cant find connector");
	}
	
	
	public static void joinWalls(Map map, Connector c1, Connector c2){
		//Wall w1 = map.getWall(c1.wallId)
		//Wall w2 = map.getWall(c2.wallId);
		
		map.linkRedWalls(c1.sectorId, c1.wallId, c2.sectorId, c2.wallId);
		
		
	}
	
	
	


}
