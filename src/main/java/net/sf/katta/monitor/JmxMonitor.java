/**
 * Copyright 2008 the original author or authors.
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
package net.sf.katta.monitor;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import net.sf.katta.util.ZkConfiguration;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.apache.log4j.Logger;

public class JmxMonitor implements IMonitor {
  protected final static Logger LOG = Logger.getLogger(JmxMonitor.class);

  private ZkClient _zkClient;
  private JmxMonitorThread _thread;
  private String _metricsPath;
  private String _serverId;

  @Override
  public void startMonitoring(String serverId, ZkClient zkClient, ZkConfiguration zkConfiguration) {
    if (serverId == null || zkClient == null || zkConfiguration == null) {
      throw new IllegalArgumentException("parameters can't be null");
    }
    _serverId = serverId;
    _metricsPath = zkConfiguration.getZKMetricsPathForServer(serverId);
    _zkClient = zkClient;
    _thread = new JmxMonitorThread();
    _thread.start();
  }

  @Override
  public void stopMonitoring() {
    _thread.interrupt();
  }

  private class JmxMonitorThread extends Thread {

    public void run() {
      // create the node for the first time...
      while (!isInterrupted()) {
        MetricsRecord metrics = new MetricsRecord(_serverId);
        // there is no good way to get underlaying system matrics . Only
        // com.sun.management.OperatingSystemMXBean provides some basic
        // information. See:
        // http://nadeausoftware.com/articles/2008/03/java%5Ftip%5Fhow%5Fget%5Fcpu%5Fand%5Fuser%5Ftime%5Fbenchmarking#UsingaSuninternalclasstogetJVMCPUtime

        try {
          // operation system
          OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

          Long processCpuTime = getValue(operatingSystemMXBean, "getProcessCpuTime");
          metrics.addValue("ProcessCpuTime", processCpuTime, System.currentTimeMillis());

          Long totalSwapSpaceSize = getValue(operatingSystemMXBean, "getTotalSwapSpaceSize");
          metrics.addValue("TotalSwapSpaceSize", totalSwapSpaceSize, System.currentTimeMillis());
          Long freeSwapSpaceSize = getValue(operatingSystemMXBean, "getFreeSwapSpaceSize");
          metrics.addValue("FreeSwapSpaceSize", freeSwapSpaceSize, System.currentTimeMillis());

          Long freePhysicalMemorySize = getValue(operatingSystemMXBean, "getFreePhysicalMemorySize");
          metrics.addValue("FreePhysicalMemorySize", freePhysicalMemorySize, System.currentTimeMillis());
          Long totalPhysicalMemorySize = getValue(operatingSystemMXBean, "getTotalPhysicalMemorySize");
          metrics.addValue("TotalPhysicalMemorySize", totalPhysicalMemorySize, System.currentTimeMillis());

          Long committedVirtualMemorySize = getValue(operatingSystemMXBean, "getCommittedVirtualMemorySize");
          metrics.addValue("CommittedVirtualMemorySize", committedVirtualMemorySize, System.currentTimeMillis());

          // memory
          MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
          metrics.addValue("heapMemoryUsageMax", heapMemoryUsage.getMax(), System.currentTimeMillis());
          metrics.addValue("heapMemoryUsageUsed", heapMemoryUsage.getUsed(), System.currentTimeMillis());
          metrics.addValue("heapMemoryUsageInit", heapMemoryUsage.getInit(), System.currentTimeMillis());
          metrics.addValue("heapMemoryUsageCommited", heapMemoryUsage.getCommitted(), System.currentTimeMillis());

          MemoryUsage nonHeapMemoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
          metrics.addValue("nonHeapMemoryUsageMax", nonHeapMemoryUsage.getMax(), System.currentTimeMillis());
          metrics.addValue("nonHeapMemoryUsageUsed", nonHeapMemoryUsage.getUsed(), System.currentTimeMillis());
          metrics.addValue("nonHeapMemoryUsageInit", nonHeapMemoryUsage.getInit(), System.currentTimeMillis());
          metrics.addValue("nonHeapMemoryUsageCommited", nonHeapMemoryUsage.getCommitted(), System.currentTimeMillis());

          // Garbage collection
          List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
          for (GarbageCollectorMXBean bean : garbageCollectorMXBeans) {
            String gcName = bean.getName();
            long collectionCount = bean.getCollectionCount();
            metrics.addValue("collectionCount_" + gcName, collectionCount, System.currentTimeMillis());
            long collectionTime = bean.getCollectionTime();
            metrics.addValue("collectionTime_" + gcName, collectionTime, System.currentTimeMillis());
          }
          // File System
          // TODO sg 10/03/09 We should add diskfree as well, but we need to
          // know which
          // disks we talk about. This brings in the problem that currently we
          // can only use one not multiple disks for the shards. So lets fix
          // this as soon we fix this other problem :/

        } catch (Exception e) {
          LOG.error("Unable to retrieve metrics values:", e);
        }
        try {
          _zkClient.writeData(_metricsPath, metrics);
        } catch (ZkNoNodeException e) {
          _zkClient.createEphemeral(_metricsPath, new MetricsRecord(_serverId));
        } catch (Exception e) {
          // this only happens if zk is down
          LOG.debug("Can't write to zk", e);
        }
        try {
          sleep(500);
        } catch (InterruptedException e) {
          LOG.debug("Sleep was interrupted", e);
        }
      }
    }

    private Long getValue(OperatingSystemMXBean operatingSystemMXBean, String methodName) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
      Method method = operatingSystemMXBean.getClass().getMethod(methodName, new Class[0]);
      method.setAccessible(true);
      return (Long) method.invoke(operatingSystemMXBean);
    }
  }

}