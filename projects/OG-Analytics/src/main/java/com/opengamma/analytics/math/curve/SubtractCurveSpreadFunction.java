/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.analytics.math.curve;

import com.opengamma.analytics.math.function.Function;
import com.opengamma.util.ArgumentChecker;

/**
 * A function that performs subtraction on each of the constituent curves.
 * <p>
 * Given a number of curves $C_1(x_{i_1}, y_{i_1}) , C_2(x_{i_2}, y_{i_2}), \ldots C_n(x_{i_n}, y_{i_n})$, returns a function $F$
 * that for a value $x$ will return:
 * $$
 * \begin{eqnarray*}
 * F(x) = C_1 |_x - C_2 |_x - \ldots - C_n |_x
 * \end{eqnarray*}
 * $$
 */
public class SubtractCurveSpreadFunction implements CurveSpreadFunction {
  private static final String NAME = "-";

  /**
   * @param curves An array of curves, not null or empty
   * @return A function that will find the value of each curve at the given input <i>x</i> and subtract each in turn
   */
  @Override
  public Function<Double, Double> evaluate(final Curve<Double, Double>... curves) {
    ArgumentChecker.notEmpty(curves, "curves");
    return new Function<Double, Double>() {

      @Override
      public Double evaluate(final Double... x) {
        ArgumentChecker.notEmpty(x, "x");
        final double x0 = x[0];
        double y = curves[0].getYValue(x0);
        for (int i = 1; i < curves.length; i++) {
          y -= curves[i].getYValue(x0);
        }
        return y;
      }

    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOperationName() {
    return NAME;
  }
}
