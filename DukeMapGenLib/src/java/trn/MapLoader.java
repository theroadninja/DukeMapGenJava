package trn;

import trn.prefab.PrefabPalette;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// TODO - DRY with HardcodedConfig
// See also ArtFileReader
public class MapLoader {

    private final String path;

    public MapLoader(String path){
        this.path = path;
    }

    public Map load(String filename) throws IOException {
        return loadMap(this.path + filename);
    }

    // TODO - better name for this
    public static Map loadLocalMap(String filename) throws IOException {
        // TODO - try to load from project first
        return loadMap(Main.DOSPATH + filename);
    }

    public static PrefabPalette loadPalette(String filename) throws IOException {
        return PrefabPalette.fromMap(loadMap(filename), true);
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
        return map; // TODO - am I seriously not closing the stream??
    }
}
