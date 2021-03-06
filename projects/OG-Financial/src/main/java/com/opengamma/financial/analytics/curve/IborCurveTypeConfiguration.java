/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.curve;

import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.id.ExternalId;
import com.opengamma.util.time.Tenor;

/**
 * Configuration object for curves that are to be used as an ibor curve.
 */
@BeanDefinition
public class IborCurveTypeConfiguration extends CurveTypeConfiguration {

  /** Serialization version */
  private static final long serialVersionUID = 1L;

  /**
   * The convention of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private ExternalId _convention;

  /**
   * The tenor of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private Tenor _tenor;

  /**
   * For the fudge builder
   */
  /* package */ IborCurveTypeConfiguration() {
    super();
  }

  /**
   * @param convention The id of the index convention, not null
   * @param tenor The tenor of the index, not null
   */
  public IborCurveTypeConfiguration(final ExternalId convention, final Tenor tenor) {
    super();
    setConvention(convention);
    setTenor(tenor);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborCurveTypeConfiguration}.
   * @return the meta-bean, not null
   */
  public static IborCurveTypeConfiguration.Meta meta() {
    return IborCurveTypeConfiguration.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(IborCurveTypeConfiguration.Meta.INSTANCE);
  }

  @Override
  public IborCurveTypeConfiguration.Meta metaBean() {
    return IborCurveTypeConfiguration.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 2039569265:  // convention
        return getConvention();
      case 110246592:  // tenor
        return getTenor();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 2039569265:  // convention
        setConvention((ExternalId) newValue);
        return;
      case 110246592:  // tenor
        setTenor((Tenor) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  protected void validate() {
    JodaBeanUtils.notNull(_convention, "convention");
    JodaBeanUtils.notNull(_tenor, "tenor");
    super.validate();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborCurveTypeConfiguration other = (IborCurveTypeConfiguration) obj;
      return JodaBeanUtils.equal(getConvention(), other.getConvention()) &&
          JodaBeanUtils.equal(getTenor(), other.getTenor()) &&
          super.equals(obj);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash += hash * 31 + JodaBeanUtils.hashCode(getConvention());
    hash += hash * 31 + JodaBeanUtils.hashCode(getTenor());
    return hash ^ super.hashCode();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the convention of the index.
   * @return the value of the property, not null
   */
  public ExternalId getConvention() {
    return _convention;
  }

  /**
   * Sets the convention of the index.
   * @param convention  the new value of the property, not null
   */
  public void setConvention(ExternalId convention) {
    JodaBeanUtils.notNull(convention, "convention");
    this._convention = convention;
  }

  /**
   * Gets the the {@code convention} property.
   * @return the property, not null
   */
  public final Property<ExternalId> convention() {
    return metaBean().convention().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tenor of the index.
   * @return the value of the property, not null
   */
  public Tenor getTenor() {
    return _tenor;
  }

  /**
   * Sets the tenor of the index.
   * @param tenor  the new value of the property, not null
   */
  public void setTenor(Tenor tenor) {
    JodaBeanUtils.notNull(tenor, "tenor");
    this._tenor = tenor;
  }

  /**
   * Gets the the {@code tenor} property.
   * @return the property, not null
   */
  public final Property<Tenor> tenor() {
    return metaBean().tenor().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborCurveTypeConfiguration}.
   */
  public static class Meta extends CurveTypeConfiguration.Meta {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<ExternalId> _convention = DirectMetaProperty.ofReadWrite(
        this, "convention", IborCurveTypeConfiguration.class, ExternalId.class);
    /**
     * The meta-property for the {@code tenor} property.
     */
    private final MetaProperty<Tenor> _tenor = DirectMetaProperty.ofReadWrite(
        this, "tenor", IborCurveTypeConfiguration.class, Tenor.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, (DirectMetaPropertyMap) super.metaPropertyMap(),
        "convention",
        "tenor");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 2039569265:  // convention
          return _convention;
        case 110246592:  // tenor
          return _tenor;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends IborCurveTypeConfiguration> builder() {
      return new DirectBeanBuilder<IborCurveTypeConfiguration>(new IborCurveTypeConfiguration());
    }

    @Override
    public Class<? extends IborCurveTypeConfiguration> beanType() {
      return IborCurveTypeConfiguration.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ExternalId> convention() {
      return _convention;
    }

    /**
     * The meta-property for the {@code tenor} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Tenor> tenor() {
      return _tenor;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
