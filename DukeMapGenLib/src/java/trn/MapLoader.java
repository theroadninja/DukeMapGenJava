package trn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MapLoader {

    // TODO - better name for this
    public static Map loadLocalMap(String filename) throws IOException {
        // TODO - try to load from project first
        return loadMap(Main.DOSPATH + filename);
    }

    public static Map loadMap(String filename) throws IOException {
        File path = new File(filename);
        if(path.isAbsolute()){
            // C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/
            return loadMap(path);
        }else{
            return loadMap(new File(System.getProperty("user.dir") + File.separator + "testdata" + File.separator, filename));
        }
    }

    public static Map loadMap(File mapfile) throws IOException {
        FileInputStream bs = new FileInputStream(mapfile);
        Map map = Map.readMap(bs);
        return map;
    }
}
