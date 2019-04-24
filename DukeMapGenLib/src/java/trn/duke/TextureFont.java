package trn.duke;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Basically a fancy map from string characters to Texture ID.
 */
public class TextureFont {

    private final java.util.Map<String, Integer> charMap;
    final Set<Integer> textures;

    private TextureFont(java.util.Map<String, Integer> charMap){
        this.charMap = charMap;
        this.textures = new TreeSet<>();
        textures.addAll(charMap.values());
    }

    public TextureFont(int startTexture, String characters){
        this(fromString(startTexture, characters));
    }

    public boolean contains(int textureId){
        return textures.contains(textureId);
    }

    public TextureFont addedTo(TextureFont other){
        TreeMap<String, Integer> result = new TreeMap<>();
        result.putAll(this.charMap);
        result.putAll(other.charMap);
        return new TextureFont(result);
    }

    public int textureFor(String s){
        if(s == null || s.length() != 1) throw new IllegalArgumentException();
        if(charMap.containsKey(s)){
            return charMap.get(s);
        }else{
            throw new NoSuchElementException("No font texture for character: " + s);
        }
    }

    private static TreeMap<String, Integer> fromString(int startTexture, String characters){
        TreeMap<String, Integer> result = new TreeMap<>();
        if(startTexture < 0) throw new IllegalArgumentException();
        if(characters == null || characters.length() < 1) throw new IllegalArgumentException();
        for(int i = 0; i < characters.length(); ++i){
            result.put(characters.substring(i, i + 1), startTexture + i);
        }
        return result;
    }
}
