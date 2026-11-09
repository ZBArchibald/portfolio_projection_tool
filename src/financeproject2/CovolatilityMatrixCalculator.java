package financeproject2;

import java.util.ArrayList;

public class CovolatilityMatrixCalculator 
{

	public static ArrayList<ArrayList<Double>> generateCovolatilityMatrix(ArrayList<ArrayList<Double>> dailyPricesOfStocks, 
		ArrayList<ArrayList<Double>> driftsAndVolatilities, ArrayList<ArrayList<Double>> logReturnsOfStocks)	
	{
		ArrayList<ArrayList<Double>> covolatilityMatrix = constructEmptyCovolatilityMatrix(dailyPricesOfStocks.size());
		
		populateCovolatilityMatrixDiagonal(covolatilityMatrix, driftsAndVolatilities);
		
		populateCovolatilityMatrixNonDiagonal(covolatilityMatrix, driftsAndVolatilities, logReturnsOfStocks);
		
		return covolatilityMatrix;
	}

		private static ArrayList<ArrayList<Double>> constructEmptyCovolatilityMatrix(int size) 
	{
		ArrayList<ArrayList<Double>> covolatilityMatrix = new ArrayList<ArrayList<Double>>();
		
		for(int i = 0; i < size; i++)
		{
			ArrayList<Double> rowOfMatrix = new ArrayList<Double>();
			
			for(int j = 0; j < size; j++)
			{
				rowOfMatrix.add(0.0);
			}
			
			covolatilityMatrix.add(rowOfMatrix);
		}
	
		return covolatilityMatrix;
	}

	private static void populateCovolatilityMatrixDiagonal(ArrayList<ArrayList<Double>> covolatilityMatrix,
			ArrayList<ArrayList<Double>> driftsAndVolatilities) 
	{
		for(int row = 0; row < covolatilityMatrix.size(); row++)
		{
			Double volatilityOfRowthStock = driftsAndVolatilities.get(row).get(1);
			
			covolatilityMatrix.get(row).set(row, volatilityOfRowthStock);
		}
	}
	
	private static void populateCovolatilityMatrixNonDiagonal(ArrayList<ArrayList<Double>> covolatilityMatrix,
			ArrayList<ArrayList<Double>> driftsAndVolatilities, ArrayList<ArrayList<Double>> logReturnsOfStocks) 
	{
		for(int row = 0; row < covolatilityMatrix.size(); row++)
		{
			for(int collumn = row + 1; collumn < covolatilityMatrix.size(); collumn++)
			{
				Double currentCovolatility = computeCovolatility(row, collumn, driftsAndVolatilities, logReturnsOfStocks);
				
				covolatilityMatrix.get(row).set(collumn, currentCovolatility);
				covolatilityMatrix.get(collumn).set(row, currentCovolatility);
			}
		}
	}

	private static Double computeCovolatility(int row, int collumn, 
			ArrayList<ArrayList<Double>> driftsAndVolatilities, ArrayList<ArrayList<Double>> logReturnsOfStocks) 
	{
		// Drifts are annualized (250 * mean daily log return), so divide by 250 to recover the daily mean.
		Double rowDailyMean = driftsAndVolatilities.get(row).get(0) / 250.0;
		Double collumnDailyMean = driftsAndVolatilities.get(collumn).get(0) / 250.0;

		ArrayList<Double> productsOfDeviations =
				computeProductsOfDeviations(logReturnsOfStocks, row, collumn, rowDailyMean, collumnDailyMean);

		// Annualize the daily covariance the same way the diagonal variances are annualized.
		int numberOfReturns = productsOfDeviations.size();
		Double covolatility = 250.0 * GatherDriftsAndVolatilities.sum(productsOfDeviations) / numberOfReturns;

		return covolatility;
	}

	private static ArrayList<Double> computeProductsOfDeviations(ArrayList<ArrayList<Double>> logReturnsOfStocks,
			int row, int collumn, Double rowDrift, Double collumnDrift) 
	{
		ArrayList<Double> logReturnsOfRowStock = logReturnsOfStocks.get(row);
		ArrayList<Double> logReturnsOfCollumnStock = logReturnsOfStocks.get(collumn);
		
		ArrayList<Double> productsOfDeviations = new ArrayList<Double>();
		
		for(int i = 0; i < logReturnsOfRowStock.size(); i++)
		{
			Double productOfDeviations = (logReturnsOfRowStock.get(i) - rowDrift) * 
					(logReturnsOfCollumnStock.get(i) - collumnDrift);
			
			productsOfDeviations.add(productOfDeviations);
		}
		return productsOfDeviations;
	}

	

	
}
