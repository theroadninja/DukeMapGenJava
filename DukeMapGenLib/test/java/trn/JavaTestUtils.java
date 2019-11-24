package trn;

import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JavaTestUtils {

    private static String testFilePath(String fname){
        String filepath = System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "testdata" + File.separator + fname;
        return filepath;
    }

    private static Map readMap(String filename) throws IOException {
        File f = new File(filename);
        Assert.assertTrue(f.exists() && f.isFile());
        return Map.readMap(new FileInputStream(f));
    }

    public static Map readTestMap(String filename) throws IOException {
        return readMap(testFilePath(filename));
    }
}
