/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.analytics.financial.provider.curve;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.threeten.bp.Period;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorCurveYieldInterpolated;
import com.opengamma.analytics.financial.curve.interestrate.generator.GeneratorYDCurve;
import com.opengamma.analytics.financial.forex.method.FXMatrix;
import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.instrument.cash.CashDefinition;
import com.opengamma.analytics.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureTransactionDefinition;
import com.opengamma.analytics.financial.instrument.index.GeneratorAttribute;
import com.opengamma.analytics.financial.instrument.index.GeneratorAttributeIR;
import com.opengamma.analytics.financial.instrument.index.GeneratorDepositIbor;
import com.opengamma.analytics.financial.instrument.index.GeneratorDepositON;
import com.opengamma.analytics.financial.instrument.index.GeneratorFRA;
import com.opengamma.analytics.financial.instrument.index.GeneratorInstrument;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedIbor;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedIborMaster;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedON;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedONMaster;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedONDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.ParSpreadMarketQuoteDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.discounting.PresentValueDiscountingCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.LastTimeCalculator;
import com.opengamma.analytics.financial.provider.curve.multicurve.MulticurveDiscountBuildingRepository;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MulticurveSensitivity;
import com.opengamma.analytics.financial.schedule.ScheduleCalculator;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.timeseries.precise.zdt.ImmutableZonedDateTimeDoubleTimeSeries;
import com.opengamma.timeseries.precise.zdt.ZonedDateTimeDoubleTimeSeries;
import com.opengamma.util.money.Currency;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.tuple.Pair;

/**
 * Build of curve in several blocks with relevant Jacobian matrices.
 * Three curves in EUR; no futures.
 */
public class MulticurveBuildingDiscountingDiscountEUR3Test {

