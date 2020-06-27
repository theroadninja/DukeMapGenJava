package trn;

import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JavaTestUtils {

    // making it easy to find all the tests that use a certain map.
    public static final String JUNIT1 = "JUNIT1.MAP";

    public static final String MULTI_WALL_CONN_MAP = "UNITMW.MAP";
    public static final String ADD_LOOP = "ADDLOOP.MAP";
    public static final String JOIN = "JOIN.MAP";
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
}
