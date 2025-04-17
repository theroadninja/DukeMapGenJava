package trn.prefab;

import trn.Sprite;

import java.util.Arrays;
import java.util.List;

/**
 * Support code for Marker Sprites
 */
public class Marker {

    public static class Lotags {

        /**
         * lotag of construction sprite whose hitag serves as an id for the group
         *
         * Note: if a sector group does NOT have a group id, then it also cannot have a sprite with texture 0 and a
         * lotag of 1 (only doing this to remind user they forgot to set the texture).
         */
        public static int GROUP_ID = 1;

        // is the hitag then a priority to beat out other player starts?
        public static int PLAYER_START = 2;

        /**
         * An anchor whose position you can read to help place a sector group on the grid.
         * For example, if you want to place the group such that the middle of the room is in
         * a certain spot, put an anchor in the middle of the room and use its coordinates for
         * translation.
         */
        public static int ANCHOR = 3;

        /**
         * Identifies this sector group as a child of another sector group.  A redwall child group cannot have its
         * own ID (no marker sprites with lotag 1).
         *
         * The child group can only be a child to one parent, and cannot exist on its own.  It will be absorbed into
         * the parent.
         *
         * TODO:  support child sectors that connect to other child sectors with the same parent.  Example:  parent is
         * 	sector A:  children are B and C.  The connectorIDs are arranged such that B connects to A, and C connects to
         * 	B.
         *
         * lotag:  4
         * hitag:  ID of parent sector group  (if parent doesnt exist yet, dont add this marker yet)
         * sector placed in:   same sector as the redwall connector to use
         * 		the redwall connector must have a connectorID that matches a connector in the parent group
         * 		the parent group can only have one connector with that connectorID (TODO - update)
         * 		the child connector id must be > 0
         */
        public static int REDWALL_CHILD = 4;

        /**
         * Marks a sector with existing text sprites meant to spell out words.
         *
         * Marker sprite:
         * 	lotag: 5
         * 	hitag: 0 OR some ID
         *
         * Text Sprites:
         *   lotag:  MUST be non zero, value indicates order (smaller numbers to the left)
         *
         */
        public static int AUTO_TEXT = 5;

        /**
         * The sector group should stay right where it is.  If it has an ID ( GROUP_ID set ) then it will be scanned
         * and can be pasted again, but the original copy will stay where it is.
         *
         */
        public static int STAY = 6;

        /**
         * Sprite marks a good location for an enemy.
         *
         * See the `EnemyMarker` scala class.
         *
         * See MoonBase2.adjustEnemies(), MoonBase2.Enemy
         */
        public static int ENEMY = 8;  // maybe this should be called "random enemy"

        /**
         * Location for a powerup, including keys.
         */
        public static int ITEM = 9;

        /**
         * TODO - this was unused
         * Original description: Used for a sector group that isnt a real sector group but it only used as input for a particular generator
         * algorithm.
         * TODO see also ALGO_HINT and ALGO_GENERIC
         */
        // public static int GENERATOR_INPUT = 10;  TODO

        /**
         * This is for a companion sector group that is connected only via water/teleporters/elevators,
         * and not by redwalls.  This companion sector must be pasted and linked to its parent, however
         * it can be pasted anyway.
         *
         * See REDWALL_CHILD for a child group that connects via a redwall.
         *
         * All teleporer and elevator connectors between groups must match via connector ids.
         *
         * TODO - water automatic?
         *
         * Lotag: 11
         * Hitag:  matches sector group id of parent sector group
         */
        public static int TELEPORT_CHILD = 11;

        /**
         * Causes the entire sector group to be translazed along the z-axis as it is being read from the source file.
         * This is for lazy people who want to use elevators but forgot to pgdown the lower sector group.
         *
         * Only one of these may exist in a single source file. (did I mean "sector group"?)
         *
         * Sprite hitag:  set to amount of z to translate by - TODO better explanation - positive for down?
         */
        public static int TRANSLATE_Z = 12;

