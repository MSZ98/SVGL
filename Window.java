import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;


// THIS IS AN EXAMPLE USE OF THE SVGL


public class Window extends JFrame {


	private static final long serialVersionUID = 1L;
	
	private BufferedImage image;
	private JPanel content;
	

	public Window() {
		super("Drawing window");
		getContentPane().setBackground(Color.white);
		setSize(500, 500);
		setLocationRelativeTo(null);
		setVisible(true);
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		content = new JPanel() {
			private static final long serialVersionUID = 1L;
			public void paint(Graphics g) {
				super.paint(g);
				g.drawImage(image, 0, 0, null);
			}
		};
		content.setDoubleBuffered(true);
		setContentPane(content);
		
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				BufferedImage i = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
				if(image != null) i.getGraphics().drawImage(image, 0, 0, null);
				image = i;
				getContentPane().repaint();
			}
		});
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
				System.exit(0);
			}
		});
	}
	
	
	public void drawLine(int x1, int y1, int x2, int y2) {
		Graphics2D g = (Graphics2D)image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(x1, y1, x2, y2);
		content.repaint();
	}
	

	public void clear() {
		Graphics2D g = (Graphics2D)image.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		getContentPane().repaint();
	}
	
	
}
