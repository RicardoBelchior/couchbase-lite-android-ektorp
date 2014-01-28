package com.couchbase.lite.testapp.ektorp.tests;

import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;
import com.couchbase.lite.ektorp.CBLiteHttpClient;

import junit.framework.Assert;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.http.HttpClient;
import org.ektorp.impl.StdCouchDbInstance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Views extends CBLiteEktorpTestCase {

    public static final String dDocName = "ddoc";
    public static final String dDocId = "_design/" + dDocName;
    public static final String viewName = "aview";
    public static final String viewReduceName = "aviewreduce";

    public void putDocs(CouchDbConnector db) {

        TestObject obj2 = new TestObject("22222", "two");
        db.create(obj2);

        TestObject obj4 = new TestObject("44444", "four");
        db.create(obj4);

        TestObject obj1 = new TestObject("11111", "one");
        db.create(obj1);

        TestObject obj3 = new TestObject("33333", "three");
        db.create(obj3);

        TestObject obj5 = new TestObject("55555", "five");
        db.create(obj5);

    }

    public static View createView(Database db) {
        View view = db.getView(String.format("%s/%s", dDocName, viewName));
        view.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                Assert.assertNotNull(document.get("_id"));
                Assert.assertNotNull(document.get("_rev"));
                if(document.get("key") != null) {
                    emitter.emit(document.get("key"), null);
                }
            }
        }, "1");
        return view;
    }

    public static View createViewWithReduce(Database db) {
        View view = db.getView(String.format("%s/%s", dDocName, viewReduceName));
        view.setMapReduce(new Mapper() {
                              @Override
                              public void map(Map<String, Object> document, Emitter emitter) {
                                  Assert.assertNotNull(document.get("_id"));
                                  Assert.assertNotNull(document.get("_rev"));
                                  if (document.get("key") != null) {
                                      emitter.emit(document.get("key"), 1);
                                  }
                              }
                          }, new Reducer() {
                              @Override
                              public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                                  return View.totalValues(values);
                              }
                          }, "1"
        );
        return view;
    }

    public void testViewQuery() throws IOException {

        HttpClient httpClient = new CBLiteHttpClient(manager);
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);

        CouchDbConnector dbConnector = dbInstance.createConnector(DEFAULT_TEST_DB, true);

        putDocs(dbConnector);
        createView(database);

        ViewQuery query = new ViewQuery().designDocId(dDocId).viewName(viewName);
        ViewResult result = dbConnector.queryView(query);
        Assert.assertEquals(5, result.getTotalRows());
        Assert.assertEquals("five", result.getRows().get(0).getKey());
        Assert.assertEquals("four", result.getRows().get(1).getKey());
        Assert.assertEquals("one", result.getRows().get(2).getKey());
        Assert.assertEquals("three", result.getRows().get(3).getKey());
        Assert.assertEquals("two", result.getRows().get(4).getKey());

        // Start/end key query:
        query = new ViewQuery().designDocId(dDocId).viewName(viewName).startKey("a").endKey("one");
        result = dbConnector.queryView(query);
        Assert.assertEquals(3, result.getTotalRows());
        Assert.assertEquals("five", result.getRows().get(0).getKey());
        Assert.assertEquals("four", result.getRows().get(1).getKey());
        Assert.assertEquals("one", result.getRows().get(2).getKey());

        // Start/end query without inclusive end:
        query = new ViewQuery().designDocId(dDocId).viewName(viewName).startKey("a").endKey("one").inclusiveEnd(false);
        result = dbConnector.queryView(query);
        Assert.assertEquals(2, result.getTotalRows());
        Assert.assertEquals("five", result.getRows().get(0).getKey());
        Assert.assertEquals("four", result.getRows().get(1).getKey());

        // Reversed:
        query = new ViewQuery().designDocId(dDocId).viewName(viewName).startKey("o").endKey("five").inclusiveEnd(true).descending(true);
        result = dbConnector.queryView(query);
        Assert.assertEquals(2, result.getTotalRows());
        Assert.assertEquals("four", result.getRows().get(0).getKey());
        Assert.assertEquals("five", result.getRows().get(1).getKey());

        // Reversed, no inclusive end:
        query = new ViewQuery().designDocId(dDocId).viewName(viewName).startKey("o").endKey("five").inclusiveEnd(false).descending(true);
        result = dbConnector.queryView(query);
        Assert.assertEquals(1, result.getTotalRows());
        Assert.assertEquals("four", result.getRows().get(0).getKey());

        // Specific keys:
        List<String> keys = new ArrayList<String>();
        keys.add("two");
        keys.add("four");
        query = new ViewQuery().designDocId(dDocId).viewName(viewName).keys(keys);
        result = dbConnector.queryView(query);
        Assert.assertEquals(2, result.getTotalRows());
        Assert.assertEquals("four", result.getRows().get(0).getKey());
        Assert.assertEquals("two", result.getRows().get(1).getKey());

        // Limit
        query = new ViewQuery().designDocId(dDocId).viewName(viewName).limit(3);
        result = dbConnector.queryView(query);
        Assert.assertEquals(3, result.getTotalRows());
        Assert.assertEquals("five", result.getRows().get(0).getKey());
        Assert.assertEquals("four", result.getRows().get(1).getKey());
        Assert.assertEquals("one", result.getRows().get(2).getKey());

        // Limit & Skip
        query = new ViewQuery().designDocId(dDocId).viewName(viewName).limit(2).skip(1);
        result = dbConnector.queryView(query);
        Assert.assertEquals(2, result.getTotalRows());
        Assert.assertEquals("four", result.getRows().get(0).getKey());
        Assert.assertEquals("one", result.getRows().get(1).getKey());
    }

    public void testViewReduceQuery() throws IOException {

        HttpClient httpClient = new CBLiteHttpClient(manager);
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);

        CouchDbConnector dbConnector = dbInstance.createConnector(DEFAULT_TEST_DB, true);

        putDocs(dbConnector);
        createViewWithReduce(database);


        //because this view has a reduce function it should default to reduce=true
        ViewQuery query = new ViewQuery().designDocId(dDocId).viewName(viewReduceName);
        ViewResult result = dbConnector.queryView(query);

        Assert.assertEquals(1, result.getTotalRows());
        Assert.assertEquals(5, result.getRows().get(0).getValueAsInt());

        //we should still be able to override it and force reduce=false
        query = new ViewQuery().designDocId(dDocId).viewName(viewReduceName).reduce(false);
        result = dbConnector.queryView(query);

        Assert.assertEquals(5, result.getTotalRows());
    }

    public void testViewQueryFromWeb() throws IOException {

        HttpClient httpClient = new CBLiteHttpClient(manager);
        CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);

        CouchDbConnector couchDbConnector = dbInstance.createConnector(DEFAULT_TEST_DB, true);

        String dDocName = "ddoc";
        String viewName = "people";
        View view = database.getView(String.format("%s/%s", dDocName, viewName));

        view.setMapReduce(new Mapper() {
                              @Override
                              public void map(Map<String, Object> document, Emitter emitter) {
                                  String type = (String) document.get("type");
                                  if ("person".equals(type)) {
                                      emitter.emit(null, document.get("_id"));
                                  }
                              }
                          }, new Reducer() {
                              @Override
                              public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                                  return null;
                              }
                          }, "1"
        );

        ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + dDocName).viewName(viewName);
        //viewQuery.descending(true); //use this to reverse the sorting order of the view
        ViewResult viewResult = couchDbConnector.queryView(viewQuery);

        Assert.assertEquals(0, viewResult.getTotalRows());

    }
}
