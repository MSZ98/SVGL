/*
 Copyright (c) 2020 MSZ98

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * A class for parsing SVG files and extracting drawing instructions.
 */
public class SVG {


	private double width, height;
	private ViewBox viewBox;
	private String version;
	private String id;
	private ArrayList<Tag> tagList = new ArrayList<Tag>();
	private Path[] paths = null;
	

	// Huge SVG constructor parses the SVG file and outputs tag list and paths array, which contains very important Command List (list of figures to draw)
	/**
	 * Constructs an SVG object by parsing an SVG file.
	 * 
	 * @param svgFile The SVG file to be parsed.
	 */
	public SVG(File svgFile) {
		
		// Converting svg file to the form of String variable
		String svgString = new String();
		Scanner scanner = null;
		try {scanner = new Scanner(svgFile);} catch(Exception e) {return;}
		while(scanner.hasNextLine()) svgString = svgString.concat(scanner.nextLine());
		
		svgString = unifyEndTags(svgString);
		findTags(svgString);
		
		// Write to svgString variable everything, what is in <svg> tag. I'm doing this because if I didn't, detecting version in whole document gives me xml version instead svg version
		for(Tag tag : tagList) if(tag.getName().equals("svg")) svgString = tag.getContent();
		
		// Parse width and height. If there is no width or height statement in file, exception is handled.
		try {width = findNumbers(findExpression(svgString, "width *= *\"(.*?)\""))[0];} catch(Exception e) {width = 0;}
		try {height = findNumbers(findExpression(svgString, "height *= *\"(.*?)\""))[0];} catch(Exception e) {height = 0;}
		
		// Parse viewBox
		double[] viewBoxSize = findNumbers(findExpression(svgString, "viewBox *= *\"(.*?)\""));
		viewBox = viewBoxSize == null ? null : new ViewBox(viewBoxSize[0], viewBoxSize[1], viewBoxSize[2], viewBoxSize[3]);
		
		// Parse version
		version = findExpression(svgString, "version *= *\"(.*?)\"");
		
		// Parse id
		id = findExpression(svgString, "id *= *\"(.*?)\"");

		// PATHS PARSING
				
		// Creating path tags list
		ArrayList<Tag> pathList = new ArrayList<Tag>();
		for(Tag tag : tagList) if(tag.getName().equals("path")) pathList.add(tag);
		
		// Creating array of Path objects, whose fields will be filled with data extracted from tags
		Path[] paths = new Path[pathList.size()];

		// Parsing type, style and id
		for(int x = 0;x < paths.length;x++) {
			paths[x] = new Path();
			paths[x].type = findExpression(pathList.get(x).getContent(), "type *= *\"(.*?)\"");
			paths[x].style = findExpression(pathList.get(x).getContent(), "style *= *\"(.*?)\"");
			paths[x].id = findExpression(pathList.get(x).getContent(), "id *= *\"(.*?)\"");
		}
		
		// Creating groups list for finding transforms
		ArrayList<Group> groupList = new ArrayList<Group>();
		// Finding group tags
		for(Tag tag : tagList) if(tag.getName().equals("g")) {
			// Finding transform in every group tag
			String transformString = findExpression(tag.getContent(), "transform *= *\" *translate *(.*?) *\"");
			if(transformString == null) transformString = "0, 0";
			double[] transform = findNumbers(transformString);
			groupList.add(new Group(tag, transform[0], transform[1]));
		}
		
		// Parsing transforms
		for(int x = 0;x < paths.length;x++) {
			for(Group group : groupList) if(group.getTag().contains(pathList.get(x))) {
				paths[x].transformX += group.getTransformX();
				paths[x].transformY += group.getTransformY();
			}
		}
		
		// Parsing commands
		Command[] commands = null;
		for(int x = 0;x < paths.length;x++) {
			// Extracting commands in form of string from path string
			String commandsString = findExpression(pathList.get(x).getContent(), "\\Wd *= *\"(.*?)\"");
			
			// Split commandsString into the form of strings containing individual commands. (Variable commandsString contains bunch of all commands of path)
			ArrayList<String> commandList = new ArrayList<String>();
			Pattern pattern = Pattern.compile("[a-df-zA-DF-Z]");
			Matcher matcher = pattern.matcher(commandsString);
			matcher.find();
			int previousLetterIndex = matcher.start();
			while(matcher.find()) {
				commandList.add(commandsString.substring(previousLetterIndex, matcher.start()));
				previousLetterIndex = matcher.start();
			}
			commandList.add(commandsString.substring(previousLetterIndex, commandsString.length()));
			
			// PARSING COMMAND
			// Creating an array of Command objects
			commands = new Command[commandList.size()];
			
			// Do for every command. This loop searches for letters and numbers in particular command strings, then it fills objects of type Command with this data
			for(int y = 0;y < commands.length;y++) {
			
				// Parsing letter
				pattern = Pattern.compile("\\w");
				matcher = pattern.matcher(commandList.get(y));
				matcher.find();
				char letter = matcher.group().charAt(0);
				
				// Parsing points. X'es are detected, when ',' is after number
				matcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?").matcher(commandList.get(y));
				ArrayList<Double> pointsList = new ArrayList<Double>();
				// All, x and y points land in pointsList. Usually x's are odd and y's are even, but it depends on command, so we can't separate them now..
				while(matcher.find()) {
					double number = 0;
					try {
						number = Double.valueOf(matcher.group(0));
						pointsList.add(number);
					} 
					catch(Exception e) {}
				}
				
				// Rewriting data from lists to arrays. If one of list is larger, than other, the empty spaces in smaller array are filled with zeros
				int size = pointsList.size();
				double[] points = new double[size];
				for(int z = 0;z < size;z++) points[z] = pointsList.get(z);
				
				// Writing data into new Objects of type Command
				commands[y] = new Command(letter, points);
			}
		
			// Writing commands data into Path object
			paths[x].commands = commands;
			this.paths = paths;
		}
		
		/*
		// This is test code, that prints every path and its points
		for(Path path : paths) for(Command command : path.getCommands()) {
			System.out.println("\n" + command.getLetter() + ":");
			for(int x = 0;x < command.getPoints().length;x++) {
				System.out.println(command.getPoints()[x] + ", ");
			}
		}
		System.out.println("================================");
		*/
		
	} // This is the end of huge constructor of SVG.
	

