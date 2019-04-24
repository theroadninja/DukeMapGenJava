package trn.duke;

import java.util.Arrays;
import java.util.List;

public class TextureFontManager {

    private final List<TextureFont> fonts;
    private final java.util.Map<Integer, TextureFont> textureToFont = new java.util.TreeMap<>();

    public TextureFontManager(TextureFont... fonts){
        this.fonts = Arrays.asList(fonts);
        for(TextureFont font: this.fonts){
            for(int texture: font.textures){
                textureToFont.put(texture, font);
            }
        }
    }

    public TextureFont get(int texture){
        return textureToFont.get(texture);
    }

    /**
     * @param texture the picnum of the texture
     * @return true if the texture is a string character, an alphanumeric or symbol
     */
    public boolean isFontTexture(int texture){
        return textureToFont.containsKey(texture);
    }
}
