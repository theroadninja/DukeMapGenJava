package trn;

import trn.art.Tile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// see also MapLoader and Map.readMap()
/// TODO - docs for art file format: https://fabiensanglard.net/duke3d/BUILDINF.TXT

/**
 *
 *
 * Note:  this code seems to have the answers I need:
 * https://git.sr.ht/~jakob/rebuild/tree/master/src/bitmap.rs
 */
public class ArtFileReader {

    public static class ArtFile {

    }

    public static class Palette {

        // it takes 3 integers to represent a single color
        List<Integer> palette;

        public Palette(List<Integer> palette){
            this.palette = palette;
        }

        public Color getAwtColor(int colorIndex){
            int r = palette.get(colorIndex * 3);
            int g = palette.get(colorIndex * 3 + 1);
            int b = palette.get(colorIndex * 3 + 2);

            // the Color() constructor that takes ints does values 0..255
            // but it has a float constructor, so why not divide
            return new Color(r/64.0f, g/64.0f, b/64.0f);
        }

        /** creates a square image showing all the colors in the palette */
        public BufferedImage toImage(){
            // turn it into a png
            int scale = 8;
            int w = 16;
            int h = 16;
            BufferedImage image = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_ARGB);
            Color background = new Color(240, 240, 240);
            java.awt.Graphics gr = image.getGraphics();
            gr.setColor(background);
            gr.fillRect(0, 0, w * scale, h * scale);

            // so each 3 bytes describes 1 color?
            for(int i = 0; i < 256; ++i){

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

    public static List<String> findArtFiles(String folder){
        List<String> results = new ArrayList<String>();
        File[] files = new File(folder).listFiles();
        for(File f : files){
            String name = f.getName().toLowerCase();
            if(name.startsWith("tiles") && name.endsWith(".art")){
                results.add(f.getAbsolutePath());
            }
        }
        Collections.sort(results);
        return results;
    }

    public static void runTest(String filename) throws IOException {
        File path = new File(filename);
        FileInputStream bs = new FileInputStream(path);
        try{
            read(bs);
        }finally{
            try{ bs.close(); }finally{}
        }
    }

    private static void println(String s){
        System.out.println(s);
    }

    public static Palette readPalette(String filename) throws IOException {
        FileInputStream bs = new FileInputStream(new File(filename));
        try{
            return readPalette(bs);
        }finally{
            try{ bs.close(); }finally{}
        }
    }

    public static Palette readPalette(InputStream paletteFile) throws IOException {
        // https://github.com/fabiensanglard/chocolate_duke3D/blob/ef372086621d1a55be6dead76ae70896074ac568/Engine/src/engine.c
        // https://git.sr.ht/~jakob/rebuild/tree/master/src/bitmap.rs

        // VGA format (each r,g,b, value has value from 0..63 allowing for 64^3 colors)
        List<Integer> palette = new ArrayList<>(768);
        for(int i = 0; i < 768; ++i){
            palette.add((int)ByteUtil.readUInt8(paletteFile));
        }

        // number of shading tables used
        int numPaLookups = ByteUtil.readUint16LE(paletteFile);
        println("numPaLookups: " + numPaLookups);

        // shading tables
        List<Integer> paLookups = new ArrayList<>(numPaLookups * 256);
        for(int i = 0; i < numPaLookups * 256; ++i){
            paLookups.add((int)ByteUtil.readUInt8(paletteFile));
        }

        // translucent lookup table
        List<Integer> transluc = new ArrayList<>(256 * 256);
        for(int i = 0; i < 256 * 256; ++i){
            transluc.add((int)ByteUtil.readUInt8(paletteFile));
        }


        Palette palette2 = new Palette(palette);

        // File output = new File(HardcodedConfig.getDeployPath("output.png"));
        // ImageIO.write(palette2.toImage(), "png", output);

        return palette2;

    }


    public static List<Tile> read(String artFile) throws IOException {
        File path = new File(artFile);
        FileInputStream bs = new FileInputStream(path);
        try{
            return read(bs);
        }finally{
            try{ bs.close(); }finally{}
        }
    }

    public static List<Tile> read(InputStream artFile) throws IOException {

        // TODO - i don't know if these values are signed or not.  seems reasonable that most of them wouldnt be...
        // I belive a 'long' is a readInt32LE
        // a short is:  readInt16LE
        // signed char or unsigned char is: readUInt8


        // TODO - a modified copy of original source code, art loading section:
        // https://github.com/fabiensanglard/chocolate_duke3D/blob/ef372086621d1a55be6dead76ae70896074ac568/Engine/src/tiles.c

        int artversion = ByteUtil.readInt32LE(artFile);
        int numtiles = ByteUtil.readInt32LE(artFile); // this is garbase according to https://fabiensanglard.net/duke3d/BUILDINF.TXT

        // number of first tile
        int localtilestart = ByteUtil.readInt32LE(artFile);
        int localtileend = ByteUtil.readInt32LE(artFile);

        int tileCount = localtileend - localtilestart + 1;
        List<Integer> tilesizex = new ArrayList<>(tileCount);
        for(int i = 0; i < tileCount; ++i){
            tilesizex.add((int)ByteUtil.readInt16LE(artFile));
        }
        List<Integer> tilesizey = new ArrayList<>(tileCount);
        for(int i = 0; i < tileCount; ++i){
            tilesizey.add((int)ByteUtil.readInt16LE(artFile));
        }

        List<Integer> picanm = new ArrayList<>(tileCount);
        for(int i = 0; i < tileCount; ++i){
            picanm.add(ByteUtil.readInt32LE(artFile));
        }


        // TODO - and then its a bunch of data

        List<Tile> tiles = new ArrayList<Tile>();
        for(int i = 0; i < tileCount; ++i){

            int width = tilesizex.get(i);
            int height = tilesizey.get(i);

            List<Integer> paletteIndexes = new ArrayList<>(width * height);

            for(int p = 0; p < width * height; ++p){
                paletteIndexes.add((int)ByteUtil.readUInt8(artFile));
                //paletteIndexes.add((int)ByteUtil.readInt16LE(artFile)); //not sure if this is 16 bit or not
            }

            tiles.add(new Tile(i, width, height, paletteIndexes));
        }




        // println("art version: " + artversion);
        // println("start tile: " + localtilestart);
        // println("end tile: " + localtileend);
        for(int i = 0; i < tilesizex.size(); ++i){
            int num = localtilestart + i;
            if(tilesizex.get(i) > 0){
                println("num=" + num + " tile size x=" + tilesizex.get(i));
            }
            // println("tile " + num + " size: " + tilesizex.get(i) + " x " + tilesizey.get(i));

        }

        return tiles;
    }

    public static void main(String[] args) throws Exception {

        String paletteFile = HardcodedConfig.PATH_WITH_ART + "PALETTE.DAT";
        Palette palette = readPalette(paletteFile);


        java.util.List<String> files = ArtFileReader.findArtFiles(HardcodedConfig.PATH_WITH_ART);

        String ff = files.get(0);
        List<Tile> results = ArtFileReader.read(files.get(0));

        Tile uglyWall = results.get(0);
        BufferedImage image = uglyWall.toImage(palette, 10);
        File output = new File(HardcodedConfig.getDeployPath("output.png"));
        ImageIO.write(image, "png", output);



        // for(String ff : files){
        // 	ArtFileReader.runTest(ff);
        // }
    }
}
