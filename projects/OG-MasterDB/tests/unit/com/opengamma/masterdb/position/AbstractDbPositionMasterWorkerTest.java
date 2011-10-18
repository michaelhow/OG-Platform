/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.masterdb.position;

import static com.opengamma.util.db.DbDateUtils.MAX_SQL_TIMESTAMP;
import static com.opengamma.util.db.DbDateUtils.toSqlDate;
import static com.opengamma.util.db.DbDateUtils.toSqlTimestamp;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import javax.time.Instant;
import javax.time.TimeSource;
import javax.time.calendar.OffsetDateTime;
import javax.time.calendar.OffsetTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.master.position.ManageablePosition;
import com.opengamma.master.position.ManageableTrade;
import com.opengamma.master.position.PositionDocument;
import com.opengamma.masterdb.DbMasterTestUtils;
import com.opengamma.util.test.DbTest;

/**
 * Base tests for DbPositionMasterWorker via DbPositionMaster.
 */
public abstract class AbstractDbPositionMasterWorkerTest extends DbTest {

  private static final Logger s_logger = LoggerFactory.getLogger(AbstractDbPositionMasterWorkerTest.class);

  protected DbPositionMaster _posMaster;
  protected Instant _version1Instant;
  protected Instant _version2Instant;
  protected int _totalPortfolios;
  protected int _totalPositions;
  protected OffsetDateTime _now;

  public AbstractDbPositionMasterWorkerTest(String databaseType, String databaseVersion) {
    super(databaseType, databaseVersion);
    s_logger.info("running testcases for {}", databaseType);
  }

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    ConfigurableApplicationContext context = DbMasterTestUtils.getContext(getDatabaseType());
    _posMaster = (DbPositionMaster) context.getBean(getDatabaseType() + "DbPositionMaster");
    
