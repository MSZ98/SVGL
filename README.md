# SVG Parser Library
This Java library provides tools for parsing SVG (Scalable Vector Graphics) files, extracting drawing instructions, and rendering SVG paths. It allows you to easily work with SVG files, extract path data, and generate graphical representations.

## Features
- Parses SVG files and extracts drawing instructions.
- Supports various SVG path commands such as MoveTo, LineTo, CurveTo, ArcTo, etc.
- Provides classes for representing SVG tags, paths, commands, and groups.
- Supports conversion of SVG drawing instructions to graphical representations.

## Initialization
To start using the library, create an instance of the `SVG` class by providing an SVG file:

```java
File svgFile = new File("path/to/your/file.svg");
SVG svg = new SVG(svgFile);
```

## Example
Here's a simple example of rendering an SVG using the library:

```java
import java.io.File;

public class Main {
    public static void main(String[] args) {
        File svgFile = new File("path/to/your/file.svg");
        SVG svg = new SVG(svgFile);

        int curvePoints = 20;
        svg.draw(curvePoints, (x0, y0, x1, y1) -> {
            // Implement your drawing logic here
            // For example, draw a line from (x0, y0) to (x1, y1)
        });
    }
}
```

## Licence
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more information.
