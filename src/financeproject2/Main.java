package financeproject2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class Main 
{
	public static void main(String[] args) throws IOException
	{
		int upSampleFactor = 1;
		
		File stockData = new File("StockData.txt");
		
		ArrayList<String> stockNames = new ArrayList<String>();

		ArrayList<ArrayList<Double>> dailyPricesOfStocks = formatStockData(stockData, stockNames);
		
		ArrayList<ArrayList<Double>> logReturnsOfStocks = 
				GatherDriftsAndVolatilities.generateLogReturnsOfStocks(dailyPricesOfStocks);
		
		ArrayList<ArrayList<Double>> driftsAndVolatilities = 
				GatherDriftsAndVolatilities.gatherStatisticsForStocks(logReturnsOfStocks, upSampleFactor);
		
		ArrayList<ArrayList<Double>> covolatilityMatrix =
		CovolatilityMatrixCalculator.generateCovolatilityMatrix(dailyPricesOfStocks, driftsAndVolatilities, logReturnsOfStocks);
		
		SDESystem SDESystemForStocks = new SDESystem(driftsAndVolatilities, covolatilityMatrix);
		
		SDESystemForStocks.printSDEComponents();
		
		ArrayList<ArrayList<ArrayList<Double>>> simulatedData =
				simulateTenTrajectories(SDESystemForStocks, dailyPricesOfStocks, stockNames, upSampleFactor);

		TrajectoryPlotter.savePlots(stockNames, simulatedData, dailyPricesOfStocks, upSampleFactor, new File("plots"));
		
		NumericalSDESolution simulation = new NumericalSDESolution(SDESystemForStocks, dailyPricesOfStocks,
				upSampleFactor);
		
		formattedPrint(simulation.getDailyPrices(), upSampleFactor);
	}

	private static ArrayList<ArrayList<Double>> formatStockData(File stockData, ArrayList<String> stockNames)
			throws FileNotFoundException
	{
		ArrayList<ArrayList<Double>> dailyPricesOfStocks = new ArrayList<ArrayList<Double>>();

		Scanner fileScanner = new Scanner(stockData);

		// The first word of the line before each '{' (e.g. "AAPL apple") is used as the stock's name.
		String lastLabel = null;

		while (fileScanner.hasNextLine())
		{
			String currentLine = fileScanner.nextLine().trim();

			if (currentLine.length() > 0 && currentLine.charAt(0)=='{')
			{
				stockNames.add(lastLabel != null ? lastLabel : "Stock " + dailyPricesOfStocks.size());
				lastLabel = null;

				addStockToDailyPricesOfStocks(fileScanner, dailyPricesOfStocks);
			}
			else if (currentLine.length() > 0)
			{
				lastLabel = currentLine.split("\\s+")[0];
			}
		}
		
		fileScanner.close();
		
		return dailyPricesOfStocks;
	}

	private static void addStockToDailyPricesOfStocks(Scanner fileScanner, ArrayList<ArrayList<Double>> dailyPricesOfStocks) 
	{
		ArrayList<Double> dailyPricesOfStock = new ArrayList<Double>();
		
		while (fileScanner.hasNext())
		{
			String currentString = fileScanner.next();
			
			if (currentString.charAt(0) == '}')
					break;
			
			else 
				dailyPricesOfStock.add(Double.parseDouble(currentString));
		}
		
		dailyPricesOfStocks.add(dailyPricesOfStock);
	}
	
	private static void formattedPrint(ArrayList<ArrayList<Double>> dailyPrices, int upSampleFactor) 
	{
		for (int day = 0; day < (dailyPrices.get(0).size()); day++)
		{
			for (int stock = 0; stock < dailyPrices.size(); stock++)
			{
				System.out.print(dailyPrices.get(stock).get(day) + "\t");
			}
			System.out.print("\n");
		}
	}
	
	private static ArrayList<ArrayList<ArrayList<Double>>> simulateTenTrajectories(SDESystem SDESystem,
			ArrayList<ArrayList<Double>> dailyPricesOfStocks, ArrayList<String> stockNames, int upSampleFactor)
			throws IOException
	{
		BufferedWriter evilShadowSkull = new BufferedWriter(new OutputStreamWriter(
			new FileOutputStream("SimulatedTrajectories.txt")));
		
		ArrayList<ArrayList<ArrayList<Double>>> simulatedData = new ArrayList<ArrayList<ArrayList<Double>>>();
		
		//build simlatedData
		for(int simulation = 0; simulation < 10; simulation++)
		{
			NumericalSDESolution simI = new NumericalSDESolution(SDESystem, dailyPricesOfStocks, upSampleFactor);
			
			simulatedData.add(simI.getDailyPrices());
		}
		
		//Gather each stocks data from simulated data and print
		for(int stock = 0; stock < dailyPricesOfStocks.size(); stock++)
		{
			evilShadowSkull.newLine();
			evilShadowSkull.write(stockNames.get(stock));
			evilShadowSkull.newLine();
			
			for(int day = 0; day < dailyPricesOfStocks.get(0).size(); day++)
			{
				for(int sim = 0; sim < 10; sim++)
				{
					evilShadowSkull.write(simulatedData.get(sim).get(stock).get(day) + "\t");
				}
				evilShadowSkull.write(dailyPricesOfStocks.get(stock).get(day) + "\t");
				evilShadowSkull.newLine();
			}
			
			evilShadowSkull.newLine();

		}
		evilShadowSkull.close();

		return simulatedData;
	}
	
}
