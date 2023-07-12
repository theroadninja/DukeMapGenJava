package duchy.sg

import trn.prefab.SectorGroup

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

) {

}
