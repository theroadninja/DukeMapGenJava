package trn;

import java.io.File;

/** Trying to at least put all my lazy hardcoded stuff in one place */
public final class HardcodedConfig {

    public static final String DOSPATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/";
    public static final String EDUKE32PATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/EDuke32/";

    public static final String PATH_WITH_ART = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3da/dave/";

    public static final String TILES_OUTPUT_PATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/duketmp/tiles/";

    public static final String getAtomicWidthsFile(){
        return System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "data" + File.separator + "atomictexwidths.txt";
    }

    /**
     * @return the full path to a file in the MapData/ folder in this project.
     */
    public static final String getMapDataPath(String filename){
        return System.getProperty("user.dir") + "/DukeMapGenLib/mapdata/" + filename;
    }

    /**
     * @return the full path to a file in my Duke3d folder in dosbox (not part of this project)
     */
    public static final String getDosboxPath(String filename){
        return DOSPATH + filename;
    }

    public static final String getEduke32Path(String filename){
        return EDUKE32PATH + filename;
    }

    public static final String getDeployPath(String filename){
        // String copyDest = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/" + filename;
        String copyDest = EDUKE32PATH + filename;
        return copyDest;
    }

    // TODO - there is also a path in MapLoader

    // TODO - also in Testutils.scala (and probably the java one) - util class for loading unit test files

}
