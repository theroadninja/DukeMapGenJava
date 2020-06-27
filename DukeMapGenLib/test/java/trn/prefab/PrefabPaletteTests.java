package trn.prefab;

import org.junit.Assert;
import org.junit.Test;
import trn.JavaTestUtils;
import trn.Map;
import trn.duke.TextureList;

import java.util.List;

public class PrefabPaletteTests {

    private int countSprites(Map map, int tex){
        int sum = 0;
        for(int i = 0; i < map.getSpriteCount(); ++i){
            if(map.getSprite(i).getTex() == tex){
                sum += 1;
            }
        }
        return sum;
    }

    @Test
    public void testParseSectorGroups() throws Exception {
        Map map = JavaTestUtils.readTestMap(JavaTestUtils.PREFAB_TEST);
        PrefabPalette palette = PrefabPalette.fromMap(map);
        Assert.assertEquals(4, palette.numberedSectorGroupCount());

        Map sg2 = palette.getSG(2).getMap();
        Assert.assertEquals(0, palette.getTeleChildren(2).size());
        Assert.assertEquals(2, sg2.getSectorCount());
        Assert.assertEquals(2, countSprites(sg2, TextureList.Items.HANDGUN));
        Assert.assertEquals(0, countSprites(sg2, TextureList.Items.SHOTGUN));
        Assert.assertEquals(0, countSprites(sg2, TextureList.Items.CHAINGUN));
        Assert.assertEquals(0, countSprites(sg2, TextureList.Items.RPG));

        Map sg3 = palette.getSG(3).getMap();
        Assert.assertEquals(0, palette.getTeleChildren(3).size());
        Assert.assertEquals(3, sg3.getSectorCount());
        Assert.assertEquals(0, countSprites(sg3, TextureList.Items.HANDGUN));
        Assert.assertEquals(3, countSprites(sg3, TextureList.Items.SHOTGUN));
        Assert.assertEquals(0, countSprites(sg3, TextureList.Items.CHAINGUN));
        Assert.assertEquals(0, countSprites(sg3, TextureList.Items.RPG));

        Map sg4 = palette.getSG(4).getMap();
        Assert.assertEquals(0, palette.getTeleChildren(4).size());
        Assert.assertEquals(4, sg4.getSectorCount());
        Assert.assertEquals(0, countSprites(sg4, TextureList.Items.HANDGUN));
        Assert.assertEquals(0, countSprites(sg4, TextureList.Items.SHOTGUN));
        Assert.assertEquals(4, countSprites(sg4, TextureList.Items.CHAINGUN));
        Assert.assertEquals(0, countSprites(sg4, TextureList.Items.RPG));

        Map sg5 = palette.getSG(5).getMap();
        Assert.assertEquals(3, sg5.getSectorCount()); // the teleport child is not automatically joined
        Assert.assertEquals(1, palette.getTeleChildren(5).size());
        Assert.assertEquals(0, countSprites(sg5, TextureList.Items.HANDGUN));
        Assert.assertEquals(0, countSprites(sg5, TextureList.Items.SHOTGUN));
        Assert.assertEquals(0, countSprites(sg5, TextureList.Items.CHAINGUN));
        Assert.assertEquals(3, countSprites(sg5, TextureList.Items.RPG));

        List<SectorGroup> sg5tele = palette.getTeleChildren(5);
        Assert.assertEquals(1, sg5tele.size());
        Assert.assertEquals(2, sg5tele.get(0).getMap().getSectorCount());
        Assert.assertEquals(2, countSprites(sg5tele.get(0).getMap(), TextureList.Items.RPG));
    }
}
