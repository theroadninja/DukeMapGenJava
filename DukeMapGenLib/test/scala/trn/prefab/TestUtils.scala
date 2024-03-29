package trn.prefab

import java.io.{ByteArrayInputStream, File, FileInputStream}

import org.apache.commons.io.IOUtils
import trn.{Map => DMap}

/**
  * The java version is JavaTestUtils.java
  */
object TestUtils {
  val MapWriterMap: String = "JUNIT2.MAP"
  val ChildTest: String = "CHILDTST.MAP"
  val MultiSect: String = "UNITMULT.MAP"
  val Inner: String = "INNER.MAP" // pasting sector group inside a sector
  val Compass: String = "cmpstest.map"
  val SpriteIntersectionTest: String = "sprtint.map"

  val TEST_DATA_PATH = System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "testdata"


  def testDataPath(path: String, filename: String): String = s"${TEST_DATA_PATH}/$path/$filename"

  /**
    * @param filename relative path starting from inside testdata/
    */
  def loadTestMap(filename: String): DMap = {
    val path = TEST_DATA_PATH + File.separator + filename
    DMap.readMap(new ByteArrayInputStream(IOUtils.toByteArray(new FileInputStream(new File(path)))))
  }

  def load(filename: String): DMap = TestUtils.loadTestMap(s"scala/trn.prefab/${filename}")

  // load a map originally intended for the java code
  def loadJavaMap(filename: String): DMap = TestUtils.loadTestMap(s"${filename}")

  def loadPalette(filename: String): PrefabPalette = PrefabPalette.fromMap(TestUtils.load(filename), true)

}
