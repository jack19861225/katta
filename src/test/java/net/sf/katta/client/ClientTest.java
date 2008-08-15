/**
 * Copyright 2008 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.katta.client;

import java.io.IOException;
import java.util.Set;

import net.sf.katta.AbstractKattaTest;
import net.sf.katta.Katta;
import net.sf.katta.master.IPaths;
import net.sf.katta.master.Master;
import net.sf.katta.node.Hit;
import net.sf.katta.node.Hits;
import net.sf.katta.node.Node;
import net.sf.katta.node.Query;
import net.sf.katta.util.KattaException;
import net.sf.katta.util.Logger;
import net.sf.katta.zk.ZKClient;
import net.sf.katta.zk.ZkServer;

import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.ipc.RPC;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * Test for {@link Client}.
 */
public class ClientTest extends AbstractKattaTest {

  private ZkServer _zkServer;
  private Node _node1;
  private Node _node2;
  private Master _master;
  private Katta _katta;
  private IClient client;

  @Override
  protected void onSetUp() throws Exception {
    _zkServer = new ZkServer(conf);

    ZKClient zkClientMaster = new ZKClient(conf);
    _master = new Master(zkClientMaster);
    Thread masterThread = createStartMasterThread(_master);
    masterThread.start();

    _node1 = startNodeServer(new ZKClient(conf));
    _node2 = startNodeServer(new ZKClient(conf));
    masterThread.join();
    waitForChilds(zkClientMaster, IPaths.NODES, 2);

    _katta = new Katta();
    _katta.addIndex("index", "src/test/testIndexA/", StandardAnalyzer.class.getName(), 1);

    _katta.addIndex("index1", "src/test/testIndexA/", StandardAnalyzer.class.getName(), 1);
    _katta.addIndex("index2", "src/test/testIndexA/", StandardAnalyzer.class.getName(), 1);
    client = new Client();
  }

  @Override
  protected void onTearDown() throws Exception {
    client.close();
    _katta.close();
    _node1.shutdown();
    _node2.shutdown();
    _master.shutdown();
    _zkServer.shutdown();
    RPC.stopClient();
  }

  //
  public void testCount() {
    final Query query = new Query("content: the");
    final int count = client.count(query, new String[] { "index" });
    assertEquals(937, count);

  }

  public void testGetDetails() throws IOException, KattaException {
    final Query query = new Query("content:the");
    final Hits hits = client.search(query, new String[] { "index" }, 10);
    assertNotNull(hits);
    assertEquals(10, hits.getHits().size());
    for (final Hit hit : hits.getHits()) {
      final MapWritable details = client.getDetails(hit);
      final Set<Writable> keySet = details.keySet();
      assertFalse(keySet.isEmpty());
      final Writable writable = details.get(new Text("path"));
      assertNotNull(writable);
    }
  }

  public void testSearch() throws KattaException {
    final Query query = new Query("foo: bar");
    final Hits hits = client.search(query, new String[] { "index2", "index1" });
    assertNotNull(hits);
    assertEquals(1f, client.getQueryPerMinute());
    for (final Hit hit : hits.getHits()) {
      Logger.info(hit.getNode() + " -- " + hit.getShard() + " -- " + hit.getScore() + " -- " + hit.getDocId());
    }
    assertEquals(8, hits.size());
    assertEquals(8, hits.getHits().size());
    for (final Hit hit : hits.getHits()) {
      Logger.info(hit.getNode() + " -- " + hit.getScore() + " -- " + hit.getDocId());
    }
  }

  public void testSearchLimit() throws KattaException {
    final Query query = new Query("foo: bar");
    final Hits hits = client.search(query, new String[] { "index2", "index1" }, 1);
    assertNotNull(hits);
    for (final Hit hit : hits.getHits()) {
      Logger.info(hit.getNode() + " -- " + hit.getShard() + " -- " + hit.getScore() + " -- " + hit.getDocId());
    }
    assertEquals(8, hits.size());
    assertEquals(1, hits.getHits().size());
    for (final Hit hit : hits.getHits()) {
      Logger.info(hit.getNode() + " -- " + hit.getScore() + " -- " + hit.getDocId());
    }
  }

  public void testSearchSimiliarity() throws KattaException {
    final Query query = new Query("foo: bar");
    final Hits hits = client.search(query, new String[] { "index1" });
    assertNotNull(hits);
    assertEquals(4, hits.getHits().size());
    for (final Hit hit : hits.getHits()) {
      Logger.info(hit.getNode() + " -- " + hit.getScore() + " -- " + hit.getDocId());
    }
  }
}
