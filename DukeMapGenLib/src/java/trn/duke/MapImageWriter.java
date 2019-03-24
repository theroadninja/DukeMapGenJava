package trn.duke;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import trn.Map;
import trn.PointXY;
import trn.Sprite;
import trn.Wall;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


/**
 * A special graphics object with some public methods that operate in the coordinate system of the map.
 *
 */
final class MapGraphics {
    static final Color BACKGROUND = new Color(240, 240, 240);

    final Color wallColor = new Color(50, 50, 50);
    final Color vertexColor = Color.GREEN;
    final Color spriteColor = new Color(200, 20, 200);

    private final java.awt.Graphics g;

    // we do the transforms manually because we operate in two coordinate systems:
    // 1. the coordinate system of the actual map (which must be scale to the image)
    // 2. the coordinate system of the image, which is used for vertex markers
    private final AffineTransform transform;

    private final int width;
    private final int height;

    public MapGraphics(BufferedImage image, AffineTransform transform){
        this.g = image.getGraphics();
        this.transform = transform;
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.g.setColor(Color.black);
        // FYI - if you want to alter the graphics transform:
        // Graphics2D g2 = ((Graphics2D)g);//.getTransform().concatenate(at);
        // AffineTransform t = g2.getTransform();
        // t.concatenate(at);
        // g2.setTransform(t);
        // g.drawRect(bb.getLeft().x, bb.getLeft().y, 50, 50);
    }

    private PointXY applyTransform(PointXY p){
        Point2D r = transform.transform(new Point2D.Float(p.x, p.y), null);
        return new PointXY((int)r.getX(), (int)r.getY());
    }

    private void drawVertex(int x, int y){ // NOTE: this is in image coordinates
        g.drawRect(x - 2, y - 2, 4, 4);
    }
    private void drawVertex(PointXY p){ // NOTE: this uses the image coordinates
        drawVertex(p.x, p.y);
    }


    public void fill(Color c){
        Color cc = g.getColor();
        g.setColor(c);
        g.fillRect(0, 0, width, height);
        g.setColor(cc);
    }

    public void drawSprite(Sprite s){
        g.setColor(spriteColor);
        PointXY p = applyTransform(s.getLocation().asPointXY());
        drawVertex(p);
    }

    public void drawWall(Wall w, PointXY point2){
        PointXY p1 = applyTransform(w.getLocation());
        PointXY p2 = applyTransform(point2);
        g.setColor(wallColor);
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
        g.setColor(vertexColor);
        drawVertex(p1);
        drawVertex(p2);
    }
}

public class MapImageWriter {

    private static Integer min(Integer i, Integer j){
        if(i == null) return j;
        return (j == null || i < j) ? i : j;
    }
    private static Integer max(Integer i, Integer j){
        if(i == null) return j;
        return (j == null || i > j) ? i : j;
    }

    /** Returns a tuple of (TopLeft, BottomRight) which represent
     * the bounds of all walls, sprites and other objects in the map.
     * @return
     */
    static Pair<PointXY, PointXY> getBounds(Map map){
        if(map.getWallCount() + map.getSpriteCount() < 1){
            return new ImmutablePair<>(new PointXY(0, 0), new PointXY(0, 0));
        }
        Integer xMin = null;
        Integer yMin = null;
        Integer xMax = null;
        Integer yMax = null;

        for(int i = 0; i < map.getWallCount(); ++i){
            Wall w = map.getWall(i);
            xMin = min(xMin, w.getX());
            yMin = min(yMin, w.getY());
            xMax = max(xMax, w.getX());
            yMax = max(yMax, w.getY());
        }

        for(int i = 0; i < map.getSpriteCount(); ++i){
            Sprite s = map.getSprite(i);
            xMin = min(xMin, s.getLocation().x);
            yMin = min(yMin, s.getLocation().y);
            xMax = max(xMax, s.getLocation().x);
            yMax = max(yMax, s.getLocation().y);
        }

        return new ImmutablePair<>(new PointXY(xMin, yMin), new PointXY(xMax, yMax));
    }

    public static BufferedImage toImage(Map map){
        Pair<PointXY, PointXY> bb = getBounds(map);

        int width = bb.getRight().x - bb.getLeft().x;
        int height = bb.getRight().y - bb.getLeft().y;
        if(width < 1 || height < 1) throw new RuntimeException("bad coding");

        // map units have ranges too large to fit into a single png
        final int MAX_WIDTH = 1280;
        final int MAX_HEIGHT = 1280;

        double scaleX = MAX_WIDTH / (double)Math.max(MAX_WIDTH, width);
        double scaleY = MAX_HEIGHT / (double)Math.max(MAX_HEIGHT, height);
        scaleX = scaleY = Math.min(scaleX, scaleY);

        BufferedImage image = new BufferedImage(MAX_WIDTH, MAX_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        AffineTransform at = new AffineTransform();
        at.scale(scaleX, scaleY);
        at.translate(- bb.getLeft().x, - bb.getLeft().y);

        MapGraphics mg = new MapGraphics(image, at);
        mg.fill(MapGraphics.BACKGROUND);
        for(int i = 0; i < map.getWallCount(); ++i){
            Wall w = map.getWall(i);
            mg.drawWall(w, map.getWall(w.getPoint2Id()).getLocation());
        }
        for(int i = 0; i < map.getSpriteCount(); ++i){
            mg.drawSprite(map.getSprite(i));
        }
        return image;
    }
}
