package trn.prefab

import java.io.{ByteArrayInputStream, File, FileInputStream}

import org.apache.commons.io.IOUtils
import trn.{Map => DMap};
object TestUtils {

  val TEST_DATA_PATH = System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "testdata"

  /**
    * @param filename relative path starting from inside testdata/
    */
  def loadTestMap(filename: String): DMap = {
    val path = TEST_DATA_PATH + File.separator + filename
    DMap.readMap(new ByteArrayInputStream(IOUtils.toByteArray(new FileInputStream(new File(path)))))
  }

}
