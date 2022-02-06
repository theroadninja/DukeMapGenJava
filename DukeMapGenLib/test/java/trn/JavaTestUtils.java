package trn;

import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// TODO NOTE there is a scala version of this in TestUtils.scala
public class JavaTestUtils {

    // making it easy to find all the tests that use a certain map.
    public static final String JUNIT1 = "JUNIT1.MAP";
    public static final String JUNIT2 = "junit2.map";

    /**
     * Used to test Map.getSectorWallIndexes().
     *
     * Doesn't have sector groups.  Just some loose sectors.  The main one contains sprite 685 (CAMERALIGHT).
     */
    public static final String JUNIT3 = "junit3.map";

    /**
     * Test Map for Wall Deletion
     */
    public static final String JUNIT4 = "junit4.map";

    public static final String MULTI_WALL_CONN_MAP = "UNITMW.MAP";
    public static final String ADD_LOOP = "addloop.map";
    public static final String JOIN = "join.map";
    public static final String PREFAB_TEST = "PRETEST.MAP";

    private static String testFilePath(String fname){
        String filepath = System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "testdata" + File.separator + fname;
        return filepath;
    }

    static Map readMap(String filename) throws IOException {
        File f = new File(filename);
        Assert.assertTrue(f.exists() && f.isFile());
        return Map.readMap(new FileInputStream(f));
    }

    public static Map readTestMap(String filename) throws IOException {
        return readMap(testFilePath(filename));
    }

    public static void writeMap(Map map) throws IOException {
        Main.deployTest(map, "output_unit.map", HardcodedConfig.getDeployPath("output_unit.map"));

    }
}
