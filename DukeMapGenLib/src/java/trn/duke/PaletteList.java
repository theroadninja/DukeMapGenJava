package trn.duke;

/**
 * List of "pal" values (texture palettes).
 *
 * See https://wiki.eduke32.com/wiki/Palette_(environment)
 */
public class PaletteList {

    public static int NORMAL = 0;

    /**
     * no palette change - recommended for the big orbit texture,
     * because you need a texture in order to not kill the player.
     *
     * Do we also apply to BOSS1 to turn it into a battlelord sentry?
     *
     * TODO - add a note to big orbit in texture list about needing to set a palette
     * (any palette) if you want to avoid killing the player
     *
     */
    public static int ALSO_NORMAL = 3;

    public static int BLUE_TO_RED = 21;




    // ----------------
    public static int KEYCARD_BLUE = 0;
    public static int KEYCARD_RED = 21;
    public static int KEYCARD_YELLOW = 23;

}
