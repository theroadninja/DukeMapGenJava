package trn.duke;

import trn.Sprite;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameLogic {

    private static final Set<Integer> SE_SPECIAL_ANGLE = new HashSet<>(Arrays.asList(new Integer[]{0, 11, 13, 21, 31, 32}));

    /**
     * When a sector is rotated or flipped, most of the sprites should be rotated or flipped with it, which really just
     * means that their angle should undergo the same transformation as the sector.  However, the angles of some sprites
     * have a special meaning and do NOT represent a vector in 3D space (e.g. SE sprites where you set the angle "UP"
     * or "DOWN") and those angles should not be altered.
     *
     * See also https://wiki.eduke32.com/wiki/Sector_Effector_Reference_Guide
     *
     * @param s
     * @return true if the sprite should be rotated, false if the sprite should have a special meaning.
     */
    public static boolean shouldRotate(Sprite s){
        if(s.getTexture() == TextureList.SE){
            if(SE_SPECIAL_ANGLE.contains(s.getLotag())){
                return false;
            }
        }

        return true;

    }
}
