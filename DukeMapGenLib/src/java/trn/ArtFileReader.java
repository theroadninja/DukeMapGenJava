package trn;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// see also MapLoader and Map.readMap()
public class ArtFileReader {

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

    public static void read(InputStream artFile) throws IOException {

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


        // println("art version: " + artversion);
        // println("start tile: " + localtilestart);
        // println("end tile: " + localtileend);
        for(int i = 0; i < tilesizex.size(); ++i){
            int num = localtilestart + i;
            if(tilesizex.get(i) > 0){
                println("" + num + "=" + tilesizex.get(i));
            }
            // println("tile " + num + " size: " + tilesizex.get(i) + " x " + tilesizey.get(i));

        }
    }
}