        /**
         * Generic algorithm "Hint" sprite.  The meaning of the sprite is specific to the algorithm being used to
         * generate the map.
         *
         * TODO possibly replaced by ALGO_GENERIC?   This one is used by Moonbase and Hypercube (see also SWITCH_REQUESTED)
         * Moonbase has its own set of integer values to match against the hitag, so maybe this one should be used for
         * tagging the sector group with flags.
         *
         */
        public static int ALGO_HINT = 13;

        /**
         * For some grid-based algorithms, to lock a room to a certain value on an axis.
         *
         * NOTE:  maybe I should have used the SHADE to indicate which axis instead.
         *
         * Hitag:     Locks:
         * 0          x=0
         * 1          x=1
         * 2          x=2
         * ...
         * 16         y=0
         * 17         y=1
         * 18         y=2
         * ...
         * 32         z=0
         * 33         z=1
         * 34         z=2
         * ...
         * 48         w=0
         * 49         w=1
         */
        public static int ALGO_AXIS_LOCK = 14;

        /**
         * Used to indicate that a sector group has some kind of activated affect that needs to be paired
         * with a switch or touchplate in another sector group.  The hitag of the marker sprite has the link id.
         *
         * You might also use this if you have switch channels in two groups you want to sync up.
         *
         * Examples:
         * - a sector group has a marker sprite with lotag=15 and hitag=1024 then a switch needs to be
         * created with lotag=1024
         * - the sector group has an ACTIVATOR with lotag=100, then a switch with lotag=100 is needed.
         *
         * // TODO refer to "link id" as "channel" instead?  Thats what infosuite.duke4.net uses
         *
         * Values from this marker are set in SectorGroupProperties (see SectorGroupProperties.scanMap())
         *
         * The hitag of this sprite must be updated when pasting to avoid collisions.  See
         * GameConfig.updateUniqueTagInPlace()
         */
        public static int SWITCH_REQUESTED = 15; // TODO maybe this should be called CHANNEL_REQUESTED

        /**
         * For simple, algorithm-specific use cases that require a marker that are too esoteric to be worth making into
         * a generic feature.
         *
         * E.g. inventing this to mark sectors where touchplaces get inserted for the HyperLoop algorithm.
         *
         * This may be an accidental duplicate of ALGO_HINT
         */
        public static int ALGO_GENERIC = 16;

        /**
         * Elevator Connector
         *
         * To make an elevator with this:
         * 		marker sprite lotag 17
         * 		sector lotag 15
         */
        public static int ELEVATOR_CONNECTOR = 17;

        // TODO rename to REDWALL_CONNECTOR (obviously its a marker...)
        public static int REDWALL_MARKER = 20;
        public static int SIMPLE_CONNECTOR = 20; // TODO get rid of this one

        /**
         * Used to extend a redwall connector across sectors.
         *
         * This one is not deprecated!   marker 20 does not automatically extend into other sectors
         * you must place one of these in each sector of a redwall conn
         * NOTE this is NOT about making "child" sector groups.  See Redwall Child for that (lotag 4)
         */
        public static int MULTISECTOR_CHILD = 21;  // accomplishes multi-sector redwall conns by being a child segment

        // TODO don't use 22 yet until sure its gone from test maps -- that was the old MULTI_SECTOR

        /**
         * Used to place randomly chosen items (weapons, ammo, powerups).
         *
         * Not meant for important things that the algorithm needs to work with, like keys.  For those,
         * see "ITEM" (lotag 9).
         *
         * Since I don't know what I'm doing yet, the hitag will just indicate behavior for specific algorithms.
         *
         * For more info see the RandomItemMarker class.
         *
         * To implement, see RandomItemMarker (??)
         */
        public static int RANDOM_ITEM = 23;

        /**
         * A connector sprite that becomes a normal or water teleporter.
         * (but not a silent teleporter).
         * When done this way, you don't need an SE sprite because this sprite
         * becomes an SE sprite.
         *
         * You can also make a teleporter connector by putting a simple connector
         * in a sector group with a teleporter.  TODO - is this still true?
         */
        public static int TELEPORT_CONNECTOR = 27;

