package trn.prefab

import trn.{LineSegmentXY, PointXY, WallView}

object Polygon {

  val IntersectVectors: Seq[PointXY] = (-9 until 10).flatMap{
    i => Seq(new PointXY(-1, i), new PointXY(1, i), new PointXY(i, 1), new PointXY(i, -1))
  }

  def rayIntersectAny(rayStart: PointXY, rayVector: PointXY, vertexes: Seq[PointXY], radius: Int): Boolean = {
    require(radius > 0)
    vertexes.exists(vertex => PointXY.rayCircleIntersect(rayStart, rayVector, vertex, radius))
  }


  /**
    *  TODO -  THIS DOESNT DEAL WITH HOLES YET
    *
    * Exclusive means if its on the line or a vertex, that counts as being OUTSIDE the loop.
    *
    * @param outerWallLoop
    * @param point
    * @return
    */
  def containsExclusive(outerWallLoop: Seq[LineSegmentXY], point: PointXY): Boolean = {
    val bb = BoundingBox(outerWallLoop.map(_.getP1))
    if(! bb.contains(point)){
      false
    }else{
      // first, find a ray that does not intersect with any vertexes
      val vertices = outerWallLoop.map(_.getP1)
      val rayVector: PointXY = IntersectVectors.find { v =>
        !rayIntersectAny(point, v, vertices, 2)
      }.getOrElse(throw new MathIsHardException("Polygon.containsExclusive cant handle this case yet"))

      val intersects = outerWallLoop.filter { segment =>
        // TODO if it goes through a vertex, this is fucked (currently mitigating by creative ray choice)
        segment.intersectsRay(point, rayVector, false) && !segment.isParallel(rayVector);
      }

      intersects.size % 2 == 1
    }
  }

  def containsExclusive2(outerWallLoop: Seq[WallView], point: PointXY): Boolean = {
    containsExclusive(outerWallLoop.map(_.getLineSegment), point)
  }

  /** Dont call this directly, except for unit tests.  TODO this is a heuristic only
    */
  private[prefab] def containsPoints(wallLoop: Seq[LineSegmentXY], points: Seq[PointXY]): Boolean = points.exists { point =>
    try {
      containsExclusive(wallLoop, point)
    } catch {
      case ex: MathIsHardException => false // probably a vertex overlap, we are ignoring these for now
    }
  }

  /**
    * This method should be 100% accurate when it says polys are overlapped, but may
    * be wrong if it says they are not.
    *
    * WARNING: this does not do a bounding box test.
    *
    * @return true if our heuristic suggests the polygons are overlapped
    */
  def guessOverlap(wallLoop1: Seq[LineSegmentXY], wallLoop2: Seq[LineSegmentXY]): Boolean = {


    containsPoints(wallLoop1, wallLoop2.map(_.getP1)) || containsPoints(wallLoop2, wallLoop1.map(_.getP1)) || {
      wallLoop1.exists { w1 =>
        wallLoop2.exists { w2 =>
          PointXY.intersectSementsForPoly(w1.getP1, w1.getVector, w2.getP1, w2.getVector, false, false)
        }
      }
    }
  }

  def guessGroupsOverlap(loops1: Seq[Seq[LineSegmentXY]], loops2: Seq[Seq[LineSegmentXY]]): Boolean = {
    loops1.exists { wallLoop1 => loops2.exists { wallLoop2 =>
        guessOverlap(wallLoop1, wallLoop2)
    }}
  }


}
