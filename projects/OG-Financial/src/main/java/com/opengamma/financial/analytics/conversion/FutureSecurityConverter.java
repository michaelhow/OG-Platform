/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.conversion;

import java.util.Set;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.commodity.definition.AgricultureFutureDefinition;
import com.opengamma.analytics.financial.commodity.definition.EnergyFutureDefinition;
import com.opengamma.analytics.financial.commodity.definition.MetalFutureDefinition;
import com.opengamma.analytics.financial.commodity.definition.SettlementType;
import com.opengamma.analytics.financial.equity.future.definition.EquityFutureDefinition;
import com.opengamma.analytics.financial.equity.future.definition.IndexFutureDefinition;
import com.opengamma.analytics.financial.instrument.InstrumentDefinitionWithData;
import com.opengamma.analytics.financial.instrument.future.BondFutureDefinition;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureSecurityDefinition;
import com.opengamma.analytics.financial.instrument.future.InterestRateFutureTransactionDefinition;
import com.opengamma.financial.security.FinancialSecurityVisitor;
import com.opengamma.financial.security.FinancialSecurityVisitorAdapter;
import com.opengamma.financial.security.future.AgricultureFutureSecurity;
import com.opengamma.financial.security.future.BondFutureSecurity;
import com.opengamma.financial.security.future.EnergyFutureSecurity;
import com.opengamma.financial.security.future.EquityFutureSecurity;
import com.opengamma.financial.security.future.EquityIndexDividendFutureSecurity;
import com.opengamma.financial.security.future.FutureSecurity;
import com.opengamma.financial.security.future.IndexFutureSecurity;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.financial.security.future.MetalFutureSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public class FutureSecurityConverter extends FinancialSecurityVisitorAdapter<InstrumentDefinitionWithData<?, Double>>
    implements FinancialSecurityVisitorWithData<Double, InstrumentDefinitionWithData<?, Double>> {
  private final InterestRateFutureSecurityConverter _irFutureConverter;
  private final BondFutureSecurityConverter _bondFutureConverter;

  public FutureSecurityConverter(final InterestRateFutureSecurityConverter irFutureConverter, final BondFutureSecurityConverter bondFutureConverter) {
    _irFutureConverter = irFutureConverter;
    _bondFutureConverter = bondFutureConverter;
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visit(final FutureSecurity future, final Double referencePrice) {

    final FinancialSecurityVisitor<InstrumentDefinitionWithData<?, Double>> visitor = new FinancialSecurityVisitorAdapter<InstrumentDefinitionWithData<?, Double>>() {

      @Override
      public InstrumentDefinitionWithData<?, Double> visitAgricultureFutureSecurity(final AgricultureFutureSecurity security) {
        ArgumentChecker.notNull(security, "security");
        final ZonedDateTime expiry = security.getExpiry().getExpiry();
        final Set<ExternalId> externalIds = security.getExternalIdBundle().getExternalIds();
        if (externalIds == null) {
          throw new OpenGammaRuntimeException("Can't get security id");
        }
        return new AgricultureFutureDefinition(expiry, externalIds.iterator().next(), security.getUnitNumber(), null, null,
            1.0, security.getUnitName(), SettlementType.CASH, referencePrice, security.getCurrency(), expiry);
      }

      @Override
      public InstrumentDefinitionWithData<?, Double> visitEnergyFutureSecurity(final EnergyFutureSecurity security) {
        ArgumentChecker.notNull(security, "security");
        final ZonedDateTime expiry = security.getExpiry().getExpiry();
        final Set<ExternalId> externalIds = security.getExternalIdBundle().getExternalIds();
        if (externalIds == null) {
          throw new OpenGammaRuntimeException("Can't get security id");
        }
        return new EnergyFutureDefinition(expiry, externalIds.iterator().next(), security.getUnitNumber(), null, null,
            1.0, security.getUnitName(), SettlementType.CASH, referencePrice, security.getCurrency(), expiry);
      }

      @Override
      public InstrumentDefinitionWithData<?, Double> visitMetalFutureSecurity(final MetalFutureSecurity security) {
        ArgumentChecker.notNull(security, "security");
        final ZonedDateTime expiry = security.getExpiry().getExpiry();
        final Set<ExternalId> externalIds = security.getExternalIdBundle().getExternalIds();
        if (externalIds == null) {
          throw new OpenGammaRuntimeException("Can't get security id");
        }
        return new MetalFutureDefinition(expiry, externalIds.iterator().next(), security.getUnitNumber(), null, null,
            1.0, security.getUnitName(), SettlementType.CASH, referencePrice, security.getCurrency(), expiry);
      }

      @Override
      public InstrumentDefinitionWithData<?, Double> visitEquityIndexDividendFutureSecurity(final EquityIndexDividendFutureSecurity security) {
        final ZonedDateTime expiry = security.getExpiry().getExpiry();
        return new EquityFutureDefinition(expiry, expiry, referencePrice, security.getCurrency(), security.getUnitAmount());
      }

      @Override
      public InstrumentDefinitionWithData<?, Double> visitEquityFutureSecurity(final EquityFutureSecurity security) {
        final ZonedDateTime expiry = security.getExpiry().getExpiry();
        return new EquityFutureDefinition(expiry, expiry, referencePrice, security.getCurrency(), security.getUnitAmount());
      }

      
      @Override
      public InstrumentDefinitionWithData<?, Double> visitIndexFutureSecurity(final IndexFutureSecurity security) {
        final ZonedDateTime expiry = security.getExpiry().getExpiry();
        String type = security.getSecurityType();
        return new IndexFutureDefinition(expiry, expiry, referencePrice, security.getCurrency(), security.getUnitAmount(), security.getUnderlyingId());
      }
      
      @SuppressWarnings("synthetic-access")
      @Override
      public InstrumentDefinitionWithData<?, Double> visitInterestRateFutureSecurity(final InterestRateFutureSecurity security) {
        final InterestRateFutureTransactionDefinition securityDefinition = (InterestRateFutureTransactionDefinition) security.accept(_irFutureConverter);
        return securityDefinition;
        //      new InterestRateFutureTransactionDefinition(securityDefinition.getLastTradingDate(), referencePrice, 
        //      securityDefinition.getLastTradingDate(), securityDefinition.getIborIndex(),
        //      securityDefinition.getNotional(), securityDefinition.getPaymentAccrualFactor(), 1, securityDefinition.getName());
      }

      @SuppressWarnings("synthetic-access")
      @Override
      public InstrumentDefinitionWithData<?, Double> visitBondFutureSecurity(final BondFutureSecurity security) {
        return (BondFutureDefinition) security.accept(_bondFutureConverter);
      }

    };

    return future.accept(visitor);
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visitAgricultureFutureSecurity(final AgricultureFutureSecurity security) {
    return visit(security, 0.0);
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visitEnergyFutureSecurity(final EnergyFutureSecurity security) {
    return visit(security, 0.0);
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visitMetalFutureSecurity(final MetalFutureSecurity security) {
    return visit(security, 0.0);
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visitEquityIndexDividendFutureSecurity(final EquityIndexDividendFutureSecurity security) {
    return visit(security, 0.);
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visitEquityFutureSecurity(final EquityFutureSecurity security) {
    return visit(security, 0.);
  }
  
  @Override
  public InstrumentDefinitionWithData<?, Double> visitIndexFutureSecurity(final IndexFutureSecurity security) {
    return visit(security, 0.);
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visitInterestRateFutureSecurity(final InterestRateFutureSecurity security) {
    return (InterestRateFutureSecurityDefinition) security.accept(_irFutureConverter);
  }

  @Override
  public InstrumentDefinitionWithData<?, Double> visitBondFutureSecurity(final BondFutureSecurity security) {
    return (BondFutureDefinition) security.accept(_bondFutureConverter);
  }
}
