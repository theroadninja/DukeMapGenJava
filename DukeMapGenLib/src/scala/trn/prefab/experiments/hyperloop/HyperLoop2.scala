package trn.prefab.experiments.hyperloop

import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.hyperloop.Loop2Plan.Blank
import trn.{HardcodedConfig, RandomX, ScalaMapLoader}
import trn.prefab.{MapWriter, GameConfig, DukeConfig}

case class Loop2Plan(inner: Seq[String], outer: Seq[String])

class Loop2Planner(random: RandomX){

  def randomPlan(size: Int): Loop2Plan = {
    // TODO implement randomness

    val inner = "_________E______"
    val outer = "_S_k____________"

    require(size == 16)
    require(inner.size == size)
    require(outer.size == size)
    Loop2Plan(
      inner.map(s => s.toString),
      outer.map(s => s.toString),
    )
  }

}

object Loop2Plan {

  val Blank = "_"

  /** outer section containing player start */
  val Start = "S"

  /** inner section, or set piece, containing end (always requires the second key to access) */
  val End = "E"

  // TODO ? val Blocked = "*" // for things like end having two connections

  /** first key */
  val Key1 = "k"

  /** second key */
  val Key2 = "K2"

  /** set piece that blocks the passage; force field or "blast doors" */
  val ForceField = "F"

  /** 80% shotgun, 10% chaingun, 10% pipe bombs */
  val LightWeapon = "L"

  // TODO tripmine and shrink ray to have lower probability
  val Weapon = "W" // "H" for "heavy weapon" ?

  /** currently includes ammo */
  val Powerup = "P"

  /** enemy section - section dedicated to enemies.  NOT used for enemies that spawn in the passage */
  val Enemy = "N"
}

object HyperLoop2 {
  val Filename = "loop2.map"

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    val random = new RandomX()

    val plan = new Loop2Planner(random).randomPlan(16)
    println(plan)

    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    renderPlan(gameCfg, plan, new Loop2Palette(gameCfg, random, palette))
  }

  def renderPlan(gameCfg: GameConfig, plan: Loop2Plan, loop2Palette: Loop2Palette): Unit = {
    val sectionCount = 16
    val ringLayout = RingLayout.fromHyperLoopPalette(loop2Palette)
    val writer = MapWriter(gameCfg)
    val ringPrinter = new RingPrinter1(writer, ringLayout, sectionCount)

    (0 until sectionCount).foreach { index =>
      val angleType = RingLayout.indexToAngleType(index)
      ringPrinter.tryPasteMid(index, loop2Palette.getMidBlank(angleType))
    }

    (0 until sectionCount).foreach { index =>
      val angleType = RingLayout.indexToAngleType(index)
      println(s"pasting blank inner index=${index} angleType=${angleType}")
      plan.inner(index) match {
        case Loop2Plan.Blank => ringPrinter.tryPasteInner(index, loop2Palette.getInnerBlank(angleType))
        case Loop2Plan.End => ringPrinter.pasteInner(index, loop2Palette.getEnd(angleType))
        case Loop2Plan.Start => throw new RuntimeException("cannot place START section on inner ring")
        case s: String => println(s"ERROR ${s}")
      }
      println(s"pasting blank outer index=${index} angleType=${angleType}")
      plan.outer(index) match {
        case Loop2Plan.Blank => ringPrinter.tryPasteOuter(index, loop2Palette.getOuterBlank(angleType))
        case Loop2Plan.Start => {
          val sg = loop2Palette.getPlayerStartOuter(angleType)
          ringPrinter.tryPasteOuter(index, sg)
        }
        case Loop2Plan.End => ??? // TODO ?
        case Loop2Plan.Key1 => ringPrinter.pasteOuter(index, loop2Palette.getItemOuter(angleType))
        case s: String => println(s"ERROR ${s}")
      }
    }

    ringPrinter.autoLink()
    ExpUtil.finishAndWrite(writer)
  }

}
