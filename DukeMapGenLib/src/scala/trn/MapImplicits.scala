package trn

import trn.prefab.Matrix2D
import trn.{Map => DMap}
import scala.collection.JavaConverters._

/** extending the Map class with scala stuff */
object MapImplicits {

  private def reverseWalls(map: DMap): Unit = {
    val walls2 = new java.util.ArrayList[Wall](map.walls.size())

    // map of (p2 -> p1) where p1.getPoint2 == p2
    val reversedLinks = map.walls.asScala.zipWithIndex.map{case (w, i) => (w.getPoint2Id -> i) }.toMap

    def reversedWall(w: Wall, wallId: Int): Wall ={
      val point2 = map.getWall(w.getPoint2Id)
      val w2: Wall = w.copy()
      w2.x = point2.x
      w2.y = point2.y
      w2.setPoint2Id(reversedLinks(wallId))
      w2
    }

    for(i <- 0 until map.walls.size()){
      val w = map.walls.get(i)
      walls2.add(reversedWall(w, i))
    }
    map.walls = walls2
  }

  class MapExtended(map: DMap) {

    def allSprites: Seq[Sprite] = {
      val list = new collection.mutable.ArrayBuffer[Sprite](map.getSpriteCount)
      for(i <- 0 until map.getSpriteCount){
        list += map.getSprite(i)
      }
      list
    }

    def allWalls: Seq[Wall] = {
      val list = new collection.mutable.ArrayBuffer[Wall](map.getWallCount)
      for(i <- 0 until map.getWallCount){
        list += map.getWall(i)
      }
      list
    }

    def rotatedCW(anchor: PointXY): DMap = {
      val transform = Matrix2D.rotateAroundCW(anchor)
      val copy = map.copy()
      copy.walls.asScala.foreach{ w =>
        val translated = transform * w.getLocation
        w.setLocation(translated)
      }
      copy.sprites.asScala.foreach{ s =>
        val translated = transform * s.getLocation.asPointXY()
        s.x = translated.x
        s.y = translated.y
        s.setAng(Sprite.rotateAngleCW(s.ang))
      }


      // TODO - do angles
      // (convert to unit-ish vector?)
      copy
      // TODO - the problem is: this fucks up the wall loops!!!!
    }



    def flippedX(anchorX: Int): DMap = {
      val transform = Matrix2D.flipXat(anchorX)
      val copy = map.copy()
      copy.walls.asScala.foreach{ w =>
        val translated = transform * w.getLocation
        w.setLocation(translated)
      }
      copy.sprites.asScala.foreach{ s =>
        val translated = transform * s.getLocation.asPointXY()
        s.x = translated.x
        s.y = translated.y
        // TODO - s.setAng(Sprite.rotateAngleCW(s.ang))
      }

      // NOW we need to reverse the walls, because flipping fucked up the clockwise pattern
      reverseWalls(copy)

      copy
    }



  }
  implicit def mapExtended(map: DMap) = new MapExtended(map)
}

