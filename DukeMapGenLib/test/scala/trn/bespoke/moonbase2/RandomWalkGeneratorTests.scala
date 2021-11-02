package trn.bespoke.moonbase2

import org.junit.{Assert, Test}
import trn.logic.Point3d
import trn.prefab.Heading

class RandomWalkGeneratorTests {


  @Test
  def testLogicalRoom(): Unit = {
    val start = LogicalRoom("S")
    Assert.assertEquals("START", start.tag.get)
    Assert.assertEquals(0, start.zone)
    Assert.assertEquals(None, start.keyindex)
    Assert.assertEquals(LogicalRoom("S", 0, Some("START"), None, None), start)

    val end = LogicalRoom("E")
    Assert.assertEquals(LogicalRoom("E", 3, Some("END"), None, None), end)

    val r0 = LogicalRoom("0")
    Assert.assertEquals(None, r0.tag)
    Assert.assertEquals(0, start.zone)
    Assert.assertEquals(None, start.keyindex)
    Assert.assertEquals(LogicalRoom("0", 0, None, None, None), r0)

    val k1 = LogicalRoom("K1")
    Assert.assertEquals(
      LogicalRoom("K1", 0, Some("KEY"), Some(0), None),
      k1
    )

    val k2 = LogicalRoom("K2")
    Assert.assertEquals(
      LogicalRoom("K2", 1, Some("KEY"), Some(1), None),
      k2
    )

    val g3 = LogicalRoom("G3")
    Assert.assertEquals(
      LogicalRoom("G3", 2, Some("GATE"), Some(2), None),
      g3
    )

    val oneway = LogicalRoom("2<")
    Assert.assertEquals(2, oneway.zone)
    Assert.assertEquals("ONEWAY", oneway.tag.get)
    Assert.assertEquals(
      LogicalRoom("2<", 2, Some("ONEWAY"), None, Some(3)),
      oneway
    )
  }

  @Test
  def testReadSide(): Unit = {
    val logicalMap = LogicalMap[LogicalRoom, String]()
    logicalMap.nodes.put(Point3d(1, 0, 0), LogicalRoom("1")) // E
    logicalMap.nodes.put(Point3d(0, 1, 0), LogicalRoom("2")) // S
    // logicalMap.nodes.put(Point3d(-1, 0, 0), LogicalRoom("0")) // W
    logicalMap.nodes.put(Point3d(0, -1, 0), LogicalRoom("0")) // N

    logicalMap.edges.put(Edge(Point3d(0, 0, 0), Point3d(0, 1, 0)), "")

    Assert.assertEquals(
      Side(TileSpec.ConnBlocked, Some(1)),
      LogicalRoom.readSide(Point3d(0, 0, 0), Heading.E, logicalMap)
    )
    Assert.assertEquals(
      Side(TileSpec.ConnBlocked, Some(0)),
      LogicalRoom.readSide(Point3d(0, 0, 0), Heading.N, logicalMap)
    )

    Assert.assertEquals(
      Side(TileSpec.ConnRequired, Some(2)),
      LogicalRoom.readSide(Point3d(0, 0, 0), Heading.S, logicalMap)
    )
    Assert.assertEquals(
      Side(TileSpec.ConnOptional, None),
      LogicalRoom.readSide(Point3d(0, 0, 0), Heading.W, logicalMap)
    )

  }
}
