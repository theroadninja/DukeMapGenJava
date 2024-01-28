package trn.art;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * An old school 2d image "palette" which is just a big lookup table of colors.  See Tile, ArtFileReader
 */
public class Palette {

    // it takes 3 integers to represent a single color
    List<Integer> palette;

    public Palette(List<Integer> palette) {
        this.palette = palette;
    }

    public Color getAwtColor(int colorIndex) {
        int r = palette.get(colorIndex * 3);
        int g = palette.get(colorIndex * 3 + 1);
        int b = palette.get(colorIndex * 3 + 2);

        // the Color() constructor that takes ints does values 0..255
        // but it has a float constructor, so why not divide
        // (see readPalette())
        return new Color(r / 64.0f, g / 64.0f, b / 64.0f);
    }

    /**
     * creates a square image showing all the colors in the palette
     */
    public BufferedImage toImage() {
        // turn it into a png
        int scale = 8;
        int w = 16;
        int h = 16;
        BufferedImage image = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_ARGB);
        Color background = new Color(240, 240, 240);
        Graphics gr = image.getGraphics();
        gr.setColor(background);
        gr.fillRect(0, 0, w * scale, h * scale);

        // so each 3 bytes describes 1 color?
        for (int i = 0; i < 256; ++i) {

            // int r = palette.get(i * 3);
            // int g = palette.get(i * 3 + 1);
            // int b = palette.get(i * 3 + 2);

            // the Color() constructor that takes ints does values 0..255
            // but it has a float constructor, so why not divide
            //Color awtColor = new Color(r/64.0f, g/64.0f, b/64.0f);
            Color awtColor = getAwtColor(i);

            int row = i / 16;
            int col = i % 16;
            int y = row * scale;
            int x = col * scale;
            gr.setColor(awtColor);
            gr.fillRect(x, y, x + scale, y + scale);
        }

        return image;
    }

}
