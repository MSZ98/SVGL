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
			
			int w = 50;
			int h = 100;
			int n = 10;
			
			
			ArcPoints ap = getArcPoints(x0, y0, w, h, 0, large, false, xk, yk, 50);
			
			
			if(ap != null) {
			
				for(int i = 0;i < ap.x.length - 1;i++) {
					g.drawLine(ap.x[i], ap.y[i], ap.x[i + 1], ap.y[i + 1]);
				}
				//g.drawLine(ap.x[0], ap.y[0], ap.x[9], ap.y[9]);
				g.drawLine(x0, y0, xk, yk);
			
			
				//System.out.println(points[0].length);
			
			}
			
			
			//g.drawLine(x0, y0, xk, yk);
			
			
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
	
	
	
	
	public void clear() {
		Graphics g = image.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());
	}
	
	
	
	
	public class ArcPoints {
		public int[] x, y;
	}
	
	/* x0, y0
	 * width, height
	 * theta
	 * large, sweep flag
	 * xk, yk
	 * */
	public ArcPoints getArcPoints(double x0, double y0, double w, double h, double phi, boolean large, boolean sweep, double xk, double yk, int points) { 
		
		ArcPoints arcPoints = new ArcPoints();
		
		phi = Math.toRadians(phi);
		
		double sinPhi = Math.sin(phi);
		double cosPhi = Math.cos(phi);	
		if(sweep) {
			sinPhi = Math.cos(phi);
			cosPhi = Math.sin(phi);
		}
		double sinPhi2 = sinPhi * sinPhi;
		double cosPhi2 = cosPhi * cosPhi;
		double c = (x0 - xk) / (y0 - yk);
		double b = Math.atan(h / w * (c * cosPhi - sinPhi) / (c * sinPhi - cosPhi));
		double a = Math.asin(0.5 * (x0 - xk) / (h * sinPhi * Math.cos(b) - w * cosPhi * Math.sin(b)));
		double t1 = a + b;
		double t2 = b - a;
		double xm = (x0 * cosPhi - yk * sinPhi - w * (Math.cos(t1) * cosPhi2 - Math.cos(t2) * sinPhi2) - h * sinPhi * cosPhi * (Math.sin(t1) - Math.sin(t2))) / (cosPhi2 - sinPhi2);
		double ym = (x0 * sinPhi - yk * cosPhi - h * (Math.sin(t1) * sinPhi2 - Math.sin(t2) * cosPhi2) - w * sinPhi * cosPhi * (Math.cos(t1) - Math.cos(t2))) / (sinPhi2 - cosPhi2);
		
		arcPoints.x = new int[points];
		arcPoints.y = new int[points];
		
		double t0 = Math.min(t1, t2);
		double tk = Math.max(t1, t2);
		
		t0 += large ? 2 * Math.PI : 0;
		
		for(int i = 0;i < points - 1;i++) {
			double t = t0 + i * (tk - t0) / (points - 1);
			arcPoints.x[i] = (int)(cosPhi * (xm + w * Math.cos(t)) + sinPhi * (ym + h * Math.sin(t)));
			arcPoints.y[i] = (int)(sinPhi * (xm + w * Math.cos(t)) + cosPhi * (ym + h * Math.sin(t)));
		}
		
		arcPoints.x[arcPoints.x.length - 1] = (int)(cosPhi * (xm + w * Math.cos(tk)) + sinPhi * (ym + h * Math.sin(tk)));
		arcPoints.y[arcPoints.y.length - 1] = (int)(sinPhi * (xm + w * Math.cos(tk)) + cosPhi * (ym + h * Math.sin(tk)));
		
		return arcPoints;
	}
	

	
	
	
	
	public static void main(String[] args) {
		new ArcDraw();
	}

}
