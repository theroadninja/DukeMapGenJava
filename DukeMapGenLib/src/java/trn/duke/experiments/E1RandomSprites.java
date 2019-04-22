package trn.duke.experiments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import trn.Main;
import trn.Map;
import trn.Sprite;

/**
 * Loads RT1.MAP and replaces sprites based on their lotag.
 * 
 * @author Dave
 *
 */
public class E1RandomSprites {
	
	public static File intputFile(){
		return new File( System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "testdata" + File.separator + "RT1.MAP" );
	}
	
	//i bet this isn't thread safe
	private static Random RANDOM = new Random();
	
	public static final short UGLY_BRICK_TEXTURE = 0;

	static short[] weapons = new short[]{ 22, 23, 24, 25, 26, 27, 28, 29 };
	
	static short[] enemies = new short[]{ 1680, 1820, 1880, 1920, 1960, 2000, 2120, 2360 };
	
	
	static short random(short[] shorties){
		return shorties[ Math.abs(RANDOM.nextInt() % shorties.length) ];
	}

	public static void main(String[] args) throws IOException {
		FileInputStream bs = new FileInputStream(E1RandomSprites.intputFile());
		Map map = Map.readMap(bs);
		E1RandomSprites.randomSprites(map);
		Main.deployTest(map);
		// String resultsFile = System.getProperty("user.dir") + File.separator + "dukeoutput" + File.separator + "output.map";
		// FileOutputStream output = new FileOutputStream(new File(resultsFile));
		// map.toBytes(output);
		// output.close();
	}

	public static void randomSprites(Map map){
		//replace lotag 1 with random weapon, and lotag 2 with random enemy
		Random r = new Random();
		for(int i = 0; i < map.spriteCount(); ++i){
			Sprite sp = map.getSprite(i);
			if(UGLY_BRICK_TEXTURE == sp.getTexture()
					&& 1 == sp.getLotag()){
				sp.setTexture(random(weapons));
				sp.setLotag(0); //apparently they dont work with a lotag set
			}
			if(UGLY_BRICK_TEXTURE == sp.getTexture()
					&& 2 == sp.getLotag()){
				
				sp.setTexture(random(enemies));
				sp.setLotag(0);
			}
		} //for
	}
}
