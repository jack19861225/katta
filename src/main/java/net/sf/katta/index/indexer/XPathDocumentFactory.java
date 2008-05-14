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
package net.sf.katta.index.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Set;

import net.sf.katta.util.Logger;
import net.sf.katta.util.NumberPaddingUtil;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

public class XPathDocumentFactory implements IDocumentFactory<Text, Text> {

  public static final String XPATH_INPUT_FILE = "xpath.input.file";
  private IXPathService _xPathService = new DefaultXPathService();
  private Properties _properties = new Properties();
  private static final SimpleDateFormat _dateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");

  public Document convert(final Text key, final Text value) {
    final Document document = new Document();
    final Field content = new Field("content", value.toString(), Store.YES, Index.NO);
    document.add(content);
    try {
      final Set<Object> keySet = _properties.keySet();
      for (final Object xpath : keySet) {
        final Object parsedValueType = _properties.get(xpath);
        String parsedValue = _xPathService.parse(xpath.toString(), value.toString());
        if ("NUMBER".equals(parsedValueType)) {
          parsedValue = NumberPaddingUtil.padding(Double.parseDouble(parsedValue));
        } else if ("DATE".equals(parsedValueType)) {
          final Date date = _dateFormat.parse(parsedValue);
          final GregorianCalendar calendar = new GregorianCalendar();
          calendar.setTime(date);
          parsedValue = "" + calendar.get(GregorianCalendar.YEAR) + calendar.get(GregorianCalendar.MONTH)
              + calendar.get(GregorianCalendar.DAY_OF_MONTH);
        }
        final Field field = new Field("" + xpath.hashCode(), parsedValue, Store.YES, Index.UN_TOKENIZED);
        document.add(field);
      }
    } catch (final Exception e) {
      Logger.warn("can not create document", e);
    }
    return document;
  }

  public Analyzer getIndexAnalyzer() {
    // TODO correct analyzer
    return new StandardAnalyzer();
  }

  public void setXPathService(final IXPathService pathService) {
    _xPathService = pathService;
  }

  public void configure(final JobConf jobConf) throws IOException {
    final String fileName = jobConf.get(XPATH_INPUT_FILE);
    if (fileName != null) {
      final Path xpathInputFile = new Path(fileName);
      final FileSystem fileSystem = FileSystem.get(jobConf);
      final String tmp = System.getProperty("java.io.tmpdir");
      final Path tmpFile = new Path(tmp, XPathDocumentFactory.class.getName());
      fileSystem.copyToLocalFile(xpathInputFile, tmpFile);
      final File file = new File(tmpFile.toString());
      _properties = new Properties();
      _properties.load(new FileInputStream(file));
    } else {
      _properties.load(XPathDocumentFactory.class.getResourceAsStream("/xpath.properties"));
    }
  }

}