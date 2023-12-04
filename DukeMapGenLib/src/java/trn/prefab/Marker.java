package trn.prefab;

import trn.Sprite;

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
        public static int SWITCH_REQUESTED = 15;

        /**
         * For simple, algorithm-specific use cases that require a marker that are too esoteric to be worth making into
         * a generic feature.
         *
         * E.g. inventing this to mark sectors where touchplaces get inserted for the HyperLoop algorithm.
         *
         * This may be an accidental duplicate of ALGO_HINT
         */
        public static int ALGO_GENERIC = 16;

        public static int REDWALL_MARKER = 20;

        /**
         * Used to place randomly chosen items (weapons, ammo, powerups).
         *
         * Not meant for important things that the algorithm needs to work with, like keys.  For those,
         * see "ITEM" (lotag 9).
         *
         * Since I don't know what I'm doing yet, the hitag will just indicate behavior for specific algorithms.
         *
         * For more info see the RandomItemMarker class.
         */
        public static int RANDOM_ITEM = 23;


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

    }

    // TODO this text should be game-specific!
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
}
