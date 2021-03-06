/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.curve.interestrate.sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;

import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.interestrate.InterestRateCurveSensitivity;
import com.opengamma.analytics.financial.interestrate.YieldCurveBundle;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.DoublesPair;

/**
 * For an instrument, computes the sensitivity of a value (often the present value or a par spread) to the parameters used in the curve.
 * The meaning of "parameters" will depend of the way the curve is stored (interpolated yield, function parameters, etc.) and also on the way
 * the parameters sensitivities are aggregated (the same parameter can be used in several curves).
 */
public abstract class AbstractParameterSensitivityCalculator {

  /**
   * The sensitivity calculator to compute the sensitivity of the value with respect to the zero-coupon continuously compounded rates at different times.
   */
  private final InstrumentDerivativeVisitor<YieldCurveBundle, InterestRateCurveSensitivity> _curveSensitivityCalculator;

  /**
   * The constructor from a curve sensitivity calculator.
   * @param curveSensitivityCalculator The calculator.
   */
  public AbstractParameterSensitivityCalculator(final InstrumentDerivativeVisitor<YieldCurveBundle, InterestRateCurveSensitivity> curveSensitivityCalculator) {
    ArgumentChecker.notNull(curveSensitivityCalculator, "Sensitivity calculator");
    _curveSensitivityCalculator = curveSensitivityCalculator;
  }

  /**
   * Computes the sensitivity with respect to the parameters.
   * @param instrument The instrument. Not null.
   * @param fixedCurves The fixed curves names (for which the parameter sensitivity are not computed even if they are necessary for the instrument pricing).
   * The curve in the list may or may not be in the bundle. Not null.
   * @param bundle The curve bundle with all curves and data required to price the instrument.
   * The sensitivity with respect to the curves in the fixedCurves list will not be part of the output total sensitivity. Not null.
   * @return The sensitivity (as a DoubleMatrix1D).
   */
  public DoubleMatrix1D calculateSensitivity(final InstrumentDerivative instrument, final Set<String> fixedCurves, final YieldCurveBundle bundle) {
    Validate.notNull(instrument, "null InterestRateDerivative");
    Validate.notNull(fixedCurves, "null set of fixed curves.");
    Validate.notNull(bundle, "null bundle");
    InterestRateCurveSensitivity sensitivity = instrument.accept(_curveSensitivityCalculator, bundle);
    sensitivity = sensitivity.cleaned(); // TODO: for testing purposes mainly. Could be removed after the tests.
    return pointToParameterSensitivity(sensitivity, fixedCurves, bundle);
  }

  /**
   * Computes the sensitivity with respect to the parameters from the point sensitivities to the continuously compounded rate.
   * @param sensitivity The point sensitivity.
   * @param fixedCurves The fixed curves names (for which the parameter sensitivity are not computed even if they are necessary for the instrument pricing).
   * The curve in the list may or may not be in the bundle. Not null.
   * @param bundle The curve bundle with all the curves with respect to which the sensitivity should be computed. Not null.
   * @return The sensitivity (as a DoubleMatrix1D).
   */
  public abstract DoubleMatrix1D pointToParameterSensitivity(final InterestRateCurveSensitivity sensitivity, final Set<String> fixedCurves, final YieldCurveBundle bundle);

  /**
   * Computes the sensitivity with respect to the parameters from the point sensitivities to only one curve.
   * @param sensitivity The point sensitivity with respect to the given curve.
   * @param curve The curve.
   * @return The sensitivity (as a list of doubles).
   */
  public List<Double> pointToParameterSensitivity(final List<DoublesPair> sensitivity, final YieldAndDiscountCurve curve) {
    final List<Double> result = new ArrayList<>();
    if (sensitivity != null && sensitivity.size() > 0) {
      final double[][] sensitivityYP = new double[sensitivity.size()][];
      // Implementation note: Sensitivity of the interpolated yield to the parameters
      int k = 0;
      for (final DoublesPair timeAndS : sensitivity) {
        sensitivityYP[k++] = curve.getInterestRateParameterSensitivity(timeAndS.getFirst());
      }
      for (int j = 0; j < sensitivityYP[0].length; j++) {
        double temp = 0.0;
        k = 0;
        for (final DoublesPair timeAndS : sensitivity) {
          temp += timeAndS.getSecond() * sensitivityYP[k++][j];
        }
        result.add(temp);
      }
    } else {
      for (int i = 0; i < curve.getNumberOfParameters(); i++) {
        result.add(0.0);
      }
    }
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _curveSensitivityCalculator.hashCode();
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final AbstractParameterSensitivityCalculator other = (AbstractParameterSensitivityCalculator) obj;
    return ObjectUtils.equals(_curveSensitivityCalculator, other._curveSensitivityCalculator);
  }

}
