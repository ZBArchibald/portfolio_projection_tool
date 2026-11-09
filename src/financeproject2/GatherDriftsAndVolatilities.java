package financeproject2;

import java.util.ArrayList;
import java.lang.Math;

public class GatherDriftsAndVolatilities 
{
	public static ArrayList<ArrayList<Double>> gatherStatisticsForStocks(ArrayList<ArrayList<Double>> LogReturnsOfStocks,
			int upSampleFactor)
	{
		ArrayList<ArrayList<Double>> driftsAndVolatilitiesOfStocks = new ArrayList<ArrayList<Double>>();
		
		for (int i = 0; i < LogReturnsOfStocks.size(); i++)
		{
			ArrayList<Double> logReturnsOfStock = LogReturnsOfStocks.get(i);
			
			Double driftOfStock = computeDriftOfStock(logReturnsOfStock);
			Double volatilityOfStock = computeVolatilityOfStock(logReturnsOfStock, driftOfStock);

			ArrayList<Double> driftsAndVolatilitiesOfStock = new ArrayList<Double>();
			driftsAndVolatilitiesOfStock.add(driftOfStock);
			driftsAndVolatilitiesOfStock.add(volatilityOfStock);
			
			driftsAndVolatilitiesOfStocks.add(driftsAndVolatilitiesOfStock);
		}
		
		return driftsAndVolatilitiesOfStocks;
	}
	
	public static ArrayList<Double> generateLogReturnsOfStock(ArrayList<Double> dailyPricesOfStock)
	{
		ArrayList<Double> LogReturnsOfStock = new ArrayList<Double>();
		
		for (int i = 0; i < dailyPricesOfStock.size() - 1; i++)
		{
			Double LogReturn = Math.log(dailyPricesOfStock.get(i+1) / dailyPricesOfStock.get(i));
			LogReturnsOfStock.add(LogReturn);
		}
		
		return LogReturnsOfStock;
	}
	
	private static Double computeDriftOfStock(ArrayList<Double> logReturnsOfStock) 
	{
		Double drift = 250.0 * (sum(logReturnsOfStock)) / logReturnsOfStock.size();
		return drift;
	}

	private static Double computeVolatilityOfStock(ArrayList<Double> logReturnsOfStock, Double driftOfStock) 
	{
		Double volatilityOfStock = 250 * variance(logReturnsOfStock, driftOfStock / 250);
		
		return volatilityOfStock;
	}

	public static Double sum(ArrayList<Double> data) 
	{
		Double sum = 0.0;
		for (int i = 0; i < data.size(); i++)
		{
			sum += data.get(i);
		}
		
		return sum;
	}
	
	private static Double variance(ArrayList<Double> data, Double mean) 
	{
		ArrayList<Double> centeredData = center(data, mean);
		
		ArrayList<Double> squaredCenteredData = squareElements(centeredData);
		
		Double variance = sum(squaredCenteredData) / data.size();
		
		return variance;
	}

	private static ArrayList<Double> center(ArrayList<Double> data, Double mean) 
	{
		ArrayList<Double> centeredData = new ArrayList<Double>();
		
		for(int i = 0; i < data.size(); i++)
		{
			centeredData.add(data.get(i) - mean);
		}
		
		return centeredData;
	}

	private static ArrayList<Double> squareElements(ArrayList<Double> data) 
	{
		ArrayList<Double> dataSquared = new ArrayList<Double>();
		for (Double d : data)
		{
			dataSquared.add(Math.pow(d,2));
		}
		
		return dataSquared;
	}
	
	
	public static ArrayList<ArrayList<Double>> generateLogReturnsOfStocks(ArrayList<ArrayList<Double>> dailyPricesOfStocks)
	{
		ArrayList<ArrayList<Double>> logReturnsOfStocks = new ArrayList<ArrayList<Double>>();
		
		for (int i = 0; i < dailyPricesOfStocks.size(); i++)
		{
			ArrayList<Double> dailyPricesOfStock = dailyPricesOfStocks.get(i);
			ArrayList<Double> logReturnsOfStock = generateLogReturnsOfStock(dailyPricesOfStock);
			
			logReturnsOfStocks.add(logReturnsOfStock);
		}
		
		return logReturnsOfStocks;
	}
	
	
	
}