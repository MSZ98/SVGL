import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;




public class ArcDraw {

	private BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
	
	private int x0, y0;
	boolean large = false;
	
	
	public ArcDraw() {
		
		JFrame frame = new JFrame("ArcDraw");
		frame.addWindowListener(new WindowAdapter() {public void windowClosing(WindowEvent e) {frame.dispose();System.exit(0);}});
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		JPanel panel = new JPanel() {
			public void paint(Graphics g) {
				g.drawImage(image, 0, 0, null);
			}
		};
		
		frame.setContentPane(panel);
		
		panel.setDoubleBuffered(true);
		
		
		
		
		
		
		
		panel.addMouseMotionListener(new MouseMotionAdapter() {public void mouseDragged(MouseEvent e) {
			
			int xk = e.getX();
			int yk = e.getY();
			clear();
			Graphics g = image.getGraphics();
			g.setColor(Color.black);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			
			// x0, y0, w, h, 0, 0, 0, x, y
			
			int w = 40;
			int h = 80;
			int n = 50;
			boolean sweep = true;
			
			
			
			
			// DRAWING LINES
			ArcPoints ap = getArcPoints(x0, y0, w, h, 0, large, sweep, xk, yk, n);
			
			if(ap != null) {
			
				for(int i = 0;i < ap.x.length - 1;i++)
					g.drawLine((int)ap.x[i], (int)ap.y[i], (int)ap.x[i + 1], (int)ap.y[i + 1]);
			
			}
			
			
			
			
			
			frame.repaint();
		}});
		
		
		
		panel.addMouseListener(new MouseAdapter() {public void mousePressed(MouseEvent e) {
			if(e.getButton() == 1) {
				x0 = e.getX();
				y0 = e.getY();
			}
			else if(e.getButton() == 3) {
				large = !large;
			}
		}});
		
		
	}
	
	
	public class ArcPoints {
		public double[] x, y;
	}
	
	/* x1, y1 - first endpoint od an elliptical arc
	 * radius x, radius y of an ellipse
	 * phi - x axis rotation in degrees
	 * large, sweep - flags
	 * x2, y2 - second endpoint of an ellipse
	 * */
	public ArcPoints getArcPoints(double x1, double y1, double rx, double ry, double phi, boolean large, boolean sweep, double x2, double y2, int points) { 

		// An ellipse parametric equation for getting the ellipse points is:
		// x = cx + cos(phi) * rx * cos(t) - sin(phi) * ry * sin(t)
		// y = cy + cos(phi) * ry * sin(t) + sin(phi) * rx * cos(t)
		
		ArcPoints arcPoints = new ArcPoints();
		
		phi = Math.toRadians(phi);
		
		double sinPhi = Math.sin(phi);
		double cosPhi = Math.cos(phi);

		// x prime, y prime, commonly used in calculations below
		double xp = cosPhi * (x1 - x2) / 2 + sinPhi * (y1 - y2) / 2;
		double yp = -sinPhi * (x1 - x2) / 2 + cosPhi * (y1 - y2) / 2;
		
		// radius square, x prime square, it will be used frequently below
		double rx2 = rx * rx;
		double ry2 = ry * ry;
		double xp2 = xp * xp;
		double yp2 = yp * yp;
		
		// now it's time to check if radius of ellipse isn't too large, it's here, because we need x prime and y prime squares
		// if T is greater, than 0, radius shoud be multiplied by sqrt(T)
		double T = xp2 / rx2 + yp2 / ry2; 
		if(T > 1) {
			rx *= Math.sqrt(T);
			ry *= Math.sqrt(T);
			rx2 = rx * rx;
			ry2 = ry * ry;
		}
		
		// center point prime, used to calculate cender point and angles of arc drawing
		double cxp = (large != sweep ? 1 : -1) * Math.sqrt(Math.abs(rx2 * ry2 - rx2 * yp2 - ry2 * xp2) / (rx2 * yp2 + ry2 * xp2)) * rx * yp / ry;
		double cyp = (large != sweep ? 1 : -1) * Math.sqrt(Math.abs(rx2 * ry2 - rx2 * yp2 - ry2 * xp2) / (rx2 * yp2 + ry2 * xp2)) * -ry * xp / rx;
		
		// center point x, y, used to draw ellipse.
		double cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2;
		double cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2;
	
		double ux, uy, vx, vy, a, b;
		
		ux = 1;
		uy = 0;
		vx = (xp - cxp) / rx;
		vy = (yp - cyp) / ry;
		a = Math.atan2(uy, ux);
        b = Math.atan2(vy, vx);
        
		double t1 = b >= a ? b - a : 2 * Math.PI - (a - b);
		
		ux = vx;
		uy = vy;
		vx = (-xp - cxp) / rx;
		vy = (-yp - cyp) / ry;
		a = Math.atan2(uy, ux);
        b = Math.atan2(vy, vx);
		
		double dt = b >= a ? b - a : 2 * Math.PI - (a - b);

		if(sweep) dt += dt < 0 ? 2 * Math.PI : 0;
		else dt -= dt > 0 ? 2 * Math.PI : 0;
		
		// calculating points
		arcPoints.x = new double[points];
		arcPoints.y = new double[points];
		dt /= points;
		for(int i = 0;i < points - 1;i++) {
			arcPoints.x[i] = cx + Math.cos(phi) * rx * Math.cos(t1 + i * dt) - Math.sin(phi) * ry * Math.sin(t1 + i * dt);
			arcPoints.y[i] = cy + Math.cos(phi) * ry * Math.sin(t1 + i * dt) + Math.sin(phi) * rx * Math.cos(t1 + i * dt);
		}
		arcPoints.x[arcPoints.x.length - 1] = x2;
		arcPoints.y[arcPoints.y.length - 1] = y2;
		
		return arcPoints;
	}
	
	
	// draws huge rectangle, that makes JPanel clear
	public void clear() {
		Graphics g = image.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
	}
	
	public static void main(String[] args) {
		new ArcDraw();
	}

}
