/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.marketdata.spec;

import java.util.Objects;

import com.opengamma.util.ArgumentChecker;

/**
 * Specifies a source of market data that decorates an underlying source and ticks at random intervals with random
 * perturbations.
 */
public class RandomizingMarketDataSpecification extends MarketDataSpecification {

  private final MarketDataSpecification _underlying;
  private final double _updateProbability;
  private final int _maxPercentageChange;
  private final long _averageCyclePeriod;

  /**
   * @param underlying Specification of the underlying source of market data
   * @param updateProbability Probability of a value updating during a cycle
   * @param maxPercentageChange The maximum percentage change of any value in a single cycle
   * @param averageCycleInterval The average interval between calculation cycles in milliseconds. The actual interval
   * is a random value +/- 50% of this value.
   */
  public RandomizingMarketDataSpecification(MarketDataSpecification underlying,
                                            double updateProbability,
                                            int maxPercentageChange,
                                            long averageCycleInterval) {
    ArgumentChecker.notNull(underlying, "underlying");
    ArgumentChecker.isInRangeInclusive(0, 1, updateProbability);
    ArgumentChecker.notNegative(maxPercentageChange, "maxPercentageChange");
    ArgumentChecker.notNegativeOrZero(averageCycleInterval, "averageCycleInterval");
    _underlying = underlying;
    _updateProbability = updateProbability;
    _maxPercentageChange = maxPercentageChange;
    _averageCyclePeriod = averageCycleInterval;
  }

  public MarketDataSpecification getUnderlying() {
    return _underlying;
  }

  public double getUpdateProbability() {
    return _updateProbability;
  }

  public int getMaxPercentageChange() {
    return _maxPercentageChange;
  }

  public long getAverageCyclePeriod() {
    return _averageCyclePeriod;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_underlying, _updateProbability, _maxPercentageChange, _averageCyclePeriod);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final RandomizingMarketDataSpecification other = (RandomizingMarketDataSpecification) obj;
    return Objects.equals(this._underlying, other._underlying) &&
        Objects.equals(this._updateProbability, other._updateProbability) &&
        Objects.equals(this._maxPercentageChange, other._maxPercentageChange) &&
        Objects.equals(this._averageCyclePeriod, other._averageCyclePeriod);
  }
}