  private static final Interpolator1D INTERPOLATOR_LINEAR = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR);

  private static final LastTimeCalculator MATURITY_CALCULATOR = LastTimeCalculator.getInstance();
  private static final double TOLERANCE_ROOT = 1.0E-10;
  private static final int STEP_MAX = 100;

  private static final Calendar TARGET = new MondayToFridayCalendar("TARGET");
  private static final Currency EUR = Currency.EUR;
  private static final FXMatrix FX_MATRIX = new FXMatrix(EUR);

  private static final double NOTIONAL = 1.0;

  private static final GeneratorSwapFixedON GENERATOR_OIS_EUR = GeneratorSwapFixedONMaster.getInstance().getGenerator("EUR1YEONIA", TARGET);
  private static final IndexON INDEX_ON_EUR = GENERATOR_OIS_EUR.getIndex();
  private static final GeneratorDepositON GENERATOR_DEPOSIT_ON_EUR = new GeneratorDepositON("EUR Deposit ON", EUR, TARGET, INDEX_ON_EUR.getDayCount());
  private static final GeneratorSwapFixedIborMaster GENERATOR_SWAP_MASTER = GeneratorSwapFixedIborMaster.getInstance();
  private static final GeneratorSwapFixedIbor EUR1YEURIBOR3M = GENERATOR_SWAP_MASTER.getGenerator("EUR1YEURIBOR3M", TARGET);
  private static final GeneratorSwapFixedIbor EUR1YEURIBOR6M = GENERATOR_SWAP_MASTER.getGenerator("EUR1YEURIBOR6M", TARGET);
  private static final IborIndex EURIBOR3M = EUR1YEURIBOR3M.getIborIndex();
  private static final IborIndex EURIBOR6M = EUR1YEURIBOR6M.getIborIndex();
  private static final IborIndex EUROLIBOR3M = new IborIndex(EUR, Period.ofMonths(3), 2, EURIBOR3M.getDayCount(), EURIBOR3M.getBusinessDayConvention(), true, "EUROLIBOR3M");
  private static final IborIndex EUROLIBOR6M = new IborIndex(EUR, Period.ofMonths(6), 2, EURIBOR6M.getDayCount(), EURIBOR6M.getBusinessDayConvention(), true, "EUROLIBOR6M");
  private static final GeneratorFRA GENERATOR_FRA_3M = new GeneratorFRA("GENERATOR_FRA_3M", EURIBOR3M, TARGET);
  private static final GeneratorFRA GENERATOR_FRA_6M = new GeneratorFRA("GENERATOR_FRA_6M", EURIBOR6M, TARGET);
  private static final GeneratorDepositIbor GENERATOR_EURIBOR3M = new GeneratorDepositIbor("GENERATOR_EURIBOR3M", EURIBOR3M, TARGET);
  private static final GeneratorDepositIbor GENERATOR_EURIBOR6M = new GeneratorDepositIbor("GENERATOR_EURIBOR6M", EURIBOR6M, TARGET);

  private static final ZonedDateTime NOW = DateUtils.getUTCDate(2011, 9, 28);

  private static final ZonedDateTimeDoubleTimeSeries TS_EMPTY = ImmutableZonedDateTimeDoubleTimeSeries.ofEmptyUTC();
  private static final ZonedDateTimeDoubleTimeSeries TS_ON_EUR_WITH_TODAY = ImmutableZonedDateTimeDoubleTimeSeries.ofUTC(new ZonedDateTime[] {DateUtils.getUTCDate(2011, 9, 27),
      DateUtils.getUTCDate(2011, 9, 28) }, new double[] {0.07, 0.08 });
  private static final ZonedDateTimeDoubleTimeSeries TS_ON_EUR_WITHOUT_TODAY = ImmutableZonedDateTimeDoubleTimeSeries.ofUTC(new ZonedDateTime[] {DateUtils.getUTCDate(2011, 9, 27),
      DateUtils.getUTCDate(2011, 9, 28) }, new double[] {0.07, 0.08 });
  private static final ZonedDateTimeDoubleTimeSeries[] TS_FIXED_OIS_EUR_WITH_TODAY = new ZonedDateTimeDoubleTimeSeries[] {TS_EMPTY, TS_ON_EUR_WITH_TODAY };
  private static final ZonedDateTimeDoubleTimeSeries[] TS_FIXED_OIS_EUR_WITHOUT_TODAY = new ZonedDateTimeDoubleTimeSeries[] {TS_EMPTY, TS_ON_EUR_WITHOUT_TODAY };

  private static final ZonedDateTimeDoubleTimeSeries TS_IBOR_EUR3M_WITH_TODAY = ImmutableZonedDateTimeDoubleTimeSeries.ofUTC(new ZonedDateTime[] {DateUtils.getUTCDate(2011, 9, 27),
      DateUtils.getUTCDate(2011, 9, 28) }, new double[] {0.0035, 0.0036 });
  private static final ZonedDateTimeDoubleTimeSeries TS_IBOR_EUR3M_WITHOUT_TODAY = ImmutableZonedDateTimeDoubleTimeSeries.ofUTC(new ZonedDateTime[] {DateUtils.getUTCDate(2011, 9, 27) },
      new double[] {0.0035 });

  private static final ZonedDateTimeDoubleTimeSeries[] TS_FIXED_IBOR_EUR3M_WITH_TODAY = new ZonedDateTimeDoubleTimeSeries[] {TS_IBOR_EUR3M_WITH_TODAY };
  private static final ZonedDateTimeDoubleTimeSeries[] TS_FIXED_IBOR_EUR3M_WITHOUT_TODAY = new ZonedDateTimeDoubleTimeSeries[] {TS_IBOR_EUR3M_WITHOUT_TODAY };

  private static final String CURVE_NAME_DSC_EUR = "EUR Dsc";
  private static final String CURVE_NAME_FWD3_EUR = "EUR Fwd 3M";
  private static final String CURVE_NAME_FWD6_EUR = "EUR Fwd 6M";

  /** Market values for the dsc USD curve */
  private static final double[] DSC_EUR_MARKET_QUOTES = new double[] {0.0400, 0.0400, 0.0400, 0.0400, 0.0400, 0.0400, 0.0400, 0.0400, 0.0400, 0.0400, 0.0400, 0.0400 };
  /** Generators for the dsc USD curve */
  private static final GeneratorInstrument<? extends GeneratorAttribute>[] DSC_EUR_GENERATORS = new GeneratorInstrument<?>[] {GENERATOR_DEPOSIT_ON_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR,
    GENERATOR_OIS_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR, GENERATOR_OIS_EUR };
  /** Tenors for the dsc USD curve */
  private static final Period[] DSC_EUR_TENOR = new Period[] {Period.ofDays(0), Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3),
    Period.ofMonths(6), Period.ofMonths(9), Period.ofYears(1),
    Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(10) };
  private static final GeneratorAttributeIR[] DSC_EUR_ATTR = new GeneratorAttributeIR[DSC_EUR_TENOR.length];
  static {
    for (int loopins = 0; loopins < DSC_EUR_TENOR.length; loopins++) {
      DSC_EUR_ATTR[loopins] = new GeneratorAttributeIR(DSC_EUR_TENOR[loopins]);
    }
  }

  /** Market values for the Fwd 3M USD curve */
  private static final double[] FWD3_EUR_MARKET_QUOTES = new double[] {0.0420, 0.0420, 0.0420, 0.0420, 0.0430, 0.0470, 0.0540, 0.0570, 0.0600 };
  /** Generators for the Fwd 3M USD curve */
  private static final GeneratorInstrument<? extends GeneratorAttribute>[] FWD3_EUR_GENERATORS = new GeneratorInstrument<?>[] {GENERATOR_EURIBOR3M, GENERATOR_FRA_3M, GENERATOR_FRA_3M, EUR1YEURIBOR3M,
    EUR1YEURIBOR3M, EUR1YEURIBOR3M, EUR1YEURIBOR3M, EUR1YEURIBOR3M, EUR1YEURIBOR3M };
  /** Tenors for the Fwd 3M USD curve */
  private static final Period[] FWD3_EUR_TENOR = new Period[] {Period.ofMonths(0), Period.ofMonths(6), Period.ofMonths(9), Period.ofYears(1),
    Period.ofYears(2), Period.ofYears(3), Period.ofYears(5),
    Period.ofYears(7), Period.ofYears(10) };
  private static final GeneratorAttributeIR[] FWD3_EUR_ATTR = new GeneratorAttributeIR[FWD3_EUR_TENOR.length];
  static {
    for (int loopins = 0; loopins < FWD3_EUR_TENOR.length; loopins++) {
      FWD3_EUR_ATTR[loopins] = new GeneratorAttributeIR(FWD3_EUR_TENOR[loopins]);
    }
  }

  /** Market values for the Fwd 3M USD curve */
  private static final double[] FWD6_EUR_MARKET_QUOTES = new double[] {0.0440, 0.0440, 0.0440, 0.0445, 0.0485, 0.0555, 0.0580, 0.0610 };
  /** Generators for the Fwd 3M USD curve */
  private static final GeneratorInstrument<? extends GeneratorAttribute>[] FWD6_EUR_GENERATORS = new GeneratorInstrument<?>[] {GENERATOR_EURIBOR6M, GENERATOR_FRA_6M, GENERATOR_FRA_6M, EUR1YEURIBOR6M,
    EUR1YEURIBOR6M, EUR1YEURIBOR6M, EUR1YEURIBOR6M, EUR1YEURIBOR6M };
  /** Tenors for the Fwd 3M USD curve */
  private static final Period[] FWD6_EUR_TENOR = new Period[] {Period.ofMonths(0), Period.ofMonths(9), Period.ofMonths(12), Period.ofYears(2),
    Period.ofYears(3), Period.ofYears(5), Period.ofYears(7),
    Period.ofYears(10) };
  private static final GeneratorAttributeIR[] FWD6_EUR_ATTR = new GeneratorAttributeIR[FWD6_EUR_TENOR.length];
  static {
    for (int loopins = 0; loopins < FWD6_EUR_TENOR.length; loopins++) {
      FWD6_EUR_ATTR[loopins] = new GeneratorAttributeIR(FWD6_EUR_TENOR[loopins]);
    }
  }

  /** Standard USD discounting curve instrument definitions */
  private static final InstrumentDefinition<?>[] DEFINITIONS_DSC_EUR;
  /** Standard USD Forward 3M curve instrument definitions */
  private static final InstrumentDefinition<?>[] DEFINITIONS_FWD3_EUR;
  /** Standard USD Forward 3M curve instrument definitions */
  private static final InstrumentDefinition<?>[] DEFINITIONS_FWD6_EUR;

  /** Units of curves */
  private static final int[] NB_UNITS = new int[] {3, 1 };
  private static final int NB_BLOCKS = NB_UNITS.length;
  private static final InstrumentDefinition<?>[][][][] DEFINITIONS_UNITS = new InstrumentDefinition<?>[NB_BLOCKS][][][];
  private static final GeneratorYDCurve[][][] GENERATORS_UNITS = new GeneratorYDCurve[NB_BLOCKS][][];
  private static final String[][][] NAMES_UNITS = new String[NB_BLOCKS][][];

  private static final MulticurveProviderDiscount MULTICURVE_KNOWN_DATA = new MulticurveProviderDiscount(FX_MATRIX);

  private static final LinkedHashMap<String, Currency> DSC_MAP = new LinkedHashMap<>();
  private static final LinkedHashMap<String, IndexON[]> FWD_ON_MAP = new LinkedHashMap<>();
  private static final LinkedHashMap<String, IborIndex[]> FWD_IBOR_MAP = new LinkedHashMap<>();

  static {
    DEFINITIONS_DSC_EUR = getDefinitions(DSC_EUR_MARKET_QUOTES, DSC_EUR_GENERATORS, DSC_EUR_ATTR);
    DEFINITIONS_FWD3_EUR = getDefinitions(FWD3_EUR_MARKET_QUOTES, FWD3_EUR_GENERATORS, FWD3_EUR_ATTR);
    DEFINITIONS_FWD6_EUR = getDefinitions(FWD6_EUR_MARKET_QUOTES, FWD6_EUR_GENERATORS, FWD6_EUR_ATTR);
    for (int loopblock = 0; loopblock < NB_BLOCKS; loopblock++) {
      DEFINITIONS_UNITS[loopblock] = new InstrumentDefinition<?>[NB_UNITS[loopblock]][][];
      GENERATORS_UNITS[loopblock] = new GeneratorYDCurve[NB_UNITS[loopblock]][];
      NAMES_UNITS[loopblock] = new String[NB_UNITS[loopblock]][];
    }
    DEFINITIONS_UNITS[0][0] = new InstrumentDefinition<?>[][] {DEFINITIONS_DSC_EUR };
    DEFINITIONS_UNITS[0][1] = new InstrumentDefinition<?>[][] {DEFINITIONS_FWD3_EUR };
    DEFINITIONS_UNITS[0][2] = new InstrumentDefinition<?>[][] {DEFINITIONS_FWD6_EUR };
    DEFINITIONS_UNITS[1][0] = new InstrumentDefinition<?>[][] {DEFINITIONS_DSC_EUR, DEFINITIONS_FWD3_EUR, DEFINITIONS_FWD6_EUR };
    final GeneratorYDCurve genIntLin = new GeneratorCurveYieldInterpolated(MATURITY_CALCULATOR, INTERPOLATOR_LINEAR);
    GENERATORS_UNITS[0][0] = new GeneratorYDCurve[] {genIntLin };
    GENERATORS_UNITS[0][1] = new GeneratorYDCurve[] {genIntLin };
    GENERATORS_UNITS[0][2] = new GeneratorYDCurve[] {genIntLin };
    GENERATORS_UNITS[1][0] = new GeneratorYDCurve[] {genIntLin, genIntLin, genIntLin };
    NAMES_UNITS[0][0] = new String[] {CURVE_NAME_DSC_EUR };
    NAMES_UNITS[0][1] = new String[] {CURVE_NAME_FWD3_EUR };
    NAMES_UNITS[0][2] = new String[] {CURVE_NAME_FWD6_EUR };
    NAMES_UNITS[1][0] = new String[] {CURVE_NAME_DSC_EUR, CURVE_NAME_FWD3_EUR, CURVE_NAME_FWD6_EUR };
    DSC_MAP.put(CURVE_NAME_DSC_EUR, EUR);
    FWD_ON_MAP.put(CURVE_NAME_DSC_EUR, new IndexON[] {INDEX_ON_EUR });
    FWD_IBOR_MAP.put(CURVE_NAME_FWD3_EUR, new IborIndex[] {EURIBOR3M, EUROLIBOR3M });
    FWD_IBOR_MAP.put(CURVE_NAME_FWD6_EUR, new IborIndex[] {EURIBOR6M, EUROLIBOR6M });
  }

  private static final String NOT_USED = "Not used";
  private static final String[] NOT_USED_2 = {NOT_USED, NOT_USED };

  public static InstrumentDefinition<?>[] getDefinitions(final double[] marketQuotes, final GeneratorInstrument[] generators, final GeneratorAttribute[] attribute) {
    final InstrumentDefinition<?>[] definitions = new InstrumentDefinition<?>[marketQuotes.length];
    for (int loopmv = 0; loopmv < marketQuotes.length; loopmv++) {
      definitions[loopmv] = generators[loopmv].generateInstrument(NOW, marketQuotes[loopmv], NOTIONAL, attribute[loopmv]);
    }
    return definitions;
  }

  private static List<Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle>> CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK = new ArrayList<>();

  // Calculator
  private static final PresentValueDiscountingCalculator PVDC = PresentValueDiscountingCalculator.getInstance();
  private static final ParSpreadMarketQuoteDiscountingCalculator PSMQDC = ParSpreadMarketQuoteDiscountingCalculator.getInstance();
  private static final ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator PSMQCSDC = ParSpreadMarketQuoteCurveSensitivityDiscountingCalculator.getInstance();

  private static final MulticurveDiscountBuildingRepository CURVE_BUILDING_REPOSITORY = new MulticurveDiscountBuildingRepository(TOLERANCE_ROOT, TOLERANCE_ROOT, STEP_MAX);

  private static final double TOLERANCE_CAL = 1.0E-9;

  @BeforeSuite
  static void initClass() {
    for (int loopblock = 0; loopblock < NB_BLOCKS; loopblock++) {
      CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK.add(makeCurvesFromDefinitions(DEFINITIONS_UNITS[loopblock], GENERATORS_UNITS[loopblock], NAMES_UNITS[loopblock], MULTICURVE_KNOWN_DATA, PSMQDC,
          PSMQCSDC, false));
    }
  }

  @Test
  public void curveConstruction() {
    for (int loopblock = 0; loopblock < NB_BLOCKS; loopblock++) {
      curveConstructionTest(DEFINITIONS_UNITS[loopblock], CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK.get(loopblock).getFirst(), false, loopblock);
    }
    assertEquals("Curve construction", CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK.get(0).getFirst().getCurve(EURIBOR3M), CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK.get(0).getFirst().getCurve(EUROLIBOR3M));
  }

  @Test(enabled = false)
  public void comparison1Unit3Units() {
    final MulticurveProviderDiscount[] units = new MulticurveProviderDiscount[2];
    final CurveBuildingBlockBundle[] bb = new CurveBuildingBlockBundle[2];
    final YieldAndDiscountCurve[] curveDsc = new YieldAndDiscountCurve[2];
    final YieldAndDiscountCurve[] curveFwd3 = new YieldAndDiscountCurve[2];
    final YieldAndDiscountCurve[] curveFwd6 = new YieldAndDiscountCurve[2];
    for (int loopblock = 0; loopblock < 2; loopblock++) {
      units[loopblock] = CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK.get(loopblock).getFirst();
      bb[loopblock] = CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK.get(loopblock).getSecond();
      curveDsc[loopblock] = units[loopblock].getCurve(EUR);
      curveFwd3[loopblock] = units[loopblock].getCurve(EURIBOR3M);
      curveFwd6[loopblock] = units[loopblock].getCurve(EURIBOR6M);
    }
    assertEquals("Curve construction: 1 unit / 3 units ", curveDsc[0].getNumberOfParameters(), curveDsc[1].getNumberOfParameters());
    assertEquals("Curve construction: 1 unit / 3 units ", curveFwd3[0].getNumberOfParameters(), curveFwd3[1].getNumberOfParameters());
    assertEquals("Curve construction: 1 unit / 3 units ", curveFwd6[0].getNumberOfParameters(), curveFwd6[1].getNumberOfParameters());
    assertArrayEquals("Curve construction: 1 unit / 3 units ", ArrayUtils.toPrimitive(((YieldCurve) curveDsc[0]).getCurve().getXData()),
        ArrayUtils.toPrimitive(((YieldCurve) curveDsc[1]).getCurve().getXData()), TOLERANCE_CAL);
    assertArrayEquals("Curve construction: 1 unit / 3 units ", ArrayUtils.toPrimitive(((YieldCurve) curveDsc[0]).getCurve().getYData()),
        ArrayUtils.toPrimitive(((YieldCurve) curveDsc[1]).getCurve().getYData()), TOLERANCE_CAL);
    assertArrayEquals("Curve construction: 1 unit / 3 units ", ArrayUtils.toPrimitive(((YieldCurve) curveFwd3[0]).getCurve().getXData()),
        ArrayUtils.toPrimitive(((YieldCurve) curveFwd3[1]).getCurve().getXData()), TOLERANCE_CAL);
    assertArrayEquals("Curve construction: 1 unit / 3 units ", ArrayUtils.toPrimitive(((YieldCurve) curveFwd3[0]).getCurve().getYData()),
        ArrayUtils.toPrimitive(((YieldCurve) curveFwd3[1]).getCurve().getYData()), TOLERANCE_CAL);
    assertArrayEquals("Curve construction: 1 unit / 3 units ", ArrayUtils.toPrimitive(((YieldCurve) curveFwd6[0]).getCurve().getXData()),
        ArrayUtils.toPrimitive(((YieldCurve) curveFwd6[1]).getCurve().getXData()), TOLERANCE_CAL);
    assertArrayEquals("Curve construction: 1 unit / 3 units ", ArrayUtils.toPrimitive(((YieldCurve) curveFwd6[0]).getCurve().getYData()),
        ArrayUtils.toPrimitive(((YieldCurve) curveFwd6[1]).getCurve().getYData()), TOLERANCE_CAL);

    assertEquals("Curve construction: 1 unit / 3 units ", bb[0].getBlock(CURVE_NAME_FWD6_EUR).getFirst(), bb[1].getBlock(CURVE_NAME_FWD6_EUR).getFirst());
  }

  //TODO: test on the correctness of the Jacobian matrix in the CurveBuildingBlock's.

  @Test(enabled = false)
  public void performance() {
    long startTime, endTime;
    final int nbTest = 100;

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < nbTest; looptest++) {
      makeCurvesFromDefinitions(DEFINITIONS_UNITS[0], GENERATORS_UNITS[0], NAMES_UNITS[0], MULTICURVE_KNOWN_DATA, PSMQDC, PSMQCSDC, false);
    }
    endTime = System.currentTimeMillis();
    System.out.println(nbTest + " curve construction / 3 units: " + (endTime - startTime) + " ms");
    // Performance note: Curve construction 3 units: 07-Nov-12: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 655 ms for 100 sets.

    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < nbTest; looptest++) {
      makeCurvesFromDefinitions(DEFINITIONS_UNITS[1], GENERATORS_UNITS[1], NAMES_UNITS[1], MULTICURVE_KNOWN_DATA, PSMQDC, PSMQCSDC, false);
    }
    endTime = System.currentTimeMillis();
    System.out.println(nbTest + " curve construction / 1 unit: " + (endTime - startTime) + " ms");
    // Performance note: Curve construction 1 unit: 07-Nov-12: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 635 ms for 100 sets.

    // Dsc code - FRA - 655 / 630
    // Fut code - FRA - 805 / 760
    // Fut code - Fut - 820 / 840 !

  }

  public void curveConstructionTest(final InstrumentDefinition<?>[][][] definitions, final MulticurveProviderDiscount curves, final boolean withToday, final int block) {
    final int nbBlocks = definitions.length;
    for (int loopblock = 0; loopblock < nbBlocks; loopblock++) {
      final InstrumentDerivative[][] instruments = convert(definitions[loopblock], withToday);
      final double[][] pv = new double[instruments.length][];
      for (int loopcurve = 0; loopcurve < instruments.length; loopcurve++) {
        pv[loopcurve] = new double[instruments[loopcurve].length];
        for (int loopins = 0; loopins < instruments[loopcurve].length; loopins++) {
          pv[loopcurve][loopins] = curves.getFxRates().convert(instruments[loopcurve][loopins].accept(PVDC, curves), EUR).getAmount();
          assertEquals("Curve construction: block " + block + ", unit " + loopblock + " - instrument " + loopins, 0, pv[loopcurve][loopins], TOLERANCE_CAL);
        }
      }
    }
  }

  @Test(enabled = false)
  /**
   * Analyzes the shape of the forward curve.
   */
  public void forwardAnalysis() {
    final MulticurveProviderInterface marketDsc = CURVES_PAR_SPREAD_MQ_WITHOUT_TODAY_BLOCK.get(0).getFirst();
    final int jump = 1;
    final int startIndex = 0;
    final int nbDate = 2750;
    ZonedDateTime startDate = ScheduleCalculator.getAdjustedDate(NOW, EURIBOR3M.getSpotLag() + startIndex * jump, TARGET);
    final double[] rateDsc = new double[nbDate];
    final double[] startTime = new double[nbDate];
    try {
      final FileWriter writer = new FileWriter("fwd-dsc.csv");
      for (int loopdate = 0; loopdate < nbDate; loopdate++) {
        startTime[loopdate] = TimeCalculator.getTimeBetween(NOW, startDate);
        final ZonedDateTime endDate = ScheduleCalculator.getAdjustedDate(startDate, EURIBOR3M, TARGET);
        final double endTime = TimeCalculator.getTimeBetween(NOW, endDate);
        final double accrualFactor = EURIBOR3M.getDayCount().getDayCountFraction(startDate, endDate);
        rateDsc[loopdate] = marketDsc.getForwardRate(EURIBOR3M, startTime[loopdate], endTime, accrualFactor);
        startDate = ScheduleCalculator.getAdjustedDate(startDate, jump, TARGET);
        writer.append(0.0 + "," + startTime[loopdate] + "," + rateDsc[loopdate] + "\n");
      }
      writer.flush();
      writer.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  private static Pair<MulticurveProviderDiscount, CurveBuildingBlockBundle> makeCurvesFromDefinitions(final InstrumentDefinition<?>[][][] definitions, final GeneratorYDCurve[][] curveGenerators,
      final String[][] curveNames, final MulticurveProviderDiscount knownData, final InstrumentDerivativeVisitor<MulticurveProviderInterface, Double> calculator,
      final InstrumentDerivativeVisitor<MulticurveProviderInterface, MulticurveSensitivity> sensitivityCalculator, final boolean withToday) {
    final int nbUnits = curveGenerators.length;
    final double[][] parametersGuess = new double[nbUnits][];
    final GeneratorYDCurve[][] generatorFinal = new GeneratorYDCurve[nbUnits][];
    final InstrumentDerivative[][][] instruments = new InstrumentDerivative[nbUnits][][];
    for (int loopunit = 0; loopunit < nbUnits; loopunit++) {
      generatorFinal[loopunit] = new GeneratorYDCurve[curveGenerators[loopunit].length];
      int nbInsUnit = 0;
      for (int loopcurve = 0; loopcurve < curveGenerators[loopunit].length; loopcurve++) {
        nbInsUnit += definitions[loopunit][loopcurve].length;
      }
      parametersGuess[loopunit] = new double[nbInsUnit];
      int startCurve = 0; // First parameter index of the curve in the unit.
      instruments[loopunit] = convert(definitions[loopunit], withToday);
      for (int loopcurve = 0; loopcurve < curveGenerators[loopunit].length; loopcurve++) {
        generatorFinal[loopunit][loopcurve] = curveGenerators[loopunit][loopcurve].finalGenerator(instruments[loopunit][loopcurve]);
        final double[] guessCurve = generatorFinal[loopunit][loopcurve].initialGuess(initialGuess(definitions[loopunit][loopcurve]));
        System.arraycopy(guessCurve, 0, parametersGuess[loopunit], startCurve, instruments[loopunit][loopcurve].length);
        startCurve += instruments[loopunit][loopcurve].length;
      }
    }
    return CURVE_BUILDING_REPOSITORY.makeCurvesFromDerivatives(instruments, generatorFinal, curveNames, parametersGuess, knownData, DSC_MAP, FWD_IBOR_MAP, FWD_ON_MAP, calculator,
        sensitivityCalculator);
  }

  @SuppressWarnings("unchecked")
  private static InstrumentDerivative[][] convert(final InstrumentDefinition<?>[][] definitions, final boolean withToday) {
    //    int nbDef = 0;
    //    for (final InstrumentDefinition<?>[] definition : definitions) {
    //      nbDef += definition.length;
    //    }
    final InstrumentDerivative[][] instruments = new InstrumentDerivative[definitions.length][];
    for (int loopcurve = 0; loopcurve < definitions.length; loopcurve++) {
      instruments[loopcurve] = new InstrumentDerivative[definitions[loopcurve].length];
      int loopins = 0;
      for (final InstrumentDefinition<?> instrument : definitions[loopcurve]) {
        InstrumentDerivative ird;
        if (instrument instanceof SwapFixedONDefinition) {
          ird = ((SwapFixedONDefinition) instrument).toDerivative(NOW, getTSSwapFixedON(withToday), NOT_USED_2);
        } else {
          if (instrument instanceof SwapFixedIborDefinition) {
            ird = ((SwapFixedIborDefinition) instrument).toDerivative(NOW, getTSSwapFixedIbor(withToday), NOT_USED_2);
          } else {
            if (instrument instanceof InterestRateFutureTransactionDefinition) {
              ird = ((InterestRateFutureTransactionDefinition) instrument).toDerivative(NOW, 0.0, NOT_USED_2); // Trade date = today, reference price not used.
            } else {
              ird = instrument.toDerivative(NOW, NOT_USED_2);
            }
          }
        }
        instruments[loopcurve][loopins++] = ird;
      }
    }
    return instruments;
  }

  @SuppressWarnings("rawtypes")
  private static ZonedDateTimeDoubleTimeSeries[] getTSSwapFixedON(final Boolean withToday) {
    return withToday ? TS_FIXED_OIS_EUR_WITH_TODAY : TS_FIXED_OIS_EUR_WITHOUT_TODAY;
  }

  @SuppressWarnings("rawtypes")
  private static ZonedDateTimeDoubleTimeSeries[] getTSSwapFixedIbor(final Boolean withToday) { // TODO: different fixing for 3 and 6 m
    return withToday ? TS_FIXED_IBOR_EUR3M_WITH_TODAY : TS_FIXED_IBOR_EUR3M_WITHOUT_TODAY;
  }

  private static double[] initialGuess(final InstrumentDefinition<?>[] definitions) {
    final double[] result = new double[definitions.length];
    int loopr = 0;
    for (final InstrumentDefinition<?> definition : definitions) {
      result[loopr++] = initialGuess(definition);
    }
    return result;
  }

  private static double initialGuess(final InstrumentDefinition<?> instrument) {
    if (instrument instanceof SwapFixedONDefinition) {
      return ((SwapFixedONDefinition) instrument).getFixedLeg().getNthPayment(0).getRate();
    }
    if (instrument instanceof SwapFixedIborDefinition) {
      return ((SwapFixedIborDefinition) instrument).getFixedLeg().getNthPayment(0).getRate();
    }
    if (instrument instanceof ForwardRateAgreementDefinition) {
      return ((ForwardRateAgreementDefinition) instrument).getRate();
    }
    if (instrument instanceof CashDefinition) {
      return ((CashDefinition) instrument).getRate();
    }
    if (instrument instanceof InterestRateFutureTransactionDefinition) {
      return 1 - ((InterestRateFutureTransactionDefinition) instrument).getTransactionPrice();
    }
    return 0.01;
  }

}
