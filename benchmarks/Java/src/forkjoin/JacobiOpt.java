package forkjoin;

import java.util.concurrent.RecursiveTask;

import som.Benchmark;

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

// Parallelized, and one of the tasks is done locally.

/*
 * Vivek Kumar: Ported to JavaTC work-asyncing.
 */
public final class JacobiOpt extends Benchmark {

  // Assuming STEPS==10 and DEFAULT_GRANULARITY==2
  private final static double[] result_1024_10_2    = { 0.03532437858581544D,
      0.017849813902944933D, 0.01192949360929535D, 0.008988350379449561D,
      0.007195258835605234D, 0.005983223755848366D, 0.005145446751888216D,
      0.004491799673319474D, 0.0040017586597708155D, 0.0035942981873005597D,
      0.0032725732667924223D, 0.0029970514366798318D, 0.0027653688264400733D,
      0.0025724189553933963D, 0.002397642724590421D };
  private static final int      DEFAULT_GRANULARITY = 2;
  private static final int      STEPS               = 10;
  private static final double   EPSILON             = 0.0001d;

  @Override
  public boolean innerBenchmarkLoop(final int n) {
    int dim = n + 2;
    double[][] a = new double[dim][dim];
    double[][] b = new double[dim][dim];

    for (int i = 1; i < dim - 1; ++i) {
      for (int j = 1; j < dim - 1; ++j) {
        a[i][j] = EPSILON;
      }
    }

    for (int k = 0; k < dim; ++k) {
      a[k][0] = 1.0d;
      a[k][n + 1] = 1.0d;
      a[0][k] = 1.0d;
      a[n + 1][k] = 1.0d;
      b[k][0] = 1.0d;
      b[k][n + 1] = 1.0d;
      b[0][k] = 1.0d;
      b[n + 1][k] = 1.0d;
    }

    double df = 0.0D;
    for (int x = 0; x < STEPS; ++x) {
      df = new BuildNode(a, b, 1, n, 1, n, DEFAULT_GRANULARITY, x).compute();
    }

    return verifyResult(df, n);
  }

  @Override
  public Object benchmark() {
    throw new RuntimeException("Should never be reached");
  }

  @Override
  public boolean verifyResult(final Object result) {
    throw new RuntimeException("Should never be reached");
  }

  private static boolean verifyResult(final double df, final int n) {
    // original benchmark does repeated iterations on the same data
    // we don't do that to have more predictable behavior
    int iter = 0;
    if (n == 1024) {
      return df == result_1024_10_2[iter];
    }
    System.out.println("No expected result for n=" + n);
    return false;
  }

  private static class BuildNode extends RecursiveTask<Double> {
    private static final long serialVersionUID = -8076979977697518646L;

    private final double[][] a;
    private final double[][] b;
    private final int        lr;
    private final int        hr;
    private final int        lc;
    private final int        hc;
    private final int        leafs;
    private final int        steps;

    BuildNode(final double[][] a, final double[][] b, final int lr,
        final int hr, final int lc, final int hc, final int leafs,
        final int steps) {
      this.a = a;
      this.b = b;
      this.lr = lr;
      this.hr = hr;
      this.lc = lc;
      this.hc = hc;
      this.leafs = leafs;
      this.steps = steps;
    }

    @Override
    protected Double compute() {
      int rows = hr - lr + 1;
      int cols = hc - lc + 1;
      int mr = (lr + hr) >>> 1;
      int mc = (lc + hc) >>> 1;
      int hrows = mr - lr + 1;
      int hcols = mc - lc + 1;

      if (rows * cols <= leafs) {
        return processLeafNode(a, b, lr, hr, lc, hc, steps);
      } else if (hrows * hcols >= leafs) {
        BuildNode task1 = new BuildNode(a, b, lr, mr, lc, mc, leafs, steps);
        task1.fork();
        BuildNode task2 = new BuildNode(a, b, lr, mr, mc + 1, hc, leafs, steps);
        task2.fork();
        BuildNode task3 = new BuildNode(a, b, mr + 1, hr, lc, mc, leafs, steps);
        task3.fork();

        double df4 = new BuildNode(a, b, mr + 1, hr, mc + 1, hc, leafs, steps).compute();

        double df1 = task1.join();
        double df2 = task2.join();
        double df3 = task3.join();

        return ((((df1 > df2) ? df1 : df2) > df3 ? ((df1 > df2) ? df1 : df2)
            : df3) > df4)
                ? (((df1 > df2) ? df1 : df2) > df3 ? ((df1 > df2) ? df1 : df2)
                    : df3)
                : df4;
      } else if (cols >= rows) {
        BuildNode task1 = new BuildNode(a, b, lr, hr, lc, mc, leafs, steps);
        task1.fork();

        double df2 = new BuildNode(a, b, lr, hr, mc + 1, hc, leafs, steps).compute();
        double df1 = task1.join();

        return ((df1 > df2) ? df1 : df2);
      } else {
        BuildNode task1 = new BuildNode(a, b, lr, mr, lc, hc, leafs, steps);
        task1.fork();

        double df2 = new BuildNode(a, b, mr + 1, hr, lc, hc, leafs, steps).compute();
        double df1 = task1.join();

        return ((df1 > df2) ? df1 : df2);
      }
    }
  }

  private static double processLeafNode(final double[][] A, final double[][] B,
      final int loRow, final int hiRow, final int loCol, final int hiCol,
      final int steps) {
    boolean AtoB = (steps & 1) == 0;
    double[][] a = AtoB ? A : B;
    double[][] b = AtoB ? B : A;

    double md = 0.0d;

    for (int i = loRow; i <= hiRow; ++i) {
      for (int j = loCol; j <= hiCol; ++j) {
        double v = 0.25d
            * (a[i - 1][j] + a[i][j - 1] + a[i + 1][j] + a[i][j + 1]);
        b[i][j] = v;

        double diff = v - a[i][j];
        if (diff < 0) {
          diff = -diff;
        }

        if (diff > md) {
          md = diff;
        }
      }
    }
    return md;
  }
}
