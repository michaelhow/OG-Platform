/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.instrument.inflation;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.Test;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.index.IndexPrice;
import com.opengamma.analytics.financial.interestrate.inflation.derivative.CouponInflationZeroCouponInterpolation;
import com.opengamma.analytics.financial.interestrate.payments.derivative.Coupon;
import com.opengamma.analytics.financial.interestrate.payments.derivative.CouponFixed;
import com.opengamma.analytics.financial.provider.description.MulticurveProviderDiscountDataSets;
import com.opengamma.analytics.financial.schedule.ScheduleCalculator;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventionFactory;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.timeseries.DoubleTimeSeries;
import com.opengamma.timeseries.precise.zdt.ImmutableZonedDateTimeDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtils;

/**
 * Tests the zero-coupon inflation constructors.
 */
public class CouponInflationZeroCouponInterpolationDefinitionTest {
  private static final IndexPrice[] PRICE_INDEXES = MulticurveProviderDiscountDataSets.getPriceIndexes();
  private static final IndexPrice PRICE_INDEX_EUR = PRICE_INDEXES[0];
  private static final Currency CUR = PRICE_INDEX_EUR.getCurrency();
  private static final Calendar CALENDAR = new MondayToFridayCalendar("A");
  private static final BusinessDayConvention BUSINESS_DAY = BusinessDayConventionFactory.INSTANCE.getBusinessDayConvention("Modified Following");
  private static final boolean EOM = true;
  private static final ZonedDateTime START_DATE = DateUtils.getUTCDate(2008, 8, 18);
  private static final int COUPON_TENOR_YEAR = 10;
  private static final Period COUPON_TENOR = Period.ofYears(COUPON_TENOR_YEAR);
  private static final ZonedDateTime PAYMENT_DATE = ScheduleCalculator.getAdjustedDate(START_DATE, COUPON_TENOR, BUSINESS_DAY, CALENDAR, EOM);
  private static final ZonedDateTime ACCRUAL_END_DATE = PAYMENT_DATE.minusDays(1); // For getter test
  private static final double NOTIONAL = 98765432;
  private static final int MONTH_LAG = 3;
  private static final double INDEX_MAY_2008 = 108.45483870967742; // May index: 108.23 - June Index = 108.64
  private static final ZonedDateTime REFERENCE_START_DATE = DateUtils.getUTCDate(2008, 5, 18);
  private static final ZonedDateTime[] REFERENCE_END_DATE = new ZonedDateTime[2];
  static {
    REFERENCE_END_DATE[0] = PAYMENT_DATE.minusMonths(MONTH_LAG).withDayOfMonth(1);
    REFERENCE_END_DATE[1] = PAYMENT_DATE.minusMonths(MONTH_LAG - 1).withDayOfMonth(1);
  }
  final static double WEIGHT = 1.0 - (PAYMENT_DATE.getDayOfMonth() - 1.0) / PAYMENT_DATE.toLocalDate().lengthOfMonth();
  private static final CouponInflationZeroCouponInterpolationDefinition ZERO_COUPON_DEFINITION = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE,
      1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
  private static final String DISCOUNTING_CURVE_NAME = "Discounting";
  private static final String PRICE_INDEX_CURVE_NAME = "Price index";
  private static final String[] CURVE_NAMES = new String[] {DISCOUNTING_CURVE_NAME, PRICE_INDEX_CURVE_NAME };
  //  private static final DayCount ACT_ACT = DayCountFactory.INSTANCE.getDayCount("Actual/Actual ISDA");
  private static final DoubleTimeSeries<ZonedDateTime> HICPX_TS = MulticurveProviderDiscountDataSets.euroHICPXFrom2009();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullCurrency() {
    new CouponInflationZeroCouponInterpolationDefinition(null, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE,
        INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullPay() {
    new CouponInflationZeroCouponInterpolationDefinition(CUR, null, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE, INDEX_MAY_2008,
        REFERENCE_END_DATE, WEIGHT, false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullStart() {
    new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, null, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE,
        INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEnd() {
    new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, null, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE, INDEX_MAY_2008,
        REFERENCE_END_DATE, WEIGHT, false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullIndex() {
    new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, null, MONTH_LAG, 3, REFERENCE_START_DATE, INDEX_MAY_2008,
        REFERENCE_END_DATE, WEIGHT, false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRefStart() {
    new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, null, INDEX_MAY_2008,
        REFERENCE_END_DATE, WEIGHT, false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRefEnd() {
    new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE, INDEX_MAY_2008,
        null, WEIGHT, false);
  }

  @Test
  /**
   * Tests the class getter.
   */
  public void getter() {
    assertEquals("Inflation Zero-coupon: getter", CUR, ZERO_COUPON_DEFINITION.getCurrency());
    assertEquals("Inflation Zero-coupon: getter", PAYMENT_DATE, ZERO_COUPON_DEFINITION.getPaymentDate());
    assertEquals("Inflation Zero-coupon: getter", START_DATE, ZERO_COUPON_DEFINITION.getAccrualStartDate());
    assertEquals("Inflation Zero-coupon: getter", ACCRUAL_END_DATE, ZERO_COUPON_DEFINITION.getAccrualEndDate());
    assertEquals("Inflation Zero-coupon: getter", 1.0, ZERO_COUPON_DEFINITION.getPaymentYearFraction());
    assertEquals("Inflation Zero-coupon: getter", NOTIONAL, ZERO_COUPON_DEFINITION.getNotional());
    assertEquals("Inflation Zero-coupon: getter", PRICE_INDEX_EUR, ZERO_COUPON_DEFINITION.getPriceIndex());
    assertEquals("Inflation Zero-coupon: getter", REFERENCE_START_DATE, ZERO_COUPON_DEFINITION.getReferenceStartDate());
    assertEquals("Inflation Zero-coupon: getter", REFERENCE_END_DATE, ZERO_COUPON_DEFINITION.getReferenceEndDate());
    assertEquals("Inflation Zero-coupon: getter", INDEX_MAY_2008, ZERO_COUPON_DEFINITION.getIndexStartValue());
    assertEquals("Inflation Zero-coupon: getter", WEIGHT, ZERO_COUPON_DEFINITION.getWeight());
  }

  @Test
  /**
   * Tests the equal and hash-code methods.
   */
  public void equalHash() {
    assertEquals(ZERO_COUPON_DEFINITION, ZERO_COUPON_DEFINITION);
    final CouponInflationZeroCouponInterpolationDefinition couponDuplicate = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL,
        PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    assertEquals(ZERO_COUPON_DEFINITION, couponDuplicate);
    assertEquals(ZERO_COUPON_DEFINITION.hashCode(), couponDuplicate.hashCode());
    CouponInflationZeroCouponInterpolationDefinition modified;
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, ACCRUAL_END_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3,
        REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE.minusDays(1), ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3,
        REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE.minusDays(1), 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3,
        REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3,
        REFERENCE_START_DATE.minusDays(1), INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
    final ZonedDateTime[] modifiedReference = new ZonedDateTime[2];
    modifiedReference[0] = REFERENCE_END_DATE[0];
    modifiedReference[0] = REFERENCE_END_DATE[0].minusDays(1);
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE,
        INDEX_MAY_2008, modifiedReference, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 2.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3, REFERENCE_START_DATE,
        INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL + 10, PRICE_INDEX_EUR, MONTH_LAG, 3,
        REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
    modified = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, ACCRUAL_END_DATE, 1.0, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, 3,
        REFERENCE_START_DATE, INDEX_MAY_2008 + 1.0, REFERENCE_END_DATE, WEIGHT, false);
    assertFalse(ZERO_COUPON_DEFINITION.equals(modified));
  }

  @Test
  /**
   * Tests the first builder.
   */
  public void from() {
    final CouponInflationZeroCouponInterpolationDefinition constructor = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, PAYMENT_DATE, 1.0, NOTIONAL,
        PRICE_INDEX_EUR, MONTH_LAG, 3, START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    final CouponInflationZeroCouponInterpolationDefinition from = CouponInflationZeroCouponInterpolationDefinition.from(CUR, PAYMENT_DATE, START_DATE, PAYMENT_DATE, 1.0, NOTIONAL,
        PRICE_INDEX_EUR, MONTH_LAG, START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, false);
    assertEquals("Inflation zero-coupon : from", constructor, from);
  }

  @Test
  /**
   * Tests the first builder.
   */
  public void from1() {
    final CouponInflationZeroCouponInterpolationDefinition constructor = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, PAYMENT_DATE, 1.0, NOTIONAL,
        PRICE_INDEX_EUR,
        MONTH_LAG, 3, START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    final CouponInflationZeroCouponInterpolationDefinition from = CouponInflationZeroCouponInterpolationDefinition.from(START_DATE, PAYMENT_DATE, NOTIONAL, PRICE_INDEX_EUR, MONTH_LAG, MONTH_LAG,
        INDEX_MAY_2008, REFERENCE_END_DATE, false);
    assertEquals("Inflation zero-coupon : from", constructor, from);
  }

  @Test
  /**
   * Tests the builder based on indexation lag.
   */
  public void from2() {
    final CouponInflationZeroCouponInterpolationDefinition constructor = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, PAYMENT_DATE, 1.0, NOTIONAL,
        PRICE_INDEX_EUR,
        MONTH_LAG, 3, REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    final CouponInflationZeroCouponInterpolationDefinition from = CouponInflationZeroCouponInterpolationDefinition.from(START_DATE, PAYMENT_DATE, NOTIONAL, PRICE_INDEX_EUR, INDEX_MAY_2008, MONTH_LAG,
        MONTH_LAG, false);
    assertEquals("Inflation zero-coupon : from", constructor, from);
  }

  @Test
  /**
   * Tests the builder based on price index series.
   */
  public void from3() {
    final CouponInflationZeroCouponInterpolationDefinition constructor = new CouponInflationZeroCouponInterpolationDefinition(CUR, PAYMENT_DATE, START_DATE, PAYMENT_DATE, 1.0, NOTIONAL,
        PRICE_INDEX_EUR, MONTH_LAG, MONTH_LAG, REFERENCE_START_DATE, INDEX_MAY_2008, REFERENCE_END_DATE, WEIGHT, false);
    final CouponInflationZeroCouponInterpolationDefinition from = CouponInflationZeroCouponInterpolationDefinition
        .from(START_DATE, PAYMENT_DATE, NOTIONAL, PRICE_INDEX_EUR, HICPX_TS, MONTH_LAG, MONTH_LAG, false);
    assertEquals("Inflation zero-coupon : from", constructor, from);
  }

  @Test
  public void toDerivativesNoData() {
    final ZonedDateTime pricingDate = DateUtils.getUTCDate(2011, 7, 29);
    final Coupon zeroCouponConverted = ZERO_COUPON_DEFINITION.toDerivative(pricingDate, CURVE_NAMES);
    final double paymentTime = TimeCalculator.getTimeBetween(pricingDate, PAYMENT_DATE);
    final double[] referenceEndTime = new double[2];
    referenceEndTime[0] = TimeCalculator.getTimeBetween(pricingDate, REFERENCE_END_DATE[0]);
    referenceEndTime[1] = TimeCalculator.getTimeBetween(pricingDate, REFERENCE_END_DATE[1]);
    final double naturalPaymentPaymentTime = TimeCalculator.getTimeBetween(pricingDate, PAYMENT_DATE);
    CouponInflationZeroCouponInterpolation zeroCoupon = new CouponInflationZeroCouponInterpolation(CUR, paymentTime, 1.0, NOTIONAL, PRICE_INDEX_EUR, INDEX_MAY_2008, referenceEndTime,
        naturalPaymentPaymentTime, ZERO_COUPON_DEFINITION.getWeight(), false);
    assertEquals("Inflation zero-coupon: toDerivative", zeroCouponConverted, zeroCoupon);
  }

  @Test
  public void toDerivativesInterpolatedKnown() {
    final ZonedDateTime pricingDate = DateUtils.getUTCDate(2018, 7, 25);
    final DoubleTimeSeries<ZonedDateTime> priceIndexTS = ImmutableZonedDateTimeDoubleTimeSeries.ofUTC(new ZonedDateTime[] {DateUtils.getUTCDate(2018, 5, 1), DateUtils.getUTCDate(2018, 6, 1) },
        new double[] {
            128.23, 128.43 });
    final CouponInflationZeroCouponInterpolationDefinition zeroCouponInterpolated = CouponInflationZeroCouponInterpolationDefinition.from(START_DATE, PAYMENT_DATE, NOTIONAL, PRICE_INDEX_EUR,
        INDEX_MAY_2008, MONTH_LAG, MONTH_LAG, false);
    final Coupon zeroCouponConverted = zeroCouponInterpolated.toDerivative(pricingDate, priceIndexTS, CURVE_NAMES);
    final double paymentTime = TimeCalculator.getTimeBetween(pricingDate, PAYMENT_DATE);
    final double endIndex = 128.23 + (PAYMENT_DATE.getDayOfMonth() - 1.0) / (PAYMENT_DATE.toLocalDate().lengthOfMonth()) * (128.43 - 128.23);
    final CouponFixed zeroCoupon = new CouponFixed(CUR, paymentTime, DISCOUNTING_CURVE_NAME, 1.0, NOTIONAL, endIndex / INDEX_MAY_2008 - 1.0);
    assertEquals("Inflation zero-coupon: toDerivative", zeroCoupon, zeroCouponConverted);
  }

  @Test
  public void toDerivativesInterpolatedOneKnown() {
    final ZonedDateTime pricingDate = DateUtils.getUTCDate(2018, 6, 25);
    final DoubleTimeSeries<ZonedDateTime> priceIndexTS = ImmutableZonedDateTimeDoubleTimeSeries.ofUTC(new ZonedDateTime[] {DateUtils.getUTCDate(2018, 4, 1), DateUtils.getUTCDate(2018, 5, 1) },
        new double[] {
            128.03, 128.23 });
    final CouponInflationZeroCouponInterpolationDefinition zeroCouponInterpolated = CouponInflationZeroCouponInterpolationDefinition.from(START_DATE, PAYMENT_DATE, NOTIONAL, PRICE_INDEX_EUR,
        INDEX_MAY_2008, MONTH_LAG, MONTH_LAG, false);
    final Coupon zeroCouponConverted = zeroCouponInterpolated.toDerivative(pricingDate, priceIndexTS, CURVE_NAMES);
    final double paymentTime = TimeCalculator.getTimeBetween(pricingDate, PAYMENT_DATE);
    final double[] referenceEndTime = new double[2];
    referenceEndTime[0] = -TimeCalculator.getTimeBetween(REFERENCE_END_DATE[0], pricingDate);
    referenceEndTime[1] = -TimeCalculator.getTimeBetween(REFERENCE_END_DATE[1], pricingDate);
    final double naturalPaymentPaymentTime = TimeCalculator.getTimeBetween(pricingDate, PAYMENT_DATE);
    CouponInflationZeroCouponInterpolation zeroCoupon = new CouponInflationZeroCouponInterpolation(CUR, paymentTime, 1.0, NOTIONAL, PRICE_INDEX_EUR, INDEX_MAY_2008, referenceEndTime,
        naturalPaymentPaymentTime, ZERO_COUPON_DEFINITION.getWeight(), false);
    assertEquals("Inflation zero-coupon: toDerivative", zeroCoupon, zeroCouponConverted);
  }

  @Test
  public void toDerivativesInterpolatedShouldBeKnown() {
    final ZonedDateTime pricingDate = DateUtils.getUTCDate(2018, 7, 25);
    final DoubleTimeSeries<ZonedDateTime> priceIndexTS = ImmutableZonedDateTimeDoubleTimeSeries.ofUTC(new ZonedDateTime[] {DateUtils.getUTCDate(2018, 4, 1), DateUtils.getUTCDate(2018, 5, 1) },
        new double[] {
            128.03, 128.23 });
    final CouponInflationZeroCouponInterpolationDefinition zeroCouponInterpolated = CouponInflationZeroCouponInterpolationDefinition.from(START_DATE, PAYMENT_DATE, NOTIONAL, PRICE_INDEX_EUR,
        INDEX_MAY_2008, MONTH_LAG, MONTH_LAG, false);
    final Coupon zeroCouponConverted = zeroCouponInterpolated.toDerivative(pricingDate, priceIndexTS, CURVE_NAMES);
    final double paymentTime = TimeCalculator.getTimeBetween(pricingDate, PAYMENT_DATE);
    final double[] referenceEndTime = new double[2];
    referenceEndTime[0] = -TimeCalculator.getTimeBetween(REFERENCE_END_DATE[0], pricingDate);
    referenceEndTime[1] = -TimeCalculator.getTimeBetween(REFERENCE_END_DATE[1], pricingDate);
    final double naturalPaymentPaymentTime = TimeCalculator.getTimeBetween(pricingDate, PAYMENT_DATE);
    CouponInflationZeroCouponInterpolation zeroCoupon = new CouponInflationZeroCouponInterpolation(CUR, paymentTime, 1.0, NOTIONAL, PRICE_INDEX_EUR, INDEX_MAY_2008, referenceEndTime,
        naturalPaymentPaymentTime, ZERO_COUPON_DEFINITION.getWeight(), false);
    assertEquals("Inflation zero-coupon: toDerivative", zeroCoupon, zeroCouponConverted);
  }

}
