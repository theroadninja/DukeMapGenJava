package trn.prefab;

import org.junit.Assert;
import org.junit.Test;
import trn.LineSegmentXY;
import trn.PointXY;
import trn.Wall;
import trn.WallView;

import java.util.ArrayList;
import java.util.List;

public class MultiSectorConnectorJavaTests {
    // def testWall(wallId: Int, p0: PointXY, p1: PointXY, otherWall: Int = -1): WallView = {
    //     val w = new Wall(p0.x, p0.y)
    //     w.setOtherSide(otherWall, -1)
    //     w.setLotag(PrefabUtils.MarkerSpriteLoTags.MULTI_SECTOR)
    //     new WallView(w, wallId, new LineSegmentXY(p0, p1))
    // }

    private WallView testWall(int wallId, PointXY p0, PointXY p1, int otherWall){
        Wall w = new Wall(p0.x, p0.y);
        w.setOtherSide(otherWall, -1);
        w.setLotag(PrefabUtils.MarkerSpriteLoTags.MULTI_SECTOR);
        return new WallView(w, wallId, new LineSegmentXY(p0, p1), -1, -1);
    }

    private WallView testWall(int wallId, PointXY p0, PointXY p1){
        return testWall(wallId, p0, p1, -1);
    }

    private PointXY p(int x, int y){
        return new PointXY(x, y);
    }

    @Test
    public void testRelativeConnPoints(){
        List<WallView> walls = new ArrayList<WallView>(){{
            add(testWall(1, p(32, 16), p(64, 17)));
            add(testWall(2, p(64, 17), p(96, 18)));
            add(testWall(2, p(96, 18), p(128, 19)));
        }};
        PointXY anchor = p(32, 16);
        List<PointXY> results = MultiSectorConnector.getRelativeConnPoints(walls, anchor.withZ(0));
        Assert.assertEquals(4, results.size());
        Assert.assertEquals(p(0, 0), results.get(0));
        Assert.assertEquals(p(32, 1), results.get(1));
        Assert.assertEquals(p(64, 2), results.get(2));
        Assert.assertEquals(p(96, 3), results.get(3));
    }
}