	/**
	 * Finds real numbers in the source string and returns them as a double array.
	 * 
	 * @param source The source string containing numeric values
	 * @return A double array containing the extracted real numbers from the source string
	 */
	private double[] findNumbers(String source) {
		if(source == null) return null;
		Pattern pattern = Pattern.compile("([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");
		Matcher matcher = pattern.matcher(source);
		ArrayList<String> occurrences = new ArrayList<String>();
		while(matcher.find()) occurrences.add(matcher.group(1));
		double[] numbers = new double[occurrences.size()];
		for(int x = 0;x < occurrences.size();x++) numbers[x] = Double.valueOf(occurrences.get(x));
		return numbers;
	}
	

	// Uses findTag() method to fill tagList with tags
	/**
	 * Uses the findTag() method to fill the tagList with tags extracted from the source string.
	 * 
	 * @param source The source string containing tags to be extracted
	 */
	private void findTags(String source) {
		Tag tag = null;
		do {
			tag = findTag(source);
			if(tag != null) {
				source = removeTagFromString(source, tag);
				tagList.add(tag);
				findTags(tag.getContent());
			}
		} while(tag != null);
	}
	

	// Finds any tag in source string and removes the tag from it
	/**
	 * Finds the first occurrence of a tag in the source string and removes the tag from it.
	 * 
	 * @param source The source string in which to search for the tag
	 * @return The extracted Tag object containing information about the found tag, or null if no tag is found
	 */
	private Tag findTag(String source) {
		
		// Startup declarations
		String content = null;
		String name = null;
		int startIndex, endIndex;
		
		// Find anything, what is between '<' and '>'
		Pattern pattern = Pattern.compile("<" + "(.*?)" + ">");
		Matcher matcher = pattern.matcher(source);
		
		// If you found something, write it to String content, if not, return null
		if(matcher.find()) content = matcher.group();
		else return null;

		// Detect start and end index of content in source string. Start index is index of '<'. End index is index of character after '>'. source.substring(startIndex, endIndex) gives <content>.
		startIndex = source.indexOf(content);
		endIndex = startIndex + content.length();
		
		// Detect name of tag
		pattern = Pattern.compile("\\w+");
		matcher = pattern.matcher(content);
		if(matcher.find()) name = matcher.group();
		
		// Remove angle brackets from content. Content shouldn't contain his angle brackets <>, because if function findTag() tried to find a tag in it, then it would find whole tag as target
		content = content.substring(1, content.length() - 1);
		
		// Create new object of class Tag and fill it with acquired data 
		Tag tag = new Tag(name, content, startIndex, endIndex);
		
		return tag;
	}
	

