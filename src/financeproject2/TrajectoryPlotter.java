package financeproject2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javax.imageio.ImageIO;

/**
 * Renders simulated price trajectories against the actual historical prices as PNG line charts,
 * using only the Java standard library (Java2D + ImageIO).
 *
 * <p>Written with generative tools.
 */
public class TrajectoryPlotter
{
	// Logical size of one chart; images are rendered at SCALE x for crisp lines on high-DPI screens.
	private static final int CHART_WIDTH = 800;
	private static final int CHART_HEIGHT = 450;
	private static final int SCALE = 2;
	private static final int OVERVIEW_COLUMNS = 3;

	private static final int PAD_LEFT = 72;
	private static final int PAD_RIGHT = 28;
	private static final int PAD_TOP = 84;
	private static final int PAD_BOTTOM = 52;

	private static final Color SURFACE = new Color(0xfcfcfb);
	private static final Color TEXT_PRIMARY = new Color(0x0b0b0b);
	private static final Color TEXT_SECONDARY = new Color(0x52514e);
	private static final Color GRID = new Color(0xe6e5e0);
	private static final Color AXIS = new Color(0xc4c3bd);
	private static final Color SIMULATED = new Color(0x2a, 0x78, 0xd6, 100);
	private static final Color SIMULATED_KEY = new Color(0x2a78d6);
	private static final Color ACTUAL = new Color(0xeb6834);

