package gate.resources.img.svg;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class has been automatically generated using <a
 * href="http://englishjavadrinker.blogspot.com/search/label/SVGRoundTrip">SVGRoundTrip</a>.
 */
@SuppressWarnings("unused")
public class GATEIcon implements
		javax.swing.Icon {
	/**
	 * Paints the transcoded SVG image on the specified graphics context. You
	 * can install a custom transformation on the graphics context to scale the
	 * image.
	 * 
	 * @param g
	 *            Graphics context.
	 */
	public static void paint(Graphics2D g) {
        Shape shape = null;
        Paint paint = null;
        Stroke stroke = null;
        Area clip = null;
         
        float origAlpha = 1.0f;
        Composite origComposite = g.getComposite();
        if (origComposite instanceof AlphaComposite) {
            AlphaComposite origAlphaComposite = 
                (AlphaComposite)origComposite;
            if (origAlphaComposite.getRule() == AlphaComposite.SRC_OVER) {
                origAlpha = origAlphaComposite.getAlpha();
            }
        }
        
	    Shape clip_ = g.getClip();
AffineTransform defaultTransform_ = g.getTransform();
//  is CompositeGraphicsNode
float alpha__0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0 = g.getClip();
AffineTransform defaultTransform__0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
clip = new Area(g.getClip());
clip.intersect(new Area(new Rectangle2D.Double(0.0,0.0,60.0,60.0)));
g.setClip(clip);
// _0 is CompositeGraphicsNode
float alpha__0_0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_0 = g.getClip();
AffineTransform defaultTransform__0_0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, -116.65451049804688f, -204.4719696044922f));
// _0_0 is CompositeGraphicsNode
origAlpha = alpha__0_0;
g.setTransform(defaultTransform__0_0);
g.setClip(clip__0_0);
float alpha__0_1 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1 = g.getClip();
AffineTransform defaultTransform__0_1 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, -116.65451049804688f, -204.4719696044922f));
// _0_1 is CompositeGraphicsNode
float alpha__0_1_0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1_0 = g.getClip();
AffineTransform defaultTransform__0_1_0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_0 is ShapeNode
origAlpha = alpha__0_1_0;
g.setTransform(defaultTransform__0_1_0);
g.setClip(clip__0_1_0);
float alpha__0_1_1 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1_1 = g.getClip();
AffineTransform defaultTransform__0_1_1 = g.getTransform();
g.transform(new AffineTransform(1.0869563817977905f, 0.0f, 0.0f, 1.0869563817977905f, -6.230812072753906f, -26.91058349609375f));
// _0_1_1 is CompositeGraphicsNode
float alpha__0_1_1_0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1_1_0 = g.getClip();
AffineTransform defaultTransform__0_1_1_0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_1_0 is ShapeNode
paint = new Color(255, 255, 255, 255);
shape = new GeneralPath();
((GeneralPath)shape).moveTo(162.71873, 240.47195);
((GeneralPath)shape).curveTo(162.87343, 267.3946, 118.58987, 262.11823, 118.58987, 262.11823);
((GeneralPath)shape).lineTo(118.58987, 218.82571);
((GeneralPath)shape).curveTo(118.58987, 218.82571, 162.56404, 213.54935, 162.71873, 240.47197);
((GeneralPath)shape).closePath();
g.setPaint(paint);
g.fill(shape);
paint = new Color(0, 155, 0, 255);
stroke = new BasicStroke(1.8707279f,0,0,4.0f,null,0.0f);
shape = new GeneralPath();
((GeneralPath)shape).moveTo(162.71873, 240.47195);
((GeneralPath)shape).curveTo(162.87343, 267.3946, 118.58987, 262.11823, 118.58987, 262.11823);
((GeneralPath)shape).lineTo(118.58987, 218.82571);
((GeneralPath)shape).curveTo(118.58987, 218.82571, 162.56404, 213.54935, 162.71873, 240.47197);
((GeneralPath)shape).closePath();
g.setPaint(paint);
g.setStroke(stroke);
g.draw(shape);
origAlpha = alpha__0_1_1_0;
g.setTransform(defaultTransform__0_1_1_0);
g.setClip(clip__0_1_1_0);
float alpha__0_1_1_1 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1_1_1 = g.getClip();
AffineTransform defaultTransform__0_1_1_1 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_1_1 is CompositeGraphicsNode
float alpha__0_1_1_1_0 = origAlpha;
origAlpha = origAlpha * 1.0f;
g.setComposite(AlphaComposite.getInstance(3, origAlpha));
Shape clip__0_1_1_1_0 = g.getClip();
AffineTransform defaultTransform__0_1_1_1_0 = g.getTransform();
g.transform(new AffineTransform(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f));
// _0_1_1_1_0 is ShapeNode
paint = new Color(255, 0, 0, 255);
shape = new GeneralPath();
((GeneralPath)shape).moveTo(153.2775, 253.50543);
((GeneralPath)shape).lineTo(135.45905, 253.50543);
((GeneralPath)shape).curveTo(131.38077, 253.50543, 128.00046, 252.15057, 125.31812, 249.44086);
((GeneralPath)shape).curveTo(122.66314, 246.73114, 121.335655, 243.3098, 121.33566, 239.17677);
((GeneralPath)shape).curveTo(121.335655, 235.07117, 122.64946, 231.75928, 125.27706, 229.24115);
((GeneralPath)shape).curveTo(127.93203, 226.72305, 131.32602, 225.46399, 135.45905, 225.46397);
((GeneralPath)shape).lineTo(151.3068, 225.46397);
((GeneralPath)shape).lineTo(151.3068, 230.34967);
((GeneralPath)shape).lineTo(135.45905, 230.34967);
((GeneralPath)shape).curveTo(132.77667, 230.34969, 130.55965, 231.21187, 128.80792, 232.93622);
((GeneralPath)shape).curveTo(127.08355, 234.6606, 126.22136, 236.87764, 126.221375, 239.58734);
((GeneralPath)shape).curveTo(126.22137, 242.2697, 127.08356, 244.4457, 128.80792, 246.1153);
((GeneralPath)shape).curveTo(130.55965, 247.78493, 132.77669, 248.61974, 135.45905, 248.61974);
((GeneralPath)shape).lineTo(148.39178, 248.61974);
((GeneralPath)shape).lineTo(148.39178, 242.50233);
((GeneralPath)shape).lineTo(135.08954, 242.50233);
((GeneralPath)shape).lineTo(135.08954, 238.02719);
((GeneralPath)shape).lineTo(153.2775, 238.02719);
((GeneralPath)shape).lineTo(153.2775, 253.50543);
g.setPaint(paint);
g.fill(shape);
paint = new Color(140, 0, 0, 255);
stroke = new BasicStroke(0.64150524f,0,0,4.0f,null,0.0f);
shape = new GeneralPath();
((GeneralPath)shape).moveTo(153.2775, 253.50543);
((GeneralPath)shape).lineTo(135.45905, 253.50543);
((GeneralPath)shape).curveTo(131.38077, 253.50543, 128.00046, 252.15057, 125.31812, 249.44086);
((GeneralPath)shape).curveTo(122.66314, 246.73114, 121.335655, 243.3098, 121.33566, 239.17677);
((GeneralPath)shape).curveTo(121.335655, 235.07117, 122.64946, 231.75928, 125.27706, 229.24115);
((GeneralPath)shape).curveTo(127.93203, 226.72305, 131.32602, 225.46399, 135.45905, 225.46397);
((GeneralPath)shape).lineTo(151.3068, 225.46397);
((GeneralPath)shape).lineTo(151.3068, 230.34967);
((GeneralPath)shape).lineTo(135.45905, 230.34967);
((GeneralPath)shape).curveTo(132.77667, 230.34969, 130.55965, 231.21187, 128.80792, 232.93622);
((GeneralPath)shape).curveTo(127.08355, 234.6606, 126.22136, 236.87764, 126.221375, 239.58734);
((GeneralPath)shape).curveTo(126.22137, 242.2697, 127.08356, 244.4457, 128.80792, 246.1153);
((GeneralPath)shape).curveTo(130.55965, 247.78493, 132.77669, 248.61974, 135.45905, 248.61974);
((GeneralPath)shape).lineTo(148.39178, 248.61974);
((GeneralPath)shape).lineTo(148.39178, 242.50233);
((GeneralPath)shape).lineTo(135.08954, 242.50233);
((GeneralPath)shape).lineTo(135.08954, 238.02719);
((GeneralPath)shape).lineTo(153.2775, 238.02719);
((GeneralPath)shape).lineTo(153.2775, 253.50543);
g.setPaint(paint);
g.setStroke(stroke);
g.draw(shape);
origAlpha = alpha__0_1_1_1_0;
g.setTransform(defaultTransform__0_1_1_1_0);
g.setClip(clip__0_1_1_1_0);
origAlpha = alpha__0_1_1_1;
g.setTransform(defaultTransform__0_1_1_1);
g.setClip(clip__0_1_1_1);
origAlpha = alpha__0_1_1;
g.setTransform(defaultTransform__0_1_1);
g.setClip(clip__0_1_1);
origAlpha = alpha__0_1;
g.setTransform(defaultTransform__0_1);
g.setClip(clip__0_1);
origAlpha = alpha__0;
g.setTransform(defaultTransform__0);
g.setClip(clip__0);
g.setTransform(defaultTransform_);
g.setClip(clip_);

	}
	
	public Image getImage() {
		BufferedImage image =
            new BufferedImage(getIconWidth(), getIconHeight(),
                    BufferedImage.TYPE_INT_ARGB);
    	Graphics2D g = image.createGraphics();
    	paintIcon(null, g, 0, 0);
    	g.dispose();
    	return image;
	}

    /**
     * Returns the X of the bounding box of the original SVG image.
     * 
     * @return The X of the bounding box of the original SVG image.
     */
    public static int getOrigX() {
        return 0;
    }

    /**
     * Returns the Y of the bounding box of the original SVG image.
     * 
     * @return The Y of the bounding box of the original SVG image.
     */
    public static int getOrigY() {
        return 0;
    }

	/**
	 * Returns the width of the bounding box of the original SVG image.
	 * 
	 * @return The width of the bounding box of the original SVG image.
	 */
	public static int getOrigWidth() {
		return 60;
	}

	/**
	 * Returns the height of the bounding box of the original SVG image.
	 * 
	 * @return The height of the bounding box of the original SVG image.
	 */
	public static int getOrigHeight() {
		return 60;
	}

	/**
	 * The current width of this resizable icon.
	 */
	int width;

	/**
	 * The current height of this resizable icon.
	 */
	int height;

	/**
	 * Creates a new transcoded SVG image.
	 */
	public GATEIcon() {
        this.width = getOrigWidth();
        this.height = getOrigHeight();
	}
	
	/**
	 * Creates a new transcoded SVG image with the given dimensions.
	 *
	 * @param size the dimensions of the icon
	 */
	public GATEIcon(Dimension size) {
	this.width = size.width;
	this.height = size.width;
	}

	public GATEIcon(int width, int height) {
	this.width = width;
	this.height = height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconHeight()
	 */
    @Override
	public int getIconHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#getIconWidth()
	 */
    @Override
	public int getIconWidth() {
		return width;
	}

	public void setDimension(Dimension newDimension) {
		this.width = newDimension.width;
		this.height = newDimension.height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics,
	 * int, int)
	 */
    @Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.translate(x, y);
						
		Area clip = new Area(new Rectangle(0, 0, this.width, this.height));		
		if (g2d.getClip() != null) clip.intersect(new Area(g2d.getClip()));		
		g2d.setClip(clip);

		double coef1 = (double) this.width / (double) getOrigWidth();
		double coef2 = (double) this.height / (double) getOrigHeight();
		double coef = Math.min(coef1, coef2);
		g2d.scale(coef, coef);
		paint(g2d);
		g2d.dispose();
	}
}

