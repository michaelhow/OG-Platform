/**
 * Copyright (C) 2009 - 2011 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.web.bundle;


import static com.opengamma.web.bundle.BundleTest.BUTTON_CSS;
import static com.opengamma.web.bundle.BundleTest.CORE_CSS;
import static com.opengamma.web.bundle.BundleTest.CORE_JS;
import static com.opengamma.web.bundle.BundleTest.CSS_UTIL_CSS;
import static com.opengamma.web.bundle.BundleTest.FRAG_A;
import static com.opengamma.web.bundle.BundleTest.INIT_JS;
import static com.opengamma.web.bundle.BundleTest.JQUERY_JS;
import static com.opengamma.web.bundle.BundleTest.JS_BUNDLE_COMMON_JS;
import static com.opengamma.web.bundle.BundleTest.LINKS_CSS;
import static com.opengamma.web.bundle.BundleTest.OG_COMMON_CSS;
import static com.opengamma.web.bundle.BundleTest.RESET_CSS;
import static com.opengamma.web.bundle.BundleTest.makeCssBundleCommon;
import static com.opengamma.web.bundle.BundleTest.makeCssUtil;
import static com.opengamma.web.bundle.BundleTest.makejsBundleCommon;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.opengamma.web.bundle.Bundle;
import com.opengamma.web.bundle.BundleManager;
import com.opengamma.web.bundle.Fragment;

/**
 * Test BundleManager
 */
public class BundleManagerTest {
  
  private static List<Fragment> EXPECTED_JS_BUNDLE_COMMON = Lists.newArrayList(CORE_JS, INIT_JS, JQUERY_JS);
  
  private static List<Fragment> EXPECTED_CSS_UTIL = Lists.newArrayList(RESET_CSS, LINKS_CSS);
  
  private static List<Fragment> EXPECTED_CSS_BUNDLE_COMMON = Lists.newArrayList(FRAG_A, BUTTON_CSS, CORE_CSS);

  private final BundleManager _manager = new BundleManager();
  
  @Test
  public void test_addBundle_with_fragments() {
    
    _manager.addBundle(makeCssBundleCommon());
    _manager.addBundle(makeCssUtil());
    _manager.addBundle(makejsBundleCommon());
      
    Bundle bundle = _manager.getBundle(OG_COMMON_CSS);
    assertNotNull(bundle);
    assertEquals(EXPECTED_CSS_BUNDLE_COMMON, bundle.getAllFragment());
    
    bundle = _manager.getBundle(CSS_UTIL_CSS);
    assertNotNull(bundle);
    assertEquals(EXPECTED_CSS_UTIL, bundle.getAllFragment());
    
    bundle = _manager.getBundle(JS_BUNDLE_COMMON_JS);
    assertNotNull(bundle);
    assertEquals(EXPECTED_JS_BUNDLE_COMMON, bundle.getAllFragment());
    
  }
  
  @Test
  public void test_addBundle_with_bundles() {
    
    Bundle test = new Bundle("Composite");
    test.addChildNode(makeCssBundleCommon());
    test.addChildNode(makeCssUtil());
    
    _manager.addBundle(test);
    
    Bundle cssBundleCommon = _manager.getBundle(OG_COMMON_CSS);
    assertNotNull(cssBundleCommon);
    assertEquals(EXPECTED_CSS_BUNDLE_COMMON, cssBundleCommon.getAllFragment());
    
    Bundle cssUtil = _manager.getBundle(CSS_UTIL_CSS);
    assertNotNull(cssUtil);
    assertEquals(EXPECTED_CSS_UTIL, cssUtil.getAllFragment());
    
    Bundle composite = _manager.getBundle("Composite");
    assertNotNull(cssBundleCommon);
    List<Fragment> expectedComposite = new ArrayList<Fragment>(EXPECTED_CSS_BUNDLE_COMMON);
    expectedComposite.addAll(EXPECTED_CSS_UTIL);
    assertEquals(expectedComposite, composite.getAllFragment());
    
  }

}
