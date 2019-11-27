import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;

@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel {

    private Double[][] graphicsData;
    private ArrayList<Double> regions;
    private ArrayList<Double> squares;

    private static final double TURN_ANGLE = Math.PI / 2;
    private static final int MARKER_SIZE = 5;

    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean showRegions = false;

    private boolean showRectangle = false;
    private Integer dragPoint = null;

    Point rectPoint1;
    Point rectPoint2;
    private Stack<Double[][]> zooms = null;

    private byte turnCount = 0;

    private Double[] pointToPaint = null;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    private double scale = 1;

    private BasicStroke graphicsStroke;
    private BasicStroke axisStroke;
    private BasicStroke markerStroke;
    private BasicStroke rectStroke;

    private Font axisFont;

    public GraphicsDisplay() {
        setBackground(Color.gray);

        graphicsStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f,
                new float[]{3, 1, 1, 1, 1, 1, 2, 1, 2, 1}, 0.0f);
        rectStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[]{25, 25}, 0.0f);
        axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        markerStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        axisFont = new Font("Serif", Font.BOLD, 36);


        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (graphicsData == null) return;
                if (e.getButton() == MouseEvent.BUTTON3 && zooms != null) {
                    if (zooms.size() > 1) {
                        zooms.pop();
                        repaint();
                    }
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    Double[] point = pointToXY(e.getPoint());
                    int k = Arrays.binarySearch(graphicsData, point, new DataComparatorForSearch(MARKER_SIZE / scale));
                    if (k >= 0) {
                        dragPoint = k;
                    } else {
                        showRectangle = true;
                        rectPoint1 = e.getPoint();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (graphicsData == null) return;
                if (dragPoint != null) {
                    dragPoint = null;
                } else if (e.getButton() == MouseEvent.BUTTON1 && showRectangle) {
                    showRectangle = false;
                    Double[][] zoom = new Double[][]{pointToXY(rectPoint1), pointToXY(rectPoint2)};
                    zooms.push(zoom);
                    repaint();
                }
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                if (graphicsData == null) return;
                if (dragPoint != null) {
                    graphicsData[dragPoint][1] = pointToXY(e.getPoint())[1];
                    regions = null;
                    repaint();
                } else {
                    rectPoint2 = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (graphicsData != null) {
                    super.mouseMoved(e);
                    Double[] point = pointToXY(e.getPoint());
                    int k = Arrays.binarySearch(graphicsData, point, new DataComparatorForSearch(MARKER_SIZE / scale));
                    if (k >= 0) {
                        pointToPaint = graphicsData[k];
                        repaint();
                    } else {
                        pointToPaint = null;
                        repaint();
                    }
                }
            }
        });
    }

    public void showGraphics(Double[][] graphicsData) {
        this.graphicsData = graphicsData;
        Arrays.sort(graphicsData, new DataComparatorForSort());
        regions = null;
        squares = null;
        zooms = null;
        repaint();
    }

    public Double[][] getGraphicsData() {
        return graphicsData;
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void setShowRegions(boolean showRegions) {
        this.showRegions = showRegions;
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (graphicsData == null || graphicsData.length == 0) return;

        if (zooms == null) {
            minX = graphicsData[0][0];
            maxX = graphicsData[graphicsData.length - 1][0];
            minY = graphicsData[0][1];
            maxY = minY;
            for (int i = 1; i < graphicsData.length; i++) {
                if (graphicsData[i][1] < minY) minY = graphicsData[i][1];
                if (graphicsData[i][1] > maxY) maxY = graphicsData[i][1];
            }
            Double[][] p = new Double[][]{{minX, minY},{maxX,maxY}};
            zooms = new Stack<>();
            zooms.push(p);
        }

        if (zooms.peek()[1][0] > zooms.peek()[0][0]) {
            maxX = zooms.peek()[1][0];
            minX = zooms.peek()[0][0];
        } else {
            minX = zooms.peek()[1][0];
            maxX = zooms.peek()[0][0];
        }

        if (zooms.peek()[1][1] > zooms.peek()[0][1]) {
            maxY = zooms.peek()[1][1];
            minY = zooms.peek()[0][1];
        } else {
            minY = zooms.peek()[1][1];
            maxY = zooms.peek()[0][1];
        }

        double h = getSize().getHeight();
        double w = getSize().getWidth();

        double scaleX = w / (maxX - minX);
        double scaleY = h / (maxY - minY);

        if (scaleX < scaleY) {
            scale = scaleX;
            double yIncrement = (h / scale - (maxY - minY)) / 2;
            maxY += yIncrement;
            minY -= yIncrement;
        } else {
            scale = scaleY;
            double xIncrement = (w / scale - (maxX - minX)) / 2;
            maxX += xIncrement;
            minX -= xIncrement;
        }

        Graphics2D canvas = (Graphics2D) g;
        if (turnCount != 0) paintTurn(canvas);
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Paint oldPaint = canvas.getPaint();
        Font oldFont = canvas.getFont();
        if (showAxis) paintAxis(canvas);
        paintGraphics(canvas);
        if (showMarkers) paintMarkers(canvas);
        if (showRegions) {
            if (regions == null) findRegions();
            paintRegions(canvas);
        }
        if (pointToPaint != null) {
            paintPoint(canvas);
        }
        if (showRectangle) paintRectangle(canvas);
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);
    }

    protected void paintRectangle(Graphics2D canvas) {
        if (rectPoint1 == null || rectPoint2 == null) return;
        canvas.setColor(Color.YELLOW);
        canvas.setPaint(Color.YELLOW);
        canvas.setStroke(rectStroke);

        GeneralPath path = new GeneralPath();
        path.moveTo(rectPoint1.getX(), rectPoint1.getY());
        path.lineTo(rectPoint1.getX(), rectPoint2.getY());
        path.lineTo(rectPoint2.getX(), rectPoint2.getY());
        path.lineTo(rectPoint2.getX(), rectPoint1.getY());
        path.lineTo(rectPoint1.getX(), rectPoint1.getY());
        canvas.draw(path);
    }

    protected void paintGraphics(Graphics2D canvas) {
        int firstInd = 0; for (;firstInd < graphicsData.length && graphicsData[firstInd][0] < minX; ++firstInd ) ; if(firstInd != 0) --firstInd;
        canvas.setStroke(graphicsStroke);
        canvas.setColor(Color.RED);

        GeneralPath graphics = new GeneralPath();

        Point2D.Double point = xyToPoint(graphicsData[firstInd][0], graphicsData[firstInd][1]);
        graphics.moveTo(point.getX(), point.getY());
        for (int i = firstInd + 1; i < graphicsData.length; i++) {
            point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            graphics.lineTo(point.getX(), point.getY());

        }
        canvas.draw(graphics);
    }

    protected boolean checkPoint(Double y) {
        StringBuffer str = new StringBuffer(String.format(Locale.ENGLISH, "%.14f", y));
        if (str.indexOf(".") != -1) {
            for (int i = str.length() - 1; str.charAt(i) == '0'; i--) {
                str.deleteCharAt(i);
            }
            str.deleteCharAt(str.indexOf("."));
        }
        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i - 1) >= str.charAt(i)) return false;
        }
        return true;
    }

    protected void paintPoint(Graphics2D canvas) {
        if (pointToPaint != null) {
            canvas.setPaint(Color.YELLOW);
            String format = "%.4f";
            canvas.setFont(new Font("TimesNewRoman", Font.BOLD, 16));
            Rectangle2D bounds = canvas.getFont().getStringBounds("(" + String.format(Locale.ENGLISH, format, pointToPaint[0])
                            + "," + String.format(Locale.ENGLISH, format, pointToPaint[1]) + ")", canvas.getFontRenderContext());

            Point2D po = xyToPoint(pointToPaint[0], pointToPaint[1]);
            double posX = po.getX() - bounds.getWidth() / 2;
            double posY = po.getY() - bounds.getHeight() / 2;
            if (posX + bounds.getWidth() > xyToPoint(maxX, 0).getX())
                posX = xyToPoint(maxX, 0).getX() - bounds.getWidth();
            else if (posX < 0) posX = 0;
            if (posY - bounds.getHeight() < xyToPoint(0, maxY).getY())
                posY = xyToPoint(0, maxY).getY() + bounds.getHeight();

            canvas.drawString("(" + String.format(Locale.ENGLISH, format, pointToPaint[0]) + ";"
                            + String.format(Locale.ENGLISH, format, pointToPaint[1]) + ")",
                    (float) posX, (float) posY);
            pointToPaint = null;
        }
    }

    protected void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(markerStroke);
        int firstInd = 0; for (;firstInd < graphicsData.length && graphicsData[firstInd][0] < minX; ++firstInd ) ;
        for (int i = firstInd; i < graphicsData.length && graphicsData[i][0] <= maxX; i++) {
            canvas.setPaint(Color.BLACK);
            canvas.setColor(Color.BLACK);
            Point2D.Double center = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            Line2D.Double line1 = new Line2D.Double(shiftPoint(center, -MARKER_SIZE, 0), shiftPoint(center, MARKER_SIZE, 0));
            Line2D.Double line2 = new Line2D.Double(shiftPoint(center, 0, -MARKER_SIZE), shiftPoint(center, 0, MARKER_SIZE));
            Line2D.Double line3 = new Line2D.Double(shiftPoint(center, -MARKER_SIZE, -MARKER_SIZE), shiftPoint(center, MARKER_SIZE, MARKER_SIZE));
            Line2D.Double line4 = new Line2D.Double(shiftPoint(center, -MARKER_SIZE, MARKER_SIZE), shiftPoint(center, MARKER_SIZE, -MARKER_SIZE));

            if (graphicsData[i].equals(pointToPaint)) {
                canvas.setPaint(Color.YELLOW);
                canvas.setColor(Color.YELLOW);
            } else if (checkPoint(graphicsData[i][1])) {
                canvas.setPaint(Color.GREEN);
                canvas.setColor(Color.GREEN);
            }

            canvas.draw(line1);
            canvas.draw(line2);
            canvas.draw(line3);
            canvas.draw(line4);

            canvas.fill(line1);
            canvas.fill(line2);
            canvas.fill(line3);
            canvas.fill(line4);
        }
    }

    protected void paintRegions(Graphics2D canvas) {
        if (regions == null) return;
        canvas.setStroke(markerStroke);
        canvas.setPaint(Color.BLACK);
        canvas.setColor(Color.BLACK);
        int firstIndex = 0;
        int firstItReg = 0; for (;firstItReg < regions.size() && regions.get(firstItReg) < minX; firstItReg++); if (firstItReg != 0) --firstItReg;
        for (int itReg = firstItReg; itReg < regions.size() - 1; ++itReg) {
            GeneralPath region = new GeneralPath();
            Point2D.Double point = xyToPoint(regions.get(itReg), 0);
            region.moveTo(point.getX(), point.getY());

            for (; firstIndex < graphicsData.length - 1 && regions.get(itReg) > graphicsData[firstIndex][0]; firstIndex++) ;

            point = xyToPoint(graphicsData[firstIndex][0], graphicsData[firstIndex][1]);
            region.lineTo(point.getX(), point.getY());

            for (int i = firstIndex + 1; i < graphicsData.length && graphicsData[i][0] <= regions.get(itReg + 1); i++) {
                point = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
                region.lineTo(point.getX(), point.getY());
            }
            point = xyToPoint(regions.get(itReg + 1), 0);
            region.lineTo(point.getX(), point.getY());
            region.closePath();
            canvas.draw(region);
            canvas.fill(region);

            Double maxHeight = 0.0;
            for (Double[] p : graphicsData) {
                if (p[0] < regions.get(itReg)) continue;
                if (p[0] > regions.get(itReg + 1)) break;
                if (Math.abs(maxHeight) < Math.abs(p[1])) maxHeight = p[1];
            }
            Font regFont = new Font("TimesRoman", Font.BOLD, 13);
            canvas.setFont(regFont);
            Point2D.Double labelPos = xyToPoint((regions.get(itReg + 1) + regions.get(itReg)) / 2, maxHeight/2);
            canvas.setPaint(Color.RED);
            String format = "%.2f";
            String str = String.format(Locale.ENGLISH, format, squares.get(itReg));
            Rectangle2D bounds = regFont.getStringBounds(str, canvas.getFontRenderContext());
            canvas.drawString(str, (float) (labelPos.getX() - bounds.getWidth()/2),
                    (float) (labelPos.getY() + bounds.getHeight()/2));
            canvas.setPaint(Color.BLACK);
            canvas.setColor(Color.BLACK);
        }

    }

    protected void findRegions() {
        regions = new ArrayList<>();
        if (graphicsData[0][1] == 0) regions.add(graphicsData[0][0]);
        for (int i = 1; i < graphicsData.length; i++) {
            if (graphicsData[i][1] == 0) {
                regions.add(graphicsData[i][0]);
            } else if (graphicsData[i - 1][1] * graphicsData[i][1] < 0) {
                Double x = (graphicsData[i][0] * graphicsData[i - 1][1] - graphicsData[i - 1][0] * graphicsData[i][1])
                        / (graphicsData[i - 1][1] - graphicsData[i][1]);
                regions.add(x);
            }
        }
        calcSquares();
    }

    protected void calcSquares() {
        int calcSquareFirst = 0;
        squares = new ArrayList<>();
        Double sq;
        for (int j = 0; j < regions.size() - 1; j++) {
            sq = 0.0;
            for (; calcSquareFirst < graphicsData.length && graphicsData[calcSquareFirst][0] < regions.get(j); calcSquareFirst++)
                ;
            if (graphicsData[calcSquareFirst][1] != 0) {
                sq += graphicsData[calcSquareFirst][1] * (graphicsData[calcSquareFirst][0] - regions.get(j));
            }
            int i;
            for (i = calcSquareFirst + 1; i < graphicsData.length - 1 && graphicsData[i][0] <= regions.get(j + 1); i++) {
                sq += (graphicsData[i][1] + graphicsData[i - 1][1]) * (graphicsData[i][0] - graphicsData[i - 1][0]);
            }
            i--;
            if (graphicsData[i][1] != 0) {
                sq += graphicsData[i][1] * (regions.get(j + 1) - graphicsData[i][0]);
            }
            squares.add(Math.abs(sq / 2));
        }
    }

    protected void paintAxis(Graphics2D canvas) {
        canvas.setStroke(axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setPaint(Color.BLACK);
        canvas.setFont(axisFont);
        FontRenderContext context = canvas.getFontRenderContext();
        if (minX <= 0.0 && maxX >= 0.0) {
            canvas.draw(new Line2D.Double(xyToPoint(0, maxY), xyToPoint(0, minY)));
            GeneralPath arrow = new GeneralPath();
            Point2D.Double lineEnd = xyToPoint(0, maxY);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
            arrow.lineTo(arrow.getCurrentPoint().getX() + 5, arrow.getCurrentPoint().getY() + 20);
            arrow.lineTo(arrow.getCurrentPoint().getX() - 10, arrow.getCurrentPoint().getY());
            arrow.closePath();
            canvas.draw(arrow);
            canvas.fill(arrow);
            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0, maxY);
            canvas.drawString("y", (float) labelPos.getX() + 10, (float) (labelPos.getY() - bounds.getY()));
        }
        if (minY <= 0.0 && maxY >= 0.0) {
            canvas.draw(new Line2D.Double(xyToPoint(minX, 0), xyToPoint(maxX, 0)));

            GeneralPath arrow = new GeneralPath();
            Point2D.Double lineEnd = xyToPoint(maxX, 0);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
            arrow.lineTo(arrow.getCurrentPoint().getX() - 20, arrow.getCurrentPoint().getY() - 5);
            arrow.lineTo(arrow.getCurrentPoint().getX(), arrow.getCurrentPoint().getY() + 10);
            arrow.closePath();
            canvas.draw(arrow);
            canvas.fill(arrow);

            Rectangle2D bounds = axisFont.getStringBounds("x", context);
            Point2D.Double labelPos = xyToPoint(maxX, 0);
            canvas.drawString("x", (float) (labelPos.getX() - bounds.getWidth() - 10), (float) (labelPos.getY() + bounds.getY()));
        }
    }

    public void turnLeft() {
        if (--turnCount == -4) turnCount = 0;
        repaint();
    }

    public void turnRight() {
        if (++turnCount == 4) turnCount = 0;
        repaint();
    }

    protected void paintTurn(Graphics2D canvas) {
        double angle = turnCount * TURN_ANGLE;
        AffineTransform at = AffineTransform.getRotateInstance(angle, getSize().getWidth() / 2, getSize().getHeight() / 2);
        canvas.setTransform(at);
    }

    protected Point2D transformPoint(Point2D p) {
        if (turnCount != 0) {
            double angle = turnCount * TURN_ANGLE;
            AffineTransform at = AffineTransform.getRotateInstance(angle, getSize().getWidth() / 2, getSize().getHeight() / 2);
            return at.transform(p, p);
        }
        return p;
    }

    class DataComparatorForSearch implements Comparator<Double[]> {
        double bounds;

        DataComparatorForSearch(double bounds) {
            this.bounds = bounds;
        }

        @Override
        public int compare(Double[] o1, Double[] o2) {
            if (Math.abs(o1[0] - o2[0]) <= bounds && Math.abs(o1[1] - o2[1]) <= bounds) return 0;
            if (o1[0] < o2[0]) return -1;
            return 1;
        }
    }

    class DataComparatorForSort implements Comparator<Double[]> {
        @Override
        public int compare(Double[] o1, Double[] o2) {
            if (o1[0] < o2[0]) return -1;
            if (o1[0] > o2[0]) return 1;
            return 0;
        }
    }

    protected Point2D.Double xyToPoint(double x, double y) {
        double deltaX = x - minX;
        double deltaY = maxY - y;
        return new Point2D.Double(deltaX * scale, deltaY * scale);
    }

    protected Double[] pointToXY(Point p) {
        Double[] xy = new Double[2];
        xy[0] = p.getX() / scale + minX;
        xy[1] = maxY - p.getY() / scale;
        return xy;
    }

    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
        Point2D.Double dest = new Point2D.Double();
        dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
        return dest;
    }
}