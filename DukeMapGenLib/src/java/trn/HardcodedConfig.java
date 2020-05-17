package trn;

import java.io.File;

/** Trying to at least put all my lazy hardcoded stuff in one place */
public final class HardcodedConfig {

    public static final String DOSPATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/";
    public static final String PATH_WITH_ART = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3da/dave/";

    public static final String getAtomicWidthsFile(){
        return System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "data" + File.separator + "atomictexwidths.txt";
    }

    // TODO - there is also a path in MapLoader

    // TODO - also in Testutils.scala (and probably the java one) - util class for loading unit test files

}
