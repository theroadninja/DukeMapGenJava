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

    public static boolean hasUniqueHitag(Sprite s){
        return isMarker(s) && s.getLotag() == Lotags.SWITCH_REQUESTED;
    }
}
