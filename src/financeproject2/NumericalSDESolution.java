package financeproject2;

import java.util.ArrayList;
import java.util.Random;

import Jama.Matrix;

public class NumericalSDESolution 
{
	Random gaussianSampler;
	
	SDESystem SDESystemForStocks;
	
	ArrayList<ArrayList<Double>> dailyPrices;
	
	
	public NumericalSDESolution(SDESystem SDESystemForStocks, ArrayList<ArrayList<Double>> dailyPricesOfStocks,
			int upSampleFactor) 
	{
		this.SDESystemForStocks = SDESystemForStocks;
		
		this.gaussianSampler = new Random();
		
		this.dailyPrices = new ArrayList<ArrayList<Double>>();
		
		for (int i = 0; i < dailyPricesOfStocks.size(); i++)
		{
			this.dailyPrices.add(new ArrayList<Double>());
			this.dailyPrices.get(i).add(dailyPricesOfStocks.get(i).get(0));
		}
		
		for (int i = 1; i < dailyPricesOfStocks.get(0).size() * upSampleFactor; i++)
		{
			ArrayList<Double> nextValues = simulateDay(i, upSampleFactor);
			
			addToDailyPrices(nextValues);
		}
	}

	private void addToDailyPrices(ArrayList<Double> nextValues)
	{
		for (int i = 0; i < nextValues.size(); i++)
		{
			dailyPrices.get(i).add(nextValues.get(i));
		}
	}

	private ArrayList<Double> simulateDay(int day, int upSampleFactor) 
	{
		ArrayList<Double> yesterdaysStockPrices = new ArrayList<Double>();
		for (int i = 0; i < dailyPrices.size(); i++)
		{
			yesterdaysStockPrices.add(this.dailyPrices.get(i).get(day-1));
		}
		
		ArrayList<Double> todaysStockPrices = eulerMaruyamaIteration(yesterdaysStockPrices, upSampleFactor);
			
		return todaysStockPrices;
	}

	private ArrayList<Double> eulerMaruyamaIteration(ArrayList<Double> Sj, int upSampleFactor) 
	{
		ArrayList<Double> Sjplus1 = new ArrayList<Double>();
		
		ArrayList<Double> drifts = SDESystemForStocks.getDriftsOfStocks();
		
		ArrayList<Double> squareVolatilities = SDESystemForStocks.getVolatilitiesOfStocks();
		
		ArrayList<ArrayList<Double>> covolatilityMatrixSqrt = SDESystemForStocks.getCovolatilityMatrixSqrt();
		
		ArrayList<Double> nDimensionalGaussianSample = generateNDimensionalGaussionSample(Sj.size());
		
		Sjplus1 = vectorAdd(vectorAdd(Sj, scaleVector(hadamardProduct(vectorAdd(drifts, scaleVector(squareVolatilities, 0.5)), Sj), 1.0/(250 * upSampleFactor))), 
				hadamardProduct(scaleVector(Sj, 1.0 / Math.sqrt((250 * upSampleFactor))), matrixVectorProduct(covolatilityMatrixSqrt, nDimensionalGaussianSample)));
		
		return Sjplus1;
	}

	private ArrayList<Double> generateNDimensionalGaussionSample(int dimensionality) 
	{
		ArrayList<Double> nDimensionalGaussianSample = new ArrayList<Double>();
		
		for (int i = 0; i < dimensionality; i++)
		{
			nDimensionalGaussianSample.add(this.gaussianSampler.nextGaussian());
		}
		
		return nDimensionalGaussianSample;
	}

	private ArrayList<Double> vectorAdd(ArrayList<Double> leftVector, ArrayList<Double> rightVector) 
	{
		ArrayList<Double> sum = new ArrayList<Double>();
		
		for(int i = 0; i < leftVector.size(); i++)
		{
			sum.add(leftVector.get(i) + rightVector.get(i));
		}
		
		return sum;
	}

	private ArrayList<Double> scaleVector(ArrayList<Double> vector, Double scaleFactor) 
	{
		ArrayList<Double> scaled = new ArrayList<Double>();
		
		for(int i = 0; i < vector.size(); i++)
		{
			scaled.add(vector.get(i) * scaleFactor);
		}
		
		return scaled;
	}
	
	private ArrayList<ArrayList<Double>> scaleMatrix(ArrayList<ArrayList<Double>> vector, Double scaleFactor) 
	{
		ArrayList<ArrayList<Double>> scaled = new ArrayList<ArrayList<Double>>();
		
		for(int j = 0; j < vector.size(); j++)
		{
			scaled.add(new ArrayList<Double>());
			for(int i = 0; i < vector.get(j).size(); i++)
			{
				scaled.get(j).add(vector.get(j).get(i) * scaleFactor);
			}
		}
		
		return scaled;
	}

	public ArrayList<Double> hadamardProduct(ArrayList<Double> leftVector, ArrayList<Double> rightVector)
	{
		ArrayList<Double> hadamardProduct = new ArrayList<Double>();
		
		for(int i = 0; i < leftVector.size(); i++)
		{
			hadamardProduct.add(leftVector.get(i) * rightVector.get(i));
		}
		
		return hadamardProduct;
	}
	
	private ArrayList<ArrayList<Double>> matrixProduct(ArrayList<ArrayList<Double>> m1,
			ArrayList<ArrayList<Double>> m2) 
	{
		double[][] basicM1 = SDESystem.convertToBasicArrayMatrix(m1);
		double[][] basicM2 = SDESystem.convertToBasicArrayMatrix(m2);
		
		Matrix matrixM1 = new Matrix(basicM1);
		Matrix matrixM2 = new Matrix(basicM2);
		
		Matrix product = matrixM1.times(matrixM2);
		
		ArrayList<ArrayList<Double>> productAsArrayList = SDESystem.convertToArrayListMatrix(product);
		
		return productAsArrayList;
	}
	
	private ArrayList<Double> matrixVectorProduct(ArrayList<ArrayList<Double>> m1,
	ArrayList<Double> m2)
	{
		double[][] basicM1 = SDESystem.convertToBasicArrayMatrix(m1);
		double[][] basicM2 = new double[][] {SDESystem.convertToBasicArray(m2)};
		
		Matrix matrixM1 = new Matrix(basicM1);
		Matrix matrixM2 = new Matrix(basicM2);
		
		Matrix product = matrixM2.times(matrixM1);
		
		ArrayList<Double> productAsArrayList = SDESystem.convertToArrayList(product);
		
		return productAsArrayList;
	}

	public ArrayList<ArrayList<Double>> getDailyPrices() 
	{
		return dailyPrices;
	}
}
