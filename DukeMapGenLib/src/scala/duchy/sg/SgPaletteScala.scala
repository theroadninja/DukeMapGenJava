package duchy.sg

import trn.Sprite
import trn.prefab.{SectorGroup, Marker}

import scala.collection.JavaConverters._

/**
  * TODO Scala case class meant to replace PrefabPalette.java
  *
  * Represents all the "sector groups" that can be found in a single map file.
  */
case class SgPaletteScala(
  /**
    * sector groups with a numerical id
    * the key of the map is the id of the group
    */
  numberedGroups: Map[Int, SectorGroup],

  /**
    * sector groups that are actually part of another group with an id, but aren't connected by a redwall
    * e.g. underwater places, or places you teleport or fall to
    * the key of the map is the parent id
    */
  teleportChildGroups: Map[Int, Seq[SectorGroup]],

  /**
    * just sector groups that don't have an id number
    */
  anonymousSectorGroups: Seq[SectorGroup],


  spriteSectorGroups: Map[Int, SectorGroup],

) {

  /**
    * TODO an alternate idea I had was, instead of using a special sector, just put a bunch of sprites
    * in a sector and one or more markers and they "choose" from the available sprites (replaces the sprite
    * options with blank markers) -- it makes more sense for enemies to be randomly places, while item
    * sprites probably work better in a specific position
    */
  lazy val spriteGroups: Map[Int, Seq[Sprite]] = spriteSectorGroups.map { case (id, sg) =>
    id -> sg.allSprites.filterNot(s => Marker.isMarker(s)).toSeq
  }

  def getNumberedGroupsAsJava: java.util.Map[Integer, SectorGroup] = numberedGroups.map {
    case (groupId, group) => {
      val groupIdInteger: java.lang.Integer = groupId
      groupIdInteger -> group
    }
  }.asJava

  def getTeleportChildrenAsJava: java.util.Map[Integer, java.util.List[SectorGroup]] = teleportChildGroups.map {
    case (groupId, children) => {
      val groupIdInteger: java.lang.Integer = groupId
      val childrenJava = children.asJava
      (groupIdInteger -> childrenJava)
    }
  }.asJava

  def getAnonymousAsJava: java.util.List[SectorGroup] = anonymousSectorGroups.asJava

  def getSpriteGroup(spriteGroupId: Int): Seq[Sprite] = spriteGroups(spriteGroupId)


}
