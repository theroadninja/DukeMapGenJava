package trn

import trn.prefab.{DukeConfig, GameConfig, PrefabPalette}

import java.io.{File, FileInputStream}

/**
  * Scala version of MapLoader.java
  */
object ScalaMapLoader {

  def toAbsPath(filename: String): String = {
    val path = new File(filename)
    if(path.isAbsolute){
      filename
    }else{
      System.getProperty("user.dir") + "/testdata/" + filename
    }
  }

  def loadMap(filename: String): Map = {
    val path = new File(toAbsPath(filename))
    val bs = new FileInputStream(path)
    val map = Map.readMap(bs)
    bs.close()
    map
  }

  def loadPalette(filename: String, gameCfg: Option[GameConfig] = None): PrefabPalette = {
    val cfg = gameCfg.getOrElse(DukeConfig.load(HardcodedConfig.getAtomicWidthsFile))
    PrefabPalette.fromMap(cfg, loadMap(filename), true)
  }
}
