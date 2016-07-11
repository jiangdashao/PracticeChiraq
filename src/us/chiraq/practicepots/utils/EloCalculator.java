package us.chiraq.practicepots.utils;

public class EloCalculator
{
  public static double[] getEstimations(double rankingA, double rankingB)
  {
    double[] ret = new double[2];
    double estA = 1.0D / (1.0D + Math.pow(10.0D, (rankingB - rankingA) / 400.0D));
    double estB = 1.0D / (1.0D + Math.pow(10.0D, (rankingA - rankingB) / 400.0D));
    ret[0] = estA;
    ret[1] = estB;
    return ret;
  }
  
  public static int getConstant(int ranking)
  {
    if (ranking < 1000) {
      return 32;
    }
    if (ranking < 1401) {
      return 24;
    }
    return 16;
  }
  
  public static int[] getNewRankings(int rankingA, int rankingB, boolean victoryA)
  {
    int[] ret = new int[2];
    
    double[] ests = getEstimations(rankingA, rankingB);
    
    int newRankA = (int)(rankingA + getConstant(rankingA) * ((victoryA ? 1 : 0) - ests[0]));
    
    ret[0] = Math.round(newRankA);
    ret[1] = Math.round(rankingB - (newRankA - rankingA));
    return ret;
  }
  
}
