package net.logstash.log4j;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.apache.log4j.*;

import junit.framework.Assert;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Created with IntelliJ IDEA.
 * User: jvincent
 * Date: 12/5/12
 * Time: 12:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class JSONEventLayoutTest {
    static Logger logger;
    static MockAppender appender;
    static final String[] logstashFields = new String[] {
            "@message",
            "@source_host",
            "@fields",
            "@timestamp"
    };

    @BeforeClass
    public static void setupTestAppender(){
        appender = new MockAppender(new JSONEventLayout());
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappender");
        appender.activateOptions();
        logger.addAppender(appender);
    }

    @After
    public void clearTestAppender(){
        NDC.clear();
        appender.clear();
        appender.close();
    }

    @Test
    public void testJSONEventLayoutIsJSON() {
        logger.info("this is an info message");
        String  message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
    }

    @Test
    public void testJSONEventLayoutHasKeys(){
        logger.info("this is a test message");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        for(String fieldName : logstashFields){
            Assert.assertTrue("Event does not contain field: " + fieldName, jsonObject.containsKey(fieldName));
        }
    }

    @Test
    public void testJSONEventLayoutHasFieldLevel(){
        logger.fatal("this is a new test message");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertEquals("Log level is wrong", "FATAL", atFields.get("level"));
    }

    @Test
    public void testJSONEventLayoutHasNDC(){
        String ndcData = new String("json-layout-test");
        NDC.push(ndcData);
        logger.warn("I should have NDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertEquals("NDC is wrong", ndcData, atFields.get("ndc"));
    }

    @Test
    public void testJSONEventLayoutExceptions(){
        String exceptionMessage = new String("shits on fire, yo");
        logger.fatal("uh-oh",new IllegalArgumentException(exceptionMessage));
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        JSONObject exceptionInformation = (JSONObject) atFields.get("exception");

        Assert.assertEquals("Exception class missing","java.lang.IllegalArgumentException",exceptionInformation.get("exception_class"));
        Assert.assertEquals("Exception exception message",exceptionMessage,exceptionInformation.get("exception_message"));
    }

    @Test
    public void testJSONEventLayoutHasClassName() {
        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertEquals("Logged class does not match",this.getClass().getCanonicalName().toString(),atFields.get("class"));
    }

    @Test
    public void testJSONEventHasFileName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertNotNull("File value is missing", atFields.get("file"));
    }

    @Test
    public void testJSONEventLayoutNoLocationInfo() {
        JSONEventLayout layout = (JSONEventLayout)appender.getLayout();
        boolean prevLocationInfo = layout.getLocationInfo();

        layout.setLocationInfo(false);

        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertFalse("atFields contains file value", atFields.containsKey("file"));
        Assert.assertFalse("atFields contains line_number value", atFields.containsKey("line_number"));
        Assert.assertFalse("atFields contains class value", atFields.containsKey("class"));
        Assert.assertFalse("atFields contains method value", atFields.containsKey("method"));

        // Revert the change to the layout to leave it as we found it.
        layout.setLocationInfo(prevLocationInfo);
    }

    @Test
    @Ignore
    public void measureJSONEventLayoutLocationInfoPerformance() {
        JSONEventLayout layout = (JSONEventLayout)appender.getLayout();
        boolean locationInfo = layout.getLocationInfo();
        int iterations = 100000;
        long start, stop;

        start = System.currentTimeMillis();
        for (int i=0; i<iterations; i++){
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long firstMeasurement = stop - start;

        layout.setLocationInfo(!locationInfo);
        start = System.currentTimeMillis();
        for (int i=0; i<iterations; i++){
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long secondMeasurement = stop - start;

        System.out.println("First Measurement (locationInfo: " + locationInfo +"): " + firstMeasurement);
        System.out.println("Second Measurement (locationInfo: " + !locationInfo +"): " + secondMeasurement);

        // Clean up
        layout.setLocationInfo(!locationInfo);
    }

}
