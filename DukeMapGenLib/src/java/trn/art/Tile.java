package trn.art;

import trn.ArtFileReader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class Tile {
    final int tileIndex; // TODO - i think this is just its index within the file
    final int width;
    final int height;
    final List<Integer> paletteIndexes;
    // TODO: add picanm

    public Tile(int tileIndex, int width, int height, List<Integer> paletteIndexes){
        this.tileIndex = tileIndex;
        this.width = width;
        this.height = height;
        this.paletteIndexes = paletteIndexes;
        if(this.paletteIndexes.size() != this.width * this.height){
            throw new IllegalArgumentException("wrong number of pixels");
        }
    }

    public boolean isValid(){
        return this.width > 0 && this.height > 0;
    }

    public BufferedImage toImage(ArtFileReader.Palette palette, int scale){
        if(scale < 1) throw new IllegalArgumentException();

        BufferedImage image = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics gr = image.getGraphics();

        for(int i = 0; i < this.paletteIndexes.size(); ++i){
            int y = i % this.height;
            int x = i / this.height;

            Color c = palette.getAwtColor(this.paletteIndexes.get(i));
            gr.setColor(c);
            gr.fillRect(x * scale, y * scale, (x+1) * scale, (y+1) * scale);
        }

        return image;
    }
}
