package trn.bespoke

import trn.{BuildConstants, HardcodedConfig, Main, MapLoader, PointXY, PointXYZ}
import trn.prefab.{GameConfig, MapWriter, PasteOptions, SimpleSectorGroupPacker}

object DarkSide {
  // def getSpaceMap: String = HardcodedConfig.getMapDataPath("SPACE.MAP")
  def getDarkSideMap: String = HardcodedConfig.getDosboxPath("DARK1.MAP")

  def run(gameCfg: GameConfig): Unit = {
    val packer = SimpleSectorGroupPacker(

      // bottom left, below the train line

      new PointXY(BuildConstants.MIN_X, 9216),
      new PointXY(-6144, BuildConstants.MAX_Y)
    )
    val writer = MapWriter(gameCfg, Some(packer))
    try{
      run(gameCfg, writer)

      Main.deployTest(writer.outMap)
    }catch{
      case ex: Throwable => {
        ex.printStackTrace()
        println("map generation failed")
        writer.setAnyPlayerStart(true)
        Main.deployTest(writer.outMap, "error.map")
      }

    }
  }
  def run(gameCfg: GameConfig, writer: MapWriter): Unit = {
    val palette = MapLoader.loadPalette(getDarkSideMap)

    val Start = 1
    //val start = palette.getSG(1)
    val Center = 2

    val MedicalArea = 3
    val AirLock = 4
    val Outside = 5

    val psgCenter = writer.pasteSectorGroup(palette.getSG(Center), PointXYZ.ZERO)

    writer.tryPasteConnectedTo(psgCenter, palette.getSG(Start), PasteOptions())
    writer.tryPasteConnectedTo(psgCenter, palette.getCompoundSG(MedicalArea), PasteOptions())
    val airlockPsg = writer.tryPasteConnectedTo(psgCenter, palette.getSG(AirLock), PasteOptions()).get

    writer.tryPasteConnectedTo(airlockPsg, palette.getCompoundGroup(Outside), PasteOptions())

    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
  }

}
