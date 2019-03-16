package trn;

import org.junit.Assert;
import org.junit.Test;
import scala.Tuple2;
import trn.prefab.BoundingBox;
import trn.prefab.SectorGroup;

public class SectorGroupTests {
    short wallTex = 191;

    @Test
    public void testBoundingBox() throws Exception {
        Wall w4 = new Wall(0, -100, wallTex); //same position as wall 0
        Wall w5 = new Wall(1024, -100, wallTex);
        Wall w6 = new Wall(1025, 512, wallTex);
        Wall w7 = new Wall(0, 512, wallTex);

        Map map = Map.createNew();
        map.addLoop(w4, w5, w6, w7);
        SectorGroup sg = new SectorGroup(map);
        Assert.assertEquals(612, sg.bbHeight());
        Assert.assertEquals(1025, sg.bbWidth());

        Tuple2<Object, Object> bbox = sg.bbTopLeft();
        Assert.assertEquals(0, bbox._1());
        Assert.assertEquals(-100, bbox._2());

        BoundingBox bb = sg.boundingBox();
        Assert.assertEquals(0, bb.xMin());
        Assert.assertEquals(-100, bb.yMin());
        Assert.assertEquals(1025, bb.xMax());
        Assert.assertEquals(512, bb.yMax());
    }

    @Test
    public void testTriangleBoundingBox() throws Exception {
        Wall w4 = new Wall(2, 42, wallTex); //same position as wall 0
        Wall w5 = new Wall(1024, 3, wallTex);
        Wall w6 = new Wall(512, 210, wallTex);
        Map map = Map.createNew();
        map.addLoop(w4, w5, w6);
        SectorGroup sg = new SectorGroup(map);
        Assert.assertEquals(210-3, sg.bbHeight());
        Assert.assertEquals(1024-2, sg.bbWidth());

        Tuple2<Object, Object> bbox = sg.bbTopLeft();
        Assert.assertEquals(2, bbox._1());
        Assert.assertEquals(3, bbox._2());

        BoundingBox bb = sg.boundingBox();
        Assert.assertEquals(2, bb.xMin());
        Assert.assertEquals(3, bb.yMin());
        Assert.assertEquals(1024, bb.xMax());
        Assert.assertEquals(210, bb.yMax());
        Assert.assertEquals(bb.xMin(), bbox._1());
        Assert.assertEquals(bb.yMin(), bbox._2());
        Assert.assertEquals(sg.bbWidth(), bb.w());
        Assert.assertEquals(sg.bbHeight(), bb.h());
    }
}