	/**
	 * Removes the specified tag content from the source string and returns the modified string.
	 * 
	 * @param source The source string from which to remove the Tag
	 * @param tag The tag to be removed, containing the content to be deleted
	 * @return A new string with the specified tag content removed
	 */
	private String removeTagFromString(String source, Tag tag) {
		return source.replace("<" + tag.getContent() + ">", "");
	}
	
	
	// Returns source string with replaced </...> endtags with /> endTags. It makes finding tags easy and it's done once.
	/**
	 * Replaces all </...> end tags with /> end tags in the source string, simplifying tag identification.
	 * 
	 * This method modifies the source string by replacing the traditional </...> end tags
	 * with self-closing style end tags like />. This replacement simplifies the process of
	 * identifying and handling tags in the source string.
	 * 
	 * @param source The source string in which to replace the end tags
	 * @return A new string with </...> end tags replaced by /> end tags
	 */
	private String unifyEndTags(String source) {
		Pattern pattern = Pattern.compile("</" + "(.*?)" + ">");
		Matcher matcher = pattern.matcher(source);
		return matcher.replaceAll("/>");
	}
	
	
	/**
	 * Finds and returns the expression from the source string based on the provided regular expression.
	 * 
	 * This method searches for a substring within the source string that matches the given regular expression.
	 * If a match is found, the captured expression is returned; otherwise, null is returned.
	 * 
	 * @param source The source string in which to search for the expression
	 * @param regex The regular expression pattern to match the desired expression
	 * @return The captured expression if found, or null if no match is found
	 */
	private String findExpression(String source, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(source);
		return matcher.find() ? matcher.group(1) : null;
	}
	
	
	/**
	 * Represents a group of SVG elements that share a common transformation.
	 */
	public class Group {
		private Tag tag;
		private double transformX, transformY;
		public Group(Tag tag, double transformX, double transformY) {
			this.tag = tag;
			this.transformX = transformX;
			this.transformY = transformY;
		}
		public Tag getTag() {return tag;}
		public double getTransformX() {return transformX;}
		public double getTransformY() {return transformY;}
	}
	
	
	/**
	 * Represents a path in an SVG file, consisting of drawing commands.
	 * 
	 * It's a very important class, storage of figures to be drawn. There is an array of them in the top of SVG class. They're parsed in SVG class constructor.
	 */
	public class Path {
		private String type, style, id;
		private Command[] commands;
		private double transformX, transformY;
		public String getType() {return type;}
		public String getStyle() {return style;}
		public String getId() {return id;}
		public Command[] getCommands() {return commands;}
		public double getTransformX() {return transformX;}
		public double getTransformY() {return transformY;}
	}
	
	
	/**
	 * Represents a drawing command within an SVG path.
	 * 
	 * Path is storage of Commands. Commands have a letter, which is eg. L (line), C (bezier curve) and bunch of x points and y points, so they store figures to be drawn.
	 */
	public class Command {
		private char letter;
		private double[] points;
		public Command(char letter, double[] points) {
			this.letter = letter;
			this.points = points;
		}
		public char getLetter() {return letter;}
		public double[] getPoints() {return points;}
	}

	
	/**
	 * Represents an SVG tag found within the SVG content.
	 * 
	 * Tag is first stage of parsing SVG file. Tag is <... /> statement in file and all higher level things like Paths, Groups and Commands are then parsed from Tags.
	 */
	public class Tag {
		private int startIndex, endIndex;
		private String content, name;
		public Tag(String name, String content, int startIndex, int endIndex) {
			this.name = name;
			this.content = content;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}
		public String getName() {return name;}
		public String getContent() {return content;}
		public int getStartIndex() {return startIndex;}
		public int getEndIndex() {return endIndex;}
		public boolean contains(Tag tag) {
			return (startIndex < tag.getStartIndex() && endIndex > tag.getEndIndex()) ? true : false;
		}
	}
	
	
	/**
	 * Represents the viewBox attribute of an SVG, defining the visible area.
	 * 
	 * ViewBox are just pack of 4 double variables, which define sight area of an image.
	 */
	public class ViewBox {
		private double x1, y1, x2, y2;
		public ViewBox(double x1, double y1, double x2, double y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
		public double getX1() {return x1;}
		public double getY1() {return y1;}
		public double getX2() {return x2;}
		public double getY2() {return y2;}
	}
	
	
	// SETTERS AND GETTERS
	

