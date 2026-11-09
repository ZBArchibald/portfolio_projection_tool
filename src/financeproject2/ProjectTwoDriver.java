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

public class ProjectTwoDriver 
{
	public static void main(String[] args) throws IOException
	{
		int upSampleFactor = 1;
		
		File stockData = new File("StockData.txt");
		
		ArrayList<ArrayList<Double>> dailyPricesOfStocks = formatStockData(stockData);
		
		ArrayList<ArrayList<Double>> logReturnsOfStocks = 
				GatherDriftsAndVolatilities.generateLogReturnsOfStocks(dailyPricesOfStocks);
		
		ArrayList<ArrayList<Double>> driftsAndVolatilities = 
				GatherDriftsAndVolatilities.gatherStatisticsForStocks(logReturnsOfStocks, upSampleFactor);
		
		ArrayList<ArrayList<Double>> covolatilityMatrix =
		CovolatilityMatrixCalculator.generateCovolatilityMatrix(dailyPricesOfStocks, driftsAndVolatilities, logReturnsOfStocks);
		
		SDESystem SDESystemForStocks = new SDESystem(driftsAndVolatilities, covolatilityMatrix);
		
		SDESystemForStocks.printSDEComponents();
		
		simulateTenTrajectories(SDESystemForStocks, dailyPricesOfStocks, upSampleFactor);
		
		NumericalSDESolution simulation = new NumericalSDESolution(SDESystemForStocks, dailyPricesOfStocks,
				upSampleFactor);
		
		formattedPrint(simulation.getDailyPrices(), upSampleFactor);
	}

	private static ArrayList<ArrayList<Double>> formatStockData(File stockData) throws FileNotFoundException 
	{
		ArrayList<ArrayList<Double>> dailyPricesOfStocks = new ArrayList<ArrayList<Double>>();
		
		Scanner fileScanner = new Scanner(stockData);
		
		while (fileScanner.hasNextLine())
		{
			String currentLine = new String(fileScanner.nextLine());
			
			if (currentLine.length() > 0 && currentLine.charAt(0)=='{') 
			{
				addStockToDailyPricesOfStocks(fileScanner, dailyPricesOfStocks);
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
	
	private static void simulateTenTrajectories(SDESystem SDESystem, 
			ArrayList<ArrayList<Double>> dailyPricesOfStocks, int upSampleFactor) throws IOException 
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
			evilShadowSkull.write("Stock " + stock);
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
		File file = new File("SimulatedTrajectories.txt");
		file.createNewFile();
	}
	
}