    _now = OffsetDateTime.now();
    _posMaster.setTimeSource(TimeSource.fixed(_now.toInstant()));
    _version1Instant = _now.toInstant().minusSeconds(100);
    _version2Instant = _now.toInstant().minusSeconds(50);
    s_logger.debug("test data now:   {}", _version1Instant);
    s_logger.debug("test data later: {}", _version2Instant);
    final SimpleJdbcTemplate template = _posMaster.getDbSource().getJdbcTemplate();
    template.update("INSERT INTO pos_position VALUES (?,?,?,?,?, ?,?,?,?)",
        100, 100, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, "A", "100", BigDecimal.valueOf(100.987));
    template.update("INSERT INTO pos_position VALUES (?,?,?,?,?, ?,?,?,?)",
        120, 120, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, "A", "120", BigDecimal.valueOf(120.987));
    template.update("INSERT INTO pos_position VALUES (?,?,?,?,?, ?,?,?,?)",
        121, 121, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, "A", "121", BigDecimal.valueOf(121.987));
    template.update("INSERT INTO pos_position VALUES (?,?,?,?,?, ?,?,?,?)",
        122, 122, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, "A", "122", BigDecimal.valueOf(122.987));
    template.update("INSERT INTO pos_position VALUES (?,?,?,?,?, ?,?,?,?)",
        123, 123, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, "A", "123", BigDecimal.valueOf(123.987));
    template.update("INSERT INTO pos_position VALUES (?,?,?,?,?, ?,?,?,?)",
        221, 221, toSqlTimestamp(_version1Instant), toSqlTimestamp(_version2Instant), toSqlTimestamp(_version1Instant), MAX_SQL_TIMESTAMP, "A", "221", BigDecimal.valueOf(221.987));
    template.update("INSERT INTO pos_position VALUES (?,?,?,?,?, ?,?,?,?)",
        222, 221, toSqlTimestamp(_version2Instant), MAX_SQL_TIMESTAMP, toSqlTimestamp(_version2Instant), MAX_SQL_TIMESTAMP, "A", "222", BigDecimal.valueOf(222.987));
    _totalPositions = 6;
    
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        500, "TICKER", "S100");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        501, "TICKER", "T130");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        502, "TICKER", "MSFT");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        503, "NASDAQ", "Micro");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        504, "TICKER", "ORCL");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        505, "TICKER", "ORCL134");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        506, "NASDAQ", "ORCL135");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        507, "TICKER", "IBMC");
    template.update("INSERT INTO pos_idkey VALUES (?,?,?)",
        508, "OID", "DbSec~1234");
    
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 100, 500);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 120, 501);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 121, 502);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 121, 503);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 122, 504);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 123, 505);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 123, 506);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 221, 507);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 222, 507);
    template.update("INSERT INTO pos_position2idkey VALUES (?,?)", 222, 508);
    
    OffsetTime tradeTime = _now.toOffsetTime().minusSeconds(400);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        400, 400, 120, 120, BigDecimal.valueOf(120.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "C100", "B", "400");
    tradeTime = _now.toOffsetTime().minusSeconds(401);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        401, 401, 121, 121, BigDecimal.valueOf(121.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "C101", "B", "401");
    tradeTime = _now.toOffsetTime().minusSeconds(402);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        402, 402, 122, 122, BigDecimal.valueOf(100.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "JMP", "B", "402");
    tradeTime = _now.toOffsetTime().minusSeconds(403);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        403, 403, 122, 122, BigDecimal.valueOf(22.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "CISC", "B", "403");
    tradeTime = _now.toOffsetTime().minusSeconds(404);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        404, 404, 123, 123, BigDecimal.valueOf(100.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "C104", "B", "404");
    tradeTime = _now.toOffsetTime().minusSeconds(405);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        405, 405, 123, 123, BigDecimal.valueOf(200.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "C105", "B", "405");
    tradeTime = _now.toOffsetTime().minusSeconds(406);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        406, 406, 123, 123, BigDecimal.valueOf(300.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "C106", "B", "406");
    tradeTime = _now.toOffsetTime().minusSeconds(407);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        407, 407, 221, 221, BigDecimal.valueOf(221.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "C221", "B", "407");
    tradeTime = _now.toOffsetTime().minusSeconds(408);
    template.update("INSERT INTO pos_trade (id, oid, position_id, position_oid, quantity, trade_date, trade_time, zone_offset, cparty_scheme, cparty_value, provider_scheme, provider_value) " +
    		"VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", 
        408, 407, 222, 221, BigDecimal.valueOf(222.987), toSqlDate(_now.toLocalDate()), toSqlTimestamp(tradeTime), tradeTime.getOffset().getAmountSeconds(), "CPARTY", "C222", "B", "408");
    
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 400, 501);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 401, 502);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 401, 503);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 402, 504);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 403, 504);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 404, 505);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 404, 506);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 405, 505);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 405, 506);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 406, 505);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 406, 506);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 407, 507);
    template.update("INSERT INTO pos_trade2idkey VALUES (?,?)", 408, 507);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    _posMaster = null;
    super.tearDown();
  }

  //-------------------------------------------------------------------------
  protected void assert100(final PositionDocument test) {
    UniqueId uniqueId = UniqueId.of("DbPos", "100", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageablePosition position = test.getPosition();
    assertNotNull(position);
    assertEquals(uniqueId, position.getUniqueId());
    assertEquals(ExternalId.of("A", "100"), position.getProviderId());
    assertEquals(BigDecimal.valueOf(100.987), position.getQuantity());
    ExternalIdBundle secKey = position.getSecurityLink().getExternalId();
    assertEquals(1, secKey.size());
    assertEquals(true, secKey.getExternalIds().contains(ExternalId.of("TICKER", "S100")));
    assertEquals(null, position.getSecurityLink().getObjectId());
    
    List<ManageableTrade> trades = position.getTrades();
    assertNotNull(trades);
    assertTrue(trades.isEmpty());
  }

  protected void assert120(final PositionDocument test) {
    UniqueId uniqueId = UniqueId.of("DbPos", "120", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageablePosition position = test.getPosition();
    assertNotNull(position);
    assertEquals(uniqueId, position.getUniqueId());
    assertEquals(ExternalId.of("A", "120"), position.getProviderId());
    assertEquals(BigDecimal.valueOf(120.987), position.getQuantity());
    ExternalIdBundle secKey = position.getSecurityLink().getExternalId();
    assertEquals(1, secKey.size());
    assertEquals(true, secKey.getExternalIds().contains(ExternalId.of("TICKER", "T130")));
    assertEquals(null, position.getSecurityLink().getObjectId());
    
    List<ManageableTrade> trades = position.getTrades();
    assertEquals(1, trades.size());
    ManageableTrade trade = trades.get(0);
    assertNotNull(trade);
    assertEquals(UniqueId.of("DbPos", "400", "0"), trade.getUniqueId());
    assertEquals(ExternalId.of("B", "400"), trade.getProviderId());
    assertEquals(uniqueId, trade.getParentPositionId());
    assertEquals(ExternalId.of("CPARTY", "C100"), trade.getCounterpartyExternalId());
    assertEquals(BigDecimal.valueOf(120.987), trade.getQuantity());
    assertEquals(_now.toLocalDate(), trade.getTradeDate());
    assertEquals(_now.toOffsetTime().minusSeconds(400), trade.getTradeTime());
    assertEquals(true, trade.getSecurityLink().getExternalIds().contains(ExternalId.of("TICKER", "T130")));
  }

  protected void assert121(final PositionDocument test) {
    UniqueId uniqueId = UniqueId.of("DbPos", "121", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageablePosition position = test.getPosition();
    assertNotNull(position);
    assertEquals(uniqueId, position.getUniqueId());
    assertEquals(ExternalId.of("A", "121"), position.getProviderId());
    assertEquals(BigDecimal.valueOf(121.987), position.getQuantity());
    ExternalIdBundle secKey = position.getSecurityLink().getExternalId();
    assertEquals(2, secKey.size());
    assertEquals(true, secKey.getExternalIds().contains(ExternalId.of("TICKER", "MSFT")));
    assertEquals(true, secKey.getExternalIds().contains(ExternalId.of("NASDAQ", "Micro")));
    assertEquals(null, position.getSecurityLink().getObjectId());
    
    List<ManageableTrade> trades = position.getTrades();
    assertEquals(1, trades.size());
    ManageableTrade trade = trades.get(0);
    assertNotNull(trade);
    assertEquals(UniqueId.of("DbPos", "401", "0"), trade.getUniqueId());
    assertEquals(ExternalId.of("B", "401"), trade.getProviderId());
    assertEquals(uniqueId, trade.getParentPositionId());
    assertEquals(ExternalId.of("CPARTY", "C101"), trade.getCounterpartyExternalId());
    assertEquals(BigDecimal.valueOf(121.987), trade.getQuantity());
    assertEquals(_now.toLocalDate(), trade.getTradeDate());
    assertEquals(_now.toOffsetTime().minusSeconds(401), trade.getTradeTime());
  }

  protected void assert122(final PositionDocument test) {
    UniqueId uniqueId = UniqueId.of("DbPos", "122", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageablePosition position = test.getPosition();
    assertNotNull(position);
    assertEquals(uniqueId, position.getUniqueId());
    assertEquals(ExternalId.of("A", "122"), position.getProviderId());
    assertEquals(BigDecimal.valueOf(122.987), position.getQuantity());
    ExternalIdBundle secKey = position.getSecurityLink().getExternalId();
    assertEquals(1, secKey.size());
    assertEquals(ExternalId.of("TICKER", "ORCL"), secKey.getExternalIds().iterator().next());
    assertEquals(null, position.getSecurityLink().getObjectId());
    assertEquals(2, position.getTrades().size());
  }

  protected void assert123(final PositionDocument test) {
    UniqueId uniqueId = UniqueId.of("DbPos", "123", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageablePosition position = test.getPosition();
    assertNotNull(position);
    assertEquals(uniqueId, position.getUniqueId());
    assertEquals(ExternalId.of("A", "123"), position.getProviderId());
    assertEquals(BigDecimal.valueOf(123.987), position.getQuantity());
    ExternalIdBundle secKey = position.getSecurityLink().getExternalId();
    assertEquals(2, secKey.size());
    assertTrue(secKey.getExternalIds().contains(ExternalId.of("NASDAQ", "ORCL135")));
    assertTrue(secKey.getExternalIds().contains(ExternalId.of("TICKER", "ORCL134")));
    assertEquals(null, position.getSecurityLink().getObjectId());
    
    List<ManageableTrade> trades = position.getTrades();
    assertEquals(3, trades.size());
    
    ManageableTrade trade = new ManageableTrade(BigDecimal.valueOf(100.987), secKey, _now.toLocalDate(), _now.toOffsetTime().minusSeconds(404), ExternalId.of("CPARTY", "C104"));
    trade.setParentPositionId(uniqueId);
    trade.setUniqueId(UniqueId.of("DbPos", "404", "0"));
    trade.setProviderId(ExternalId.of("B", "404"));
    assertTrue(trades.contains(trade));
    
    trade = new ManageableTrade(BigDecimal.valueOf(200.987), secKey, _now.toLocalDate(), _now.toOffsetTime().minusSeconds(405), ExternalId.of("CPARTY", "C105"));
    trade.setParentPositionId(uniqueId);
    trade.setUniqueId(UniqueId.of("DbPos", "405", "0"));
    trade.setProviderId(ExternalId.of("B", "405"));
    assertTrue(trades.contains(trade));
    
    trade = new ManageableTrade(BigDecimal.valueOf(300.987), secKey, _now.toLocalDate(), _now.toOffsetTime().minusSeconds(406),ExternalId.of("CPARTY", "C106"));
    trade.setParentPositionId(uniqueId);
    trade.setUniqueId(UniqueId.of("DbPos", "406", "0"));
    trade.setProviderId(ExternalId.of("B", "406"));
    assertTrue(trades.contains(trade));
  }

  protected void assert221(final PositionDocument test) {
    UniqueId uniqueId = UniqueId.of("DbPos", "221", "0");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version1Instant, test.getVersionFromInstant());
    assertEquals(_version2Instant, test.getVersionToInstant());
    assertEquals(_version1Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageablePosition position = test.getPosition();
    assertNotNull(position);
    assertEquals(uniqueId, position.getUniqueId());
    assertEquals(ExternalId.of("A", "221"), position.getProviderId());
    assertEquals(BigDecimal.valueOf(221.987), position.getQuantity());
    ExternalIdBundle secKey = position.getSecurityLink().getExternalId();
    assertEquals(1, secKey.size());
    assertEquals(ExternalId.of("TICKER", "IBMC"), secKey.getExternalIds().iterator().next());
    assertEquals(null, position.getSecurityLink().getObjectId());
    
    List<ManageableTrade> trades = position.getTrades();
    assertEquals(1, trades.size());
    ManageableTrade expected = new ManageableTrade(BigDecimal.valueOf(221.987), secKey, _now.toLocalDate(), _now.toOffsetTime().minusSeconds(407), ExternalId.of("CPARTY", "C221"));
    expected.setParentPositionId(uniqueId);
    expected.setUniqueId(UniqueId.of("DbPos", "407", "0"));
    expected.setProviderId(ExternalId.of("B", "407"));
    assertTrue(trades.contains(expected));
  }

  protected void assert222(final PositionDocument test) {
    UniqueId uniqueId = UniqueId.of("DbPos", "221", "1");
    assertNotNull(test);
    assertEquals(uniqueId, test.getUniqueId());
    assertEquals(_version2Instant, test.getVersionFromInstant());
    assertEquals(null, test.getVersionToInstant());
    assertEquals(_version2Instant, test.getCorrectionFromInstant());
    assertEquals(null, test.getCorrectionToInstant());
    ManageablePosition position = test.getPosition();
    assertNotNull(position);
    assertEquals(uniqueId, position.getUniqueId());
    assertEquals(ExternalId.of("A", "222"), position.getProviderId());
    assertEquals(BigDecimal.valueOf(222.987), position.getQuantity());
    ExternalIdBundle secKey = position.getSecurityLink().getExternalId();
    assertEquals(1, secKey.size());
    assertEquals(ExternalId.of("TICKER", "IBMC"), secKey.getExternalIds().iterator().next());
    assertEquals(ObjectId.of("DbSec", "1234"), position.getSecurityLink().getObjectId());
    
    List<ManageableTrade> trades = position.getTrades();
    assertEquals(1, trades.size());
    ManageableTrade expected = new ManageableTrade(BigDecimal.valueOf(222.987), secKey, _now.toLocalDate(), _now.toOffsetTime().minusSeconds(408), ExternalId.of("CPARTY", "C222"));
    expected.setParentPositionId(uniqueId);
    expected.setUniqueId(UniqueId.of("DbPos", "407", "1"));
    expected.setProviderId(ExternalId.of("B", "408"));
    assertTrue(trades.contains(expected));
  }

}
