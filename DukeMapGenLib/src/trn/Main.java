package trn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class Main {

	
	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	/** TODO:  remove when I have some real unit tests */
	public static final String HELLO = "hello world";
	
	public static void main(String[] args){
		
		String fname =  "ONEROOM.MAP";
		//String fname = "TWOROOMS.MAP";
		
		String filepath = System.getProperty("user.dir") + File.separator + "testdata" + File.separator + fname;
		
		
		File f = new File(filepath);
		if(f.exists() && f.isFile()){
			
			try {
				byte[] mapFile = IOUtils.toByteArray(new FileInputStream(f));
				parseOnTheFly(mapFile);
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		}else{
			System.err.println(String.format("%s is not a valid file", filepath));
			System.exit(1);
		}
		
	}
	
	/**
	 * quick and dirty method to print shit while we learn how to read the file
	 * 
	 * TODO:  develop data types, make a nice fancy parser.
	 * @param mapFile
	 * @throws IOException 
	 */
	public static void parseOnTheFly(byte[] mapFile) throws IOException{
		
		ByteArrayInputStream bs = new ByteArrayInputStream(mapFile);
		
		Map map = Map.readMap(bs);
		
		map.print();
		//ByteArrayInputStream bs = new ByteArrayInputStream(mapFile, 1, mapFile.length - 1);
		
		
		
	}
}