	private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 17);
	private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

	/**
	 * Writes one PNG per stock plus an overview grid of all stocks into outputDirectory.
	 *
	 * @param simulatedData indexed as [simulation][stock][step]
	 * @param actualPrices  indexed as [stock][day]
	 */
	public static void savePlots(ArrayList<String> stockNames, ArrayList<ArrayList<ArrayList<Double>>> simulatedData,
			ArrayList<ArrayList<Double>> actualPrices, int upSampleFactor, File outputDirectory) throws IOException
	{
		System.setProperty("java.awt.headless", "true");

		if (!outputDirectory.exists() && !outputDirectory.mkdirs())
		{
			throw new IOException("Could not create plot directory: " + outputDirectory);
		}

		int numberOfStocks = actualPrices.size();

		for (int stock = 0; stock < numberOfStocks; stock++)
		{
			BufferedImage image = createCanvas(CHART_WIDTH, CHART_HEIGHT);
			Graphics2D g = prepareGraphics(image);

			drawChart(g, 0, 0, CHART_WIDTH, CHART_HEIGHT, stockNames.get(stock),
					trajectoriesForStock(simulatedData, stock), actualPrices.get(stock), upSampleFactor);

			g.dispose();
			ImageIO.write(image, "png", new File(outputDirectory, fileSafe(stockNames.get(stock)) + ".png"));
		}

		saveOverview(stockNames, simulatedData, actualPrices, upSampleFactor, outputDirectory);
	}

	private static void saveOverview(ArrayList<String> stockNames, ArrayList<ArrayList<ArrayList<Double>>> simulatedData,
			ArrayList<ArrayList<Double>> actualPrices, int upSampleFactor, File outputDirectory) throws IOException
	{
		int numberOfStocks = actualPrices.size();
		int columns = Math.min(OVERVIEW_COLUMNS, numberOfStocks);
		int rows = (numberOfStocks + columns - 1) / columns;

		BufferedImage image = createCanvas(CHART_WIDTH * columns, CHART_HEIGHT * rows);
		Graphics2D g = prepareGraphics(image);

		for (int stock = 0; stock < numberOfStocks; stock++)
		{
			int x = (stock % columns) * CHART_WIDTH;
			int y = (stock / columns) * CHART_HEIGHT;

			drawChart(g, x, y, CHART_WIDTH, CHART_HEIGHT, stockNames.get(stock),
					trajectoriesForStock(simulatedData, stock), actualPrices.get(stock), upSampleFactor);
		}

		g.dispose();
		ImageIO.write(image, "png", new File(outputDirectory, "all_stocks.png"));
	}

	private static ArrayList<ArrayList<Double>> trajectoriesForStock(ArrayList<ArrayList<ArrayList<Double>>> simulatedData,
			int stock)
	{
		ArrayList<ArrayList<Double>> trajectories = new ArrayList<ArrayList<Double>>();

		for (ArrayList<ArrayList<Double>> simulation : simulatedData)
		{
			trajectories.add(simulation.get(stock));
		}

		return trajectories;
	}

	private static BufferedImage createCanvas(int logicalWidth, int logicalHeight)
	{
		return new BufferedImage(logicalWidth * SCALE, logicalHeight * SCALE, BufferedImage.TYPE_INT_RGB);
	}

	private static Graphics2D prepareGraphics(BufferedImage image)
	{
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.scale(SCALE, SCALE);

		g.setColor(SURFACE);
		g.fillRect(0, 0, image.getWidth(), image.getHeight());

		return g;
	}

	private static void drawChart(Graphics2D g, int x, int y, int width, int height, String stockName,
			ArrayList<ArrayList<Double>> trajectories, ArrayList<Double> actual, int upSampleFactor)
	{
		double plotLeft = x + PAD_LEFT;
		double plotRight = x + width - PAD_RIGHT;
		double plotTop = y + PAD_TOP;
		double plotBottom = y + height - PAD_BOTTOM;

		// Domain: x in trading days, y in price.
		double xMax = actual.size() - 1;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;

		for (ArrayList<Double> trajectory : trajectories)
		{
			xMax = Math.max(xMax, (trajectory.size() - 1) / (double) upSampleFactor);
			for (double price : trajectory)
			{
				if (Double.isFinite(price))
				{
					yMin = Math.min(yMin, price);
					yMax = Math.max(yMax, price);
				}
			}
		}
		for (double price : actual)
		{
			yMin = Math.min(yMin, price);
			yMax = Math.max(yMax, price);
		}

		double yStep = niceStep(yMax - yMin, 5);
		double yLow = Math.max(0, Math.floor(yMin / yStep) * yStep);
		double yHigh = Math.ceil(yMax / yStep) * yStep;
		if (yHigh <= yLow)
		{
			yHigh = yLow + yStep;
		}
		double xStep = niceStep(xMax, 5);

		Scale scale = new Scale(plotLeft, plotRight, plotTop, plotBottom, 0, xMax, yLow, yHigh);

		drawHeader(g, x, y, stockName, trajectories.size());
		drawAxes(g, scale, xStep, yStep, x, y, width, height);

		// Clip lines to the plot area so extreme paths never draw over the labels.
		Shape previousClip = g.getClip();
		g.clipRect((int) plotLeft, (int) plotTop - 2, (int) (plotRight - plotLeft) + 2, (int) (plotBottom - plotTop) + 4);

		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(SIMULATED);
		for (ArrayList<Double> trajectory : trajectories)
		{
			g.draw(buildPath(trajectory, scale, upSampleFactor));
		}

		g.setColor(ACTUAL);
		g.draw(buildPath(actual, scale, 1));

		g.setClip(previousClip);

		// End dot on the actual series with a surface-colored ring.
		double endX = scale.x(actual.size() - 1);
		double endY = scale.y(actual.get(actual.size() - 1));
		g.setColor(SURFACE);
		g.fill(new Ellipse2D.Double(endX - 6, endY - 6, 12, 12));
		g.setColor(ACTUAL);
		g.fill(new Ellipse2D.Double(endX - 4, endY - 4, 8, 8));
	}

	private static void drawHeader(Graphics2D g, int x, int y, String stockName, int numberOfTrajectories)
	{
		int left = x + PAD_LEFT;

		g.setFont(TITLE_FONT);
		g.setColor(TEXT_PRIMARY);
		g.drawString(stockName + " — simulated vs. actual price", left, y + 34);

		g.setFont(LABEL_FONT);
		FontMetrics metrics = g.getFontMetrics();
		int legendY = y + 60;
		int cursor = left;

		cursor = drawLegendItem(g, metrics, cursor, legendY, SIMULATED_KEY,
				"Simulated paths (" + numberOfTrajectories + ")");
		drawLegendItem(g, metrics, cursor + 20, legendY, ACTUAL, "Actual price");
	}

	private static int drawLegendItem(Graphics2D g, FontMetrics metrics, int x, int baselineY, Color color, String label)
	{
		double keyY = baselineY - metrics.getAscent() / 2.0 + 1;

		g.setColor(color);
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(x, keyY, x + 16, keyY));

		g.setColor(TEXT_SECONDARY);
		g.drawString(label, x + 22, baselineY);

		return x + 22 + metrics.stringWidth(label);
	}

	private static void drawAxes(Graphics2D g, Scale scale, double xStep, double yStep, int x, int y, int width, int height)
	{
		g.setFont(LABEL_FONT);
		FontMetrics metrics = g.getFontMetrics();
		g.setStroke(new BasicStroke(1f));

		// Horizontal gridlines with price labels.
		for (double value = scale.yLow; value <= scale.yHigh + yStep / 1000; value += yStep)
		{
			double py = scale.y(value);
			g.setColor(value == scale.yLow ? AXIS : GRID);
			g.draw(new Line2D.Double(scale.left, py, scale.right, py));

			String label = formatPrice(value, yStep);
			g.setColor(TEXT_SECONDARY);
			g.drawString(label, (float) (scale.left - 10 - metrics.stringWidth(label)),
					(float) (py + metrics.getAscent() / 2.0 - 1));
		}

		// Day labels along the bottom.
		for (double day = 0; day <= scale.xHigh + xStep / 1000; day += xStep)
		{
			double px = scale.x(day);
			String label = String.format(Locale.US, "%,d", Math.round(day));
			g.setColor(TEXT_SECONDARY);
			g.drawString(label, (float) (px - metrics.stringWidth(label) / 2.0), (float) (scale.bottom + 20));
		}

		String axisTitle = "Trading day";
		g.drawString(axisTitle, (float) ((scale.left + scale.right - metrics.stringWidth(axisTitle)) / 2.0),
				(float) (y + height - 10));
	}

	private static Path2D buildPath(ArrayList<Double> values, Scale scale, int stepsPerDay)
	{
		Path2D path = new Path2D.Double();
		boolean started = false;

		for (int i = 0; i < values.size(); i++)
		{
			double value = values.get(i);
			if (!Double.isFinite(value))
			{
				started = false;
				continue;
			}

			double px = scale.x(i / (double) stepsPerDay);
			double py = scale.y(value);

			if (started)
			{
				path.lineTo(px, py);
			}
			else
			{
				path.moveTo(px, py);
				started = true;
			}
		}

		return path;
	}

	/** Rounds range / targetTicks up to a 1, 2 or 5 times a power of ten. */
	private static double niceStep(double range, int targetTicks)
	{
		if (!(range > 0))
		{
			return 1.0;
		}

		double rawStep = range / targetTicks;
		double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
		double residual = rawStep / magnitude;

		double niceResidual;
		if (residual < 1.5) niceResidual = 1;
		else if (residual < 3) niceResidual = 2;
		else if (residual < 7) niceResidual = 5;
		else niceResidual = 10;

		return niceResidual * magnitude;
	}

	private static String formatPrice(double value, double step)
	{
		if (step >= 1)
		{
			return String.format(Locale.US, "$%,.0f", value);
		}
		return String.format(Locale.US, "$%,.2f", value);
	}

	private static String fileSafe(String name)
	{
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	/** Maps data coordinates to pixel coordinates inside the plot area. */
	private static class Scale
	{
		final double left, right, top, bottom, xLow, xHigh, yLow, yHigh;

		Scale(double left, double right, double top, double bottom, double xLow, double xHigh, double yLow, double yHigh)
		{
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
			this.xLow = xLow;
			this.xHigh = xHigh;
			this.yLow = yLow;
			this.yHigh = yHigh;
		}

		double x(double value)
		{
			return left + (value - xLow) / (xHigh - xLow) * (right - left);
		}

		double y(double value)
		{
			return bottom - (value - yLow) / (yHigh - yLow) * (bottom - top);
		}
	}
}
