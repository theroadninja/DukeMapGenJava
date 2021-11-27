package trn.prefab.experiments
import trn.prefab.PrefabPalette
import trn.prefab.experiments.Hypercube4.{Filename, run}
import trn.{Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}

trait PrefabExperiment {

  def Filename: String

  def run(mapLoader: MapLoader): DMap
}

trait PrefabExperimentStdRun extends PrefabExperiment {

  def run(palette: PrefabPalette): DMap

  def run(mapLoader: MapLoader): DMap = {
    // TODO - dry with Hypercube4 runtime
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true)
    val started = System.currentTimeMillis();
    val result = run(palette)
    val ended = System.currentTimeMillis();
    println(s"Sector count: ${result.getSectorCount}")
    println(s"Map compile time: ${ended - started} ms")
    result
  }
}
