package trn.art;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Contains a "tile" or image from an editart file.  See ArtFileReader.
 *
 */
public class Tile implements Comparable<Tile> {

    /** index across multiple files; the 'picnum' in Build */
    final int globalTileIndex;

    final int tileIndex; // this is just its index within the file
    final int width;
    final int height;
    final List<Integer> paletteIndexes; // the pixel data

    public Tile(int globalTileIndex, int tileIndex, int width, int height, List<Integer> paletteIndexes){
        this.globalTileIndex = globalTileIndex;
        this.tileIndex = tileIndex;
        this.width = width;
        this.height = height;
        this.paletteIndexes = paletteIndexes;
        if(this.paletteIndexes.size() != this.width * this.height){
            throw new IllegalArgumentException("wrong number of pixels");
        }
    }

    public int getGlobalIndex(){
        return this.globalTileIndex;
    }

    public int picnum(){
        return getGlobalIndex();
    }

    public int getIndex(){
        return this.tileIndex;
    }

    public int getWidth(){
        return this.width;
    }

    public int getHeight(){
        return this.height;
    }

    public boolean isValid(){
        return this.width > 0 && this.height > 0;
    }

    public BufferedImage toImage(Palette palette, int scale){
        if(scale < 1) throw new IllegalArgumentException();

        BufferedImage image = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics gr = image.getGraphics();

        for(int i = 0; i < this.paletteIndexes.size(); ++i){
            int y = i % this.height;
            int x = i / this.height;

            Color c = palette.getAwtColor(this.paletteIndexes.get(i));
            gr.setColor(c);
            // these are pixels; using fillRect b/c of scaling
            gr.fillRect(x * scale, y * scale, (x+1) * scale, (y+1) * scale);
        }
        return image;
    }

    @Override
    public int compareTo(Tile other){
        return this.globalTileIndex - other.globalTileIndex;
    }
}
