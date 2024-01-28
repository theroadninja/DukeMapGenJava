package trn.art;

import trn.ByteUtil;
import trn.HardcodedConfig;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Reads the art files, including PALETTE.DAT and TILES###.ART
 *
 * Note:  this code seems to have the answers I need:
 * https://git.sr.ht/~jakob/rebuild/tree/master/src/bitmap.rs
 * docs for art file format: https://fabiensanglard.net/duke3d/BUILDINF.TXT
 *
 * See also TextureUtil
 */
public class ArtFileReader {

    /**
     * Locates all files in a folder that follow this pattern:
     *  TILES000.ART
     *  TILES001.ART
     *  TILES002.ART
     *  ...
     *
     * @param folder the folder to look for "art" files
     * @return the absolute paths of all the files
     */
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

    /**
     * TODO this doesnt do anything with the shading/translucent tables.
     */
    public static Palette readPalette(InputStream paletteFile) throws IOException {
        // https://github.com/fabiensanglard/chocolate_duke3D/blob/ef372086621d1a55be6dead76ae70896074ac568/Engine/src/engine.c
        // https://git.sr.ht/~jakob/rebuild/tree/master/src/bitmap.rs

        // VGA format (each r,g,b, value has value from 0..63 allowing for 64^3 colors)
        List<Integer> palette = new ArrayList<>(768);
        for(int i = 0; i < 768; ++i){
            palette.add((int) ByteUtil.readUInt8(paletteFile));
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
        return palette2;
    }


    public static List<Tile> read(String artFile, int startIndex) throws IOException {
        File path = new File(artFile);
        FileInputStream bs = new FileInputStream(path);
        try{
            return read(bs, startIndex);
        }finally{
            try{ bs.close(); }finally{}
        }
    }

    /**
     * Read all of the art files, assuming they are a set and in order.
     *
     * If any files are missing or out of order, the file indexes / picnums will be off.
     *
     * @param artFiles
     * @return
     * @throws IOException
     */
    public static List<Tile> readAll(Iterable<String> artFiles) throws IOException {
        List<Tile> results = new LinkedList<>();
        for(String artFile : artFiles){
            // results.size() is correct!
            results.addAll(read(artFile, results.size()));
        }
        return results;
    }

    public static List<Tile> read(InputStream artFile, int startIndex) throws IOException {
        // TODO - i don't know if these values are signed or not.  seems reasonable that most of them wouldnt be...
        // I belive a 'long' is a readInt32LE
        // a short is:  readInt16LE
        // signed char or unsigned char is: readUInt8

        // TODO - a modified copy of original source code, art loading section:
        // https://github.com/fabiensanglard/chocolate_duke3D/blob/ef372086621d1a55be6dead76ae70896074ac568/Engine/src/tiles.c

        int artversion = ByteUtil.readInt32LE(artFile);
        int numtiles = ByteUtil.readInt32LE(artFile); // this is garbage according to https://fabiensanglard.net/duke3d/BUILDINF.TXT

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

        // and then its a bunch of data
        List<Tile> tiles = new ArrayList<Tile>();
        for(int i = 0; i < tileCount; ++i){
            int width = tilesizex.get(i);
            int height = tilesizey.get(i);
            List<Integer> paletteIndexes = new ArrayList<>(width * height);
            for(int p = 0; p < width * height; ++p){
                paletteIndexes.add((int)ByteUtil.readUInt8(artFile));
            }
            tiles.add(new Tile(startIndex + i, i, width, height, paletteIndexes));
        }
        return tiles;
    }

    /**
     * @param artPath  path of folder containing the art files
     * @param destFolder  path of folder to write tile images to
     */
    public static void extractTiles(List<Tile> tiles, Palette palette, String artPath, String destFolder) throws IOException {
        final int SCALE = 2;
        for(int tileIndex = 0; tileIndex < tiles.size(); ++tileIndex){
            Tile tile = tiles.get(tileIndex);
            if(tile.isValid()){
                writeTile(destFolder, tile, palette, SCALE);
            }
        }
    }

    /**
     * For testing
     */
    private static void writeTile(String folder, Tile tile, Palette palette, int scale) throws IOException {
        if(scale < 1) throw new IllegalArgumentException("scale must be >= 1");
        String outfile = String.format("tile%05d.png", tile.getGlobalIndex());
        ImageIO.write(tile.toImage(palette, 2), "png", new File(folder + outfile));
    }

    public static void main(String[] args) throws Exception {
        String paletteFile = HardcodedConfig.PATH_WITH_ART + "PALETTE.DAT";
        Palette palette = readPalette(paletteFile);
        List<Tile> tiles = ArtFileReader.readAll(ArtFileReader.findArtFiles(HardcodedConfig.PATH_WITH_ART));

        // extract and write images
        extractTiles(tiles, palette, HardcodedConfig.PATH_WITH_ART, HardcodedConfig.TILES_OUTPUT_PATH);

        // print the texture heights
        Collections.sort(tiles);
        for(Tile tile: tiles){
            println(String.format("%s=%s", tile.getGlobalIndex(), tile.getHeight()));
        }
    }
}
