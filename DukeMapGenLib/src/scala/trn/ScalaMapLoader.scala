package trn

import trn.prefab.{DukeConfig, GameConfig, PrefabPalette}

import java.io.{File, FileInputStream}


class ScalaMapLoader(val path: String) {

  def load(filename: String): Map = ScalaMapLoader.loadMap(path + filename)

}

/**
  * Scala version of MapLoader.java
  */
object ScalaMapLoader {

  def apply(path: String): ScalaMapLoader = new ScalaMapLoader(path)

  def toAbsPath(filename: String): String = {
    val path = new File(filename)
    if(path.isAbsolute){
      filename
    }else{
      System.getProperty("user.dir") + "/testdata/" + filename
    }
  }

  def loadMap(filename: String): Map = {
    Map.readMapFile(toAbsPath(filename))
  }

  def loadPalette(filename: String, gameCfg: Option[GameConfig] = None): PrefabPalette = {
    val cfg = gameCfg.getOrElse(DukeConfig.load(HardcodedConfig.getAtomicWidthsFile))
    PrefabPalette.fromMap(cfg, loadMap(filename), true)
  }
}
