/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.ext.search.lucene;

import java.util.Collections;

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.SearchConditionVisitor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractLuceneQueryVisitorTest extends Assert {

    private DirectoryReader ireader;
    private IndexSearcher isearcher;
    private Directory directory;
    private Analyzer analyzer;

    @Before
    public void setUp() throws Exception {
        analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
        directory = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);

        Document doc = new Document();
        doc.add(new Field("contents", "name=text", TextField.TYPE_STORED));

        IntField intField = new IntField("intfield", 4, Field.Store.YES);
        doc.add(intField);
        iwriter.addDocument(doc);

        iwriter.close();
        ireader = DirectoryReader.open(directory);
        isearcher = new IndexSearcher(ireader);
    }

    @After
    public void tearDown() throws Exception {
        ireader.close();
        directory.close();
    }

    protected abstract SearchConditionParser<SearchBean> getParser();

    protected void doTestTextContentMatch(String expression) throws Exception {
        doTestTextContentMatch(expression, false);
    }

    protected void doTestTextContentMatchWithAnalyzer(String expression) throws Exception {
        doTestTextContentMatch(expression, true);
    }

    protected void doTestTextContentMatch(String expression, boolean useAnalyzer) throws Exception {
        Query query = createTermQuery("contents", expression, useAnalyzer);
        doTestTextContentMatchWithQuery(query);

    }

    protected void doTestNoMatch(Query query) throws Exception {
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(0, hits.length);
    }

    protected void doTestTextContentMatchWithQuery(Query query) throws Exception {
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(1, hits.length);
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            assertEquals("name=text", hitDoc.get("contents"));
        }

    }

    protected void doTestIntContentMatch(String expression) throws Exception {

        Query query = createTermQuery("intfield", expression);
        doTestIntContentMatchWithQuery(query);

    }

    protected void doTestIntContentMatchWithQuery(Query query) throws Exception {

        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(1, hits.length);
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            IndexableField field = hitDoc.getField("intfield");
            assertEquals(4, field.numericValue().intValue());
        }

    }

    protected Query createTermQuery(String expression) throws Exception {
        return createTermQuery(expression, false);
    }

    protected Query createTermQueryWithAnalyzer(String expression) throws Exception {
        return createTermQuery(expression, true);
    }

    protected Query createTermQuery(String expression, boolean useAnalyzer) throws Exception {
        SearchCondition<SearchBean> filter = getParser().parse(expression);
        SearchConditionVisitor<SearchBean, Query> lucene =
            new LuceneQueryVisitor<SearchBean>(useAnalyzer ? analyzer : null);
        lucene.visit(filter);
        return lucene.getQuery();
    }

    protected Query createTermQueryWithFieldClassWithAnalyzer(String expression, Class<?> cls) throws Exception {
        return createTermQueryWithFieldClass(expression, cls, true);
    }

    protected Query createTermQueryWithFieldClass(String expression, Class<?> cls) throws Exception {
        return createTermQueryWithFieldClass(expression, cls, false);
    }

    protected Query createTermQueryWithFieldClass(String expression, Class<?> cls,
            boolean useAnalyzer) throws Exception {
        SearchCondition<SearchBean> filter = getParser().parse(expression);
        LuceneQueryVisitor<SearchBean> lucene =
            new LuceneQueryVisitor<SearchBean>(useAnalyzer ? analyzer : null);
        lucene.setPrimitiveFieldTypeMap(Collections.<String, Class<?>>singletonMap("intfield", cls));
        lucene.visit(filter);
        return lucene.getQuery();
    }

    protected Query createTermQuery(String fieldName, String expression) throws Exception {
        return createTermQuery(fieldName, expression, false);
    }

    protected Query createTermQuery(String fieldName, String expression, boolean useAnalyzer) throws Exception {
        SearchCondition<SearchBean> filter = getParser().parse(expression);
        LuceneQueryVisitor<SearchBean> lucene =
            new LuceneQueryVisitor<SearchBean>("ct", fieldName, useAnalyzer ? analyzer : null);
        lucene.visit(filter);
        return lucene.getQuery();
    }

    protected Query createTermQueryWithFieldClass(String fieldName, String expression, Class<?> cls)
        throws Exception {
        SearchCondition<SearchBean> filter = getParser().parse(expression);
        LuceneQueryVisitor<SearchBean> lucene =
            new LuceneQueryVisitor<SearchBean>("ct", fieldName);
        lucene.setPrimitiveFieldTypeMap(Collections.<String, Class<?>>singletonMap(fieldName, cls));
        lucene.visit(filter);
        return lucene.getQuery();
    }

    protected Query createPhraseQuery(String fieldName, String expression) throws Exception {
        return createPhraseQuery(fieldName, expression, false);
    }

    protected Query createPhraseQueryWithAnalyzer(String fieldName, String expression) throws Exception {
        return createPhraseQuery(fieldName, expression, true);
    }

    protected Query createPhraseQuery(String fieldName, String expression, boolean useAnalyzer) throws Exception {
        SearchCondition<SearchBean> filter = getParser().parse(expression);
        LuceneQueryVisitor<SearchBean> lucene =
            new LuceneQueryVisitor<SearchBean>(fieldName, useAnalyzer ? analyzer : null);
        lucene.visit(filter);
        return lucene.getQuery();
    }
}