        /**
         * A teleporter off the ground that simulates falling between sectors.
         */
        public static int FALL_CONNECTOR = 28;

        /**
         * A placeholder that does nothing and gets removed.  I'm using this to as a hack to avoid using Option
         * in some places.
         */
        public static int BLANK = 29;

        /**
         * Supplies an alternate texture that can be applied to the floor, so make the sector group more random.
         * Can use more than one in a sector.
         *
         * Set the hitag to the texture number of the other texture.
         * Set the shade of the sprite to the shade the texture will be.
         *
         * TODO:  do we randomly apply, or also have a method to create the cartesian product of all variations?
         */
        public static int ALTERNATE_FLOOR_TEX = 30;

        /**
         * Marks a SectorGroup as the definition of  "Sprite Group" -- a bag of sprites that can be
         * randomly inserted by enemy and item markers.
         *
         * Lotag: 31
         * Hitag:  the id of the sprite group
         *
         * Other Requirements:
         * - the sectorgroup must have exactly 1 sector
         * - the sectorgroup must not have an ID (marker lotag=1)
         *
         */
        public static int SPRITE_GROUP_ID = 31;

        /**
         * Marks a spot where a randomly chosen sprite will be placed.  The
         * sprite is selected from the set of all non-marker sprites (and cant
         * have that R thing set either) in the same sector.  More than one of
         * these markers can be in the same sector.
         *
         * Note:  this doesnt use sprite groups defind elsewhere -- it only pulls from
         * sprites in the same sector.
         *
         * hitag: (none)
         */
        public static int INLINE_SPRITE_GROUP = 32;

        /**
         * Identifies what "type" of sprite group this is for the purpose of algorithms
         * that assemble sprite groups into bigger sprite groups.  Its meaning depends
         * on the algorithm being used.
         *
         * hitag:  the type of the sprite group
         */
        public static int SG_TYPE = 33;

        private static List<Integer> ALL = Arrays.asList(new Integer[]{
                GROUP_ID,
                PLAYER_START,
                ANCHOR,
                REDWALL_CHILD,
                AUTO_TEXT,
                STAY,
                ENEMY,
                ITEM,
                // GENERATOR_INPUT,
                TELEPORT_CHILD,
                TRANSLATE_Z,
                ALGO_HINT,
                ALGO_AXIS_LOCK,
                SWITCH_REQUESTED,
                ALGO_GENERIC,
                ELEVATOR_CONNECTOR,
                REDWALL_MARKER,
                MULTISECTOR_CHILD,
                RANDOM_ITEM,
                // MULTI_SECTOR(22) no longer used
                TELEPORT_CONNECTOR,
                FALL_CONNECTOR,
                BLANK,
                ALTERNATE_FLOOR_TEX,
                SPRITE_GROUP_ID,
                INLINE_SPRITE_GROUP,
                SG_TYPE,
        });
    }


    // TODO alternative marker for algo-specific stuff:  310

    // TODO this tex should be game-specific!
    public static int MARKER_SPRITE_TEX = 355;  // the construction sprite
    public static int TEX = 355;  // shorter version!  switch to this!

    public static boolean isMarker(Sprite s) {
        return s.getTexture() == MARKER_SPRITE_TEX && s.getPal() == 0 && s.getLotag() > 0;
    }

    public static boolean isMarker(Sprite s, int lotag){
        return isMarker(s) && s.getLotag() == lotag;
    }

    public static boolean isInvalidMarker(Sprite s){
        boolean isMarker = s.getTexture() == MARKER_SPRITE_TEX && s.getPal() == 0;
        return isMarker && s.getLotag() == 0;  // TODO if lotag nonzero, make sure its valid
    }

    public static boolean hasUniqueHitag(Sprite s){
        return isMarker(s) && s.getLotag() == Lotags.SWITCH_REQUESTED;
    }

    public static void checkValid(Sprite s) throws SpriteLogicException {
        if(Marker.isMarker(s) && !Lotags.ALL.contains(s.getLotag())){
            throw new SpriteLogicException("invalid marker sprite", s.getLocation().asXY());
        }
    }
}