	/**
	 * Returns the width of the SVG canvas.
	 * 
	 * @return The width of the SVG canvas
	 */
	public double getWidth() {
		return width;
	}
	

	/**
	 * Returns the height of the SVG canvas.
	 * 
	 * @return The height of the SVG canvas
	 */
	public double getHeight() {
		return height;
	}


	/**
	 * Returns the viewBox of the SVG.
	 * 
	 * @return The viewBox of the SVG
	 */
	public ViewBox getViewBox() {
		return viewBox;
	}


	/**
	 * Returns the version of the SVG.
	 * 
	 * @return The version of the SVG
	 */
	public String getVersion() {
		return version;
	}


	/**
	 * Returns the ID of the SVG.
	 * 
	 * @return The ID of the SVG
	 */
	public String getId() {
		return id;
	}


	/**
	 * Returns an array of Path objects representing the paths in the SVG.
	 * 
	 * @return An array of Path objects representing the paths in the SVG
	 */
	public Path[] getPaths() {return paths;}
	
	
	/**
	 * Represents x and y coordinates as arrays.
	 */
	public class Points {
		public Points(double[] x, double[] y) {
			this.x = x;
			this.y = y;
		}
		public Points() {
			this(null, null);
		}
		public double[] x, y;
	}
	

	/* x1, y1 - first endpoint od an elliptical arc
	 * radius x, radius y of an ellipse
	 * phi - x axis rotation in degrees
	 * large, sweep - flags
	 * x2, y2 - second endpoint of an ellipse
	 * */
	/**
	 * Gets the points for an elliptical arc using the specified parameters.
	 * 
	 * @param x1 The starting x-coordinate
	 * @param y1 The starting y-coordinate
	 * @param rx The x-radius of the ellipse
	 * @param ry The y-radius of the ellipse
	 * @param phi The rotation angle of the ellipse in degrees
	 * @param large Whether the arc sweeps a large angle
	 * @param sweep Whether the arc sweeps in a positive direction
	 * @param x2 The ending x-coordinate
	 * @param y2 The ending y-coordinate
	 * @param points The number of points to generate
	 * @return The Points object containing the arc points
	 */
	public Points getArcPoints(double x1, double y1, double rx, double ry, double phi, boolean large, boolean sweep, double x2, double y2, int points) { 

		// An ellipse parametric equation for getting the ellipse points is:
		// x = cx + cos(phi) * rx * cos(t) - sin(phi) * ry * sin(t)
		// y = cy + cos(phi) * ry * sin(t) + sin(phi) * rx * cos(t)
		
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
		Points arcPoints = new Points();
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


	/**
	 * Calculates and returns points along a quadratic Bezier curve defined by control points.
	 * 
	 * This method generates points along a quadratic Bezier curve based on the provided control points
	 * and the number of desired points. The curve is defined by three control points: (x1, y1), (x2, y2),
	 * and (x3, y3). The number of points generated is determined by the 'points' parameter.
	 * 
	 * @param x1 The x-coordinate of the first control point
	 * @param y1 The y-coordinate of the first control point
	 * @param x2 The x-coordinate of the second control point
	 * @param y2 The y-coordinate of the second control point
	 * @param x3 The x-coordinate of the third control point
	 * @param y3 The y-coordinate of the third control point
	 * @param points The number of points to generate along the Bezier curve, including the endpoints
	 * @return A Points object containing arrays of x and y coordinates of points along the Bezier curve
	 */
	public Points getQBezierPoints(double x1, double y1, double x2, double y2, double x3, double y3, int points) {
		double[] x = new double[points];
		double[] y = new double[points];
		for(double t = 0, dt = 1.0 / (points - 1), T = 0;t <= 1;t += dt, T++) {
			x[(int)T] = (1 - t)*((1 - t)*x1 + t*x2) + t*((1 - t)*x2 + t*x3);
			y[(int)T] = (1 - t)*((1 - t)*y1 + t*y2) + t*((1 - t)*y2 + t*y3);
		}
		x[x.length - 1] = x3;
		y[y.length - 1] = y3;
		Points p = new Points(x, y);
		return p;
	}
	
	
	/**
	 * Calculates and returns points along a cubic Bezier curve defined by control points.
	 * 
	 * This method generates points along a cubic Bezier curve based on the provided control points
	 * and the number of desired points. The curve is defined by four control points: (x1, y1), (x2, y2),
	 * (x3, y3), and (x4, y4). The number of points generated is determined by the 'points' parameter.
	 * 
	 * @param x1 The x-coordinate of the first control point
	 * @param y1 The y-coordinate of the first control point
	 * @param x2 The x-coordinate of the second control point
	 * @param y2 The y-coordinate of the second control point
	 * @param x3 The x-coordinate of the third control point
	 * @param y3 The y-coordinate of the third control point
	 * @param x4 The x-coordinate of the fourth control point
	 * @param y4 The y-coordinate of the fourth control point
	 * @param points The number of points to generate along the Bezier curve, including the endpoints
	 * @return A Points object containing arrays of x and y coordinates of points along the Bezier curve
	 */
	public Points getCBezierPoints(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, int points) {
		double[] x = new double[points];
		double[] y = new double[points];
		for(double t = 0, dt = 1.0 / (points - 1), T = 0;t <= 1;t += dt, T++) {
			x[(int)T] = x1 * Math.pow(1-t, 3) + x2 * Math.pow(1-t, 2) * t * 3 + x3 * (1-t) * Math.pow(t, 2) * 3 + x4 * 1 * Math.pow(t, 3);
			y[(int)T] = y1 * Math.pow(1-t, 3) + y2 * Math.pow(1-t, 2) * t * 3 + y3 * (1-t) * Math.pow(t, 2) * 3 + y4 * 1 * Math.pow(t, 3);
		}
		x[x.length - 1] = x4;
		y[y.length - 1] = y4;
		Points p = new Points(x, y);
		return p;
	}
	
	
	/**
	 * Draws the paths contained in this SVG using the specified drawing method.
	 * 
	 * This method iterates through the paths contained in the SVG and interprets the commands within each path
	 * to generate the corresponding lines or curves. The drawing method is provided through the 'drawLineMethod' parameter.
	 * 
	 * @param curvePoints The number of points to generate along curves, influencing their smoothness
	 * @param drawLineMethod The drawing method to be used for drawing lines or curves between points
	 */
	public void draw(int curvePoints, DrawLineMethod drawLineMethod) {
		if(paths == null) return;
		
		for(Path path : paths) {
			
			double x0 = 0;
			double y0 = 0;
			double ix = 0; // initial x
			double iy = 0; // initial y
			
			for(Command command : path.getCommands()) {
				
				if(command.getLetter() == 'M') {
					int x = 0, y = 1;
					double[] p = command.getPoints();
					ix = p[x];
					iy = p[y];
					if(p.length > 3) for(int t = 0;t < p.length / 2 - 1;t++)
						drawLineMethod.drawLine(p[x], p[y], p[x += 2], p[y += 2]);
					x0 = p[p.length - 2];
					y0 = p[p.length - 1];
				}
				if(command.getLetter() == 'm') {
					int x = 0, y = 1;
					double[] p = command.getPoints();
					ix = x0 + p[x];
					iy = y0 + p[y];
					if(p.length > 3) {
						for(int t = 0;t < p.length / 2 - 1;t++) {
							drawLineMethod.drawLine(x0 += p[x], y0 += p[y], x0 += p[x += 2], y0 += p[y += 2]);
						}
					}
					else {
						x0 += p[0];
						y0 += p[1];
					}
				}
				
				if(command.getLetter() == 'H') {
					double[] p = command.getPoints();
					for(double d : p) drawLineMethod.drawLine(x0, y0, d, y0);
					x0 = p[p.length - 1];
				}
				if(command.getLetter() == 'h') {
					double[] p = command.getPoints();
					for(double d : p) {
						drawLineMethod.drawLine(x0, y0, x0 += d, y0);
					}
				}
				
				if(command.getLetter() == 'V') {
					double[] p = command.getPoints();
					for(double d : p) drawLineMethod.drawLine(x0, y0, x0, d);
					y0 = p[p.length - 1];
				}
				if(command.getLetter() == 'v') {
					double[] p = command.getPoints();
					for(double d : p) {
						drawLineMethod.drawLine(x0, y0, x0, y0 += d);
					}
				}
				
				if(command.getLetter() == 'L') {
					int x = 0, y = 1;
					double[] p = command.getPoints();
					for(int t = 0;t < p.length / 2;t++) {
						drawLineMethod.drawLine(x0, y0, p[x], p[y]);
						x0 = p[x];
						y0 = p[y];
						x += 2;
						y += 2;
					}
				}
				if(command.getLetter() == 'l') {
					int x = 0, y = 1;
					double[] p = command.getPoints();
					for(int t = 0;t < p.length / 2;t++) {
						drawLineMethod.drawLine(x0, y0, x0 += p[x], y0 += p[y]);
						x += 2;
						y += 2;
					}
				}
				
				if(command.getLetter() == 'A') {
					double[] p = command.getPoints();
					for(int t = 0;t < p.length;t += 7) {
						double rx = p[0 + t];
						double ry = p[1 + t];
						double phi = p[2 + t];
						boolean large = p[3 + t] > 0 ? true : false;
						boolean sweep = p[4 + t] > 0 ? true : false;
						double xk = p[5 + t];
						double yk = p[6 + t];
						if(x0 == xk && y0 == yk) drawLineMethod.drawLine(x0, y0, xk, yk);
						else {
							Points ap = getArcPoints(x0, y0, rx, ry, phi, large, sweep, xk, yk, curvePoints);
							for(int i = 0;i < ap.x.length - 1;i++) drawLineMethod.drawLine(ap.x[i], ap.y[i], ap.x[i + 1], ap.y[i + 1]);
							x0 = xk;
							y0 = yk;
						}
					}
				}
				if(command.getLetter() == 'a') {
					double[] p = command.getPoints();
					for(int t = 0;t < p.length;t += 7) {
						double rx = p[0 + t];
						double ry = p[1 + t];
						double phi = p[2 + t];
						boolean large = p[3 + t] > 0 ? true : false;
						boolean sweep = p[4 + t] > 0 ? true : false;
						double xk = p[5 + t];
						double yk = p[6 + t];
						if(x0 == xk && y0 == yk) drawLineMethod.drawLine(x0, y0, x0 + xk, x0 + yk);
						else {
							Points ap = getArcPoints(x0, y0, rx, ry, phi, large, sweep, x0 + xk, y0 + yk, curvePoints);
							for(int i = 0;i < ap.x.length - 1;i++) drawLineMethod.drawLine(ap.x[i], ap.y[i], ap.x[i + 1], ap.y[i + 1]);
							x0 += xk;
							y0 += yk;
						}
					}
				}
				
				if(command.getLetter() == 'Q') {
					double[] p = command.getPoints();
					for(int t = 0;t < p.length;t += 4) {
						Points bp = getQBezierPoints(x0, y0, p[0 + t], p[1 + t], p[2 + t], p[3 + t], curvePoints);
						for(int i = 0;i < bp.x.length - 1;i++) drawLineMethod.drawLine(bp.x[i], bp.y[i], bp.x[i + 1], bp.y[i + 1]);
						x0 = bp.x[bp.x.length - 1];
						y0 = bp.y[bp.y.length - 1];
					}
				}
				if(command.getLetter() == 'q') {
					double[] p = command.getPoints();
					for(int t = 0;t < p.length;t += 4) {
						Points bp = getQBezierPoints(x0, y0, x0 + p[0 + t], y0 + p[1 + t], x0 + p[2 + t], y0 + p[3 + t], curvePoints);
						for(int i = 0;i < bp.x.length - 1;i++) drawLineMethod.drawLine(bp.x[i], bp.y[i], bp.x[i + 1], bp.y[i + 1]);
						x0 = bp.x[bp.x.length - 1];
						y0 = bp.y[bp.y.length - 1];
					}
				}
				
				if(command.getLetter() == 'C') {
					double[] p = command.getPoints();
					for(int t = 0;t < p.length;t += 6) {
						Points bp = getCBezierPoints(x0, y0, p[0 + t], p[1 + t], p[2 + t], p[3 + t], p[4 + t], p[5 + t], curvePoints);
						for(int i = 0;i < bp.x.length - 1;i++) drawLineMethod.drawLine(bp.x[i], bp.y[i], bp.x[i + 1], bp.y[i + 1]);
						x0 = bp.x[bp.x.length - 1];
						y0 = bp.y[bp.y.length - 1];
					}
				}
				if(command.getLetter() == 'c') {
					double[] p = command.getPoints();
					for(int t = 0;t < p.length;t += 6) {
						Points bp = getCBezierPoints(x0, y0, x0 + p[0 + t], y0 + p[1 + t], x0 + p[2 + t], y0 + p[3 + t], x0 + p[4 + t], y0 + p[5 + t], curvePoints);
						for(int i = 0;i < bp.x.length - 1;i++) drawLineMethod.drawLine(bp.x[i], bp.y[i], bp.x[i + 1], bp.y[i + 1]);
						x0 = bp.x[bp.x.length - 1];
						y0 = bp.y[bp.y.length - 1];
					}
				}

				if(command.getLetter() == 'Z') {
					drawLineMethod.drawLine(x0, y0, ix, iy);
					x0 = ix;
					y0 = iy;
				}
				if(command.getLetter() == 'z') {
					drawLineMethod.drawLine(x0, y0, ix, iy);
					x0 = ix;
					y0 = iy;
				}
				
			}
		} // end of paths loop
			
	}
	
	
	/**
	 * An interface for defining a method to draw a line between two points.
	 */
	public interface DrawLineMethod {
		public void drawLine(double x1, double y1, double x2, double y2);
	}
	
	
	/**
	 * Test main() method that loads svg file. It uses Window class
	 * 
	 * @param args main args
	 */
	public static void main(String[] args) {
		
		SVG aaa = new SVG(new File("C:\\Users\\MSZ\\Desktop\\nebula.svg"));
		Window window = new Window();
		aaa.draw(20, (x1, y1, x2, y2) -> {
			//System.out.println(String.format("%.2f, %.2f  ->  %.2f, %.2f", x1, y1, x2, y2));
			window.drawLine((int)Math.round(x1), (int)Math.round(y1), (int)Math.round(x2), (int)Math.round(y2));
		});
		
	}


}








