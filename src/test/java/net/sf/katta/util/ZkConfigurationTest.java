/**
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.katta.util;

import junit.framework.TestCase;

public class ZkConfigurationTest extends TestCase {

  public void testSystemProperty() {
    try {
      System.clearProperty(ZkConfiguration.KATTA_PROPERTY_NAME);
      ZkConfiguration conf1 = new ZkConfiguration();
      System.setProperty(ZkConfiguration.KATTA_PROPERTY_NAME, "/katta.zk.properties_alt_root");
      ZkConfiguration conf2 = new ZkConfiguration();
      //
      assertEquals("/katta", conf1.getZKRootPath());
      assertEquals("/test/katta20090510153800", conf2.getZKRootPath());
      //
      try {
        System.setProperty(ZkConfiguration.KATTA_PROPERTY_NAME, "/not-found");
        new ZkConfiguration();
        fail("Should have failed");
      } catch (RuntimeException e) {
        // Good.
      }
    } finally {
      System.clearProperty(ZkConfiguration.KATTA_PROPERTY_NAME);
    }
  }
  
}
