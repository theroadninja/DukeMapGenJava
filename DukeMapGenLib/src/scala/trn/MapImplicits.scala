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
        s.setLocation(translated)
        s.setAng(Sprite.rotateAngleCW(s.ang))
      }
      copy
    }


    private def applyTransforms(
      transform: Matrix2D,
      angleTransform: Matrix2D,
      mustReverseWalls: Boolean
    ): DMap = {
      val copy = map.copy()
      copy.walls.asScala.foreach{ w =>
        val translated = transform * w.getLocation
        w.setLocation(translated)
      }
      copy.sprites.asScala.foreach{ s =>
        val translated = transform * s.getLocation.asPointXY()
        s.setLocation(translated)

        // TODO - check MapUtil.shouldRotate() first ...
        val vector: PointXY = angleTransform * AngleUtil.unitVector(s.ang)
        s.setAngle(AngleUtil.angleOf(vector))
      }

      // NOW we need to reverse the walls, because flipping fucked up the clockwise pattern
      if(mustReverseWalls){
        reverseWalls(copy)
      }
      copy
    }

    def flippedX(anchorX: Int): DMap = {
      val transform = Matrix2D.flipXat(anchorX)
      val angleTransform = Matrix2D.flipX
      applyTransforms(transform, angleTransform, true)
    }

    def flippedY(anchorY: Int): DMap = {
      val transform = Matrix2D.flipYat(anchorY)
      val angleTransform = Matrix2D.flipY
      applyTransforms(transform, angleTransform, true)
    }



  }
  implicit def mapExtended(map: DMap) = new MapExtended(map)
}

