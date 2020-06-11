package trn.prefab;

import org.junit.Assert;
import org.junit.Test;
import scala.collection.JavaConverters;
import trn.JavaTestUtils;
import trn.PointXYZ;

import java.util.List;

public class MultiWallConnectorTests {

    @Test
    public void testMultiWall() throws Exception {
        PrefabPalette palette = PrefabPalette.fromMap(JavaTestUtils.readTestMap(JavaTestUtils.MULTI_WALL_CONN_MAP));

        SectorGroup sg1 = palette.getSG(1);
        List<RedwallConnector> conns1 = JavaConverters.seqAsJavaList(sg1.allRedwallConnectors());
        Assert.assertEquals(1, conns1.size());

        Assert.assertEquals(new PointXYZ(-60416, -63488, 8192), conns1.get(0).getAnchorPoint());
        Assert.assertFalse(conns1.get(0).isLinked(sg1.getMap())); // normally called after its been pasted.
        Assert.assertEquals(2560, conns1.get(0).totalManhattanLength());

        SectorGroup sg2 = palette.getSG(2);
        List<RedwallConnector> conns2 = JavaConverters.seqAsJavaList(sg2.allRedwallConnectors());

        // NOTE: this test is a little unusual because normally we'd be comparing a conn on an sg to one on
        // a psg, however this math should still work between two sector groups.
        // The 1024,0,0 comes from these groups' actual position in the input map.
        PointXYZ result = conns1.get(0).getTransformTo(conns2.get(0));
        Assert.assertEquals(1024, result.x);
        Assert.assertEquals(0, result.y);
        Assert.assertEquals(0, result.z);

        MultiWallConnector c1 = (MultiWallConnector)conns1.get(0);
        MultiWallConnector c2 = (MultiWallConnector)conns2.get(0);
        Assert.assertTrue(c1.isMatch(c2));
        Assert.assertTrue(c1.canLink(c2, null));

        // TODO - test all relative conn points

    }

    private void testPasteAndLinking(int groupId1, int groupId2) throws Exception {
        PrefabPalette palette = PrefabPalette.fromMap(JavaTestUtils.readTestMap(JavaTestUtils.MULTI_WALL_CONN_MAP));

        MapWriter writer = MapWriter.apply();
        PastedSectorGroup psg1 = writer.pasteSectorGroupAt(palette.getSG(groupId1), PointXYZ.ZERO, false);
        Assert.assertFalse(psg1.redwallConnectors().head().isLinked(writer.getMap()));

        RedwallConnector extantConn = psg1.redwallConnectors().head();
        SectorGroup sg2 = palette.getSG(groupId2);
        PastedSectorGroup psg2 = writer.pasteAndLink(extantConn, sg2, sg2.allRedwallConnectors().head());

        Assert.assertTrue(psg1.redwallConnectors().head().isLinked(writer.getMap()));
        Assert.assertTrue(psg2.redwallConnectors().head().isLinked(writer.getMap()));
    }

    @Test
    public void testPasting() throws Exception {
        testPasteAndLinking(1, 2);
        testPasteAndLinking(3, 4); // groups 3 and 4 have 1 wall at an angle, which is parsed as a "multi wall"
    }

}
