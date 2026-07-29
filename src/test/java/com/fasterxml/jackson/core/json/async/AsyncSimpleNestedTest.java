package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncSimpleNestedTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    /*
    /**********************************************************************
    /* Test methods, success
    /**********************************************************************
     */

    public void testStuffInObject() throws Exception
    {
        byte[] data = _jsonDoc(aposToQuotes(
                "{'foobar':[1,2,-999],'emptyObject':{},'emptyArray':[], 'other':{'':null} }"));

        JsonFactory f = JSON_F;
        _testStuffInObject(f, data, 0, 100);
        _testStuffInObject(f, data, 0, 3);
        _testStuffInObject(f, data, 0, 1);

        _testStuffInObject(f, data, 1, 100);
        _testStuffInObject(f, data, 1, 3);
        _testStuffInObject(f, data, 1, 1);
    }

    private void _testStuffInObject(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertFalse(r.parser().hasTextCharacters());

        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("foobar", r.currentName());
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertEquals("[", r.currentText());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(1, r.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(2, r.getIntValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(-999, r.getIntValue());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("emptyObject", r.currentName());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        

        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("emptyArray", r.currentName());
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("other", r.currentName());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("", r.currentName());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        // another twist: close in the middle, verify
        r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        r.parser().close();
        assertTrue(r.parser().isClosed());
        assertNull(r.parser().nextToken());
    }

    public void testStuffInArray() throws Exception
    {
        byte[] data = _jsonDoc(aposToQuotes("[true,{'moreStuff':0},[null],{'extraOrdinary':23}]"));
        JsonFactory f = JSON_F;

        _testStuffInArray(f, data, 0, 100);
        _testStuffInArray(f, data, 0, 3);
        _testStuffInArray(f, data, 0, 1);

        _testStuffInArray(f, data, 3, 100);
        _testStuffInArray(f, data, 3, 3);
        _testStuffInArray(f, data, 3, 1);
    }

    private void _testStuffInArray(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertFalse(r.parser().hasTextCharacters());

        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertEquals("{", r.currentText());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("moreStuff", r.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(0L, r.getLongValue());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_ARRAY, r.nextToken());
        assertToken(JsonToken.VALUE_NULL, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals("extraOrdinary", r.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(23, r.getIntValue());
        assertToken(JsonToken.END_OBJECT, r.nextToken());
        assertToken(JsonToken.END_ARRAY, r.nextToken());
    }

    final static String SHORT_NAME = String.format("u-%s", UNICODE_SEGMENT);
    final static String LONG_NAME = String.format("Unicode-with-some-longer-name-%s", UNICODE_SEGMENT);
    
    public void testStuffInArray2() throws Exception
    {
        byte[] data = _jsonDoc(aposToQuotes(String.format(
                "[{'%s':true},{'%s':false},{'%s':true},{'%s':false}]",
                SHORT_NAME, LONG_NAME, LONG_NAME, SHORT_NAME)));
        JsonFactory f = JSON_F;

        _testStuffInArray2(f, data, 0, 100);
        _testStuffInArray2(f, data, 0, 3);
        _testStuffInArray2(f, data, 0, 1);

        _testStuffInArray2(f, data, 3, 100);
        _testStuffInArray2(f, data, 3, 3);
        _testStuffInArray2(f, data, 3, 1);
    }

    private void _testStuffInArray2(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(SHORT_NAME, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(LONG_NAME, r.currentName());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(LONG_NAME, r.currentName());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.START_OBJECT, r.nextToken());
        assertToken(JsonToken.FIELD_NAME, r.nextToken());
        assertEquals(SHORT_NAME, r.currentName());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());
        assertToken(JsonToken.END_OBJECT, r.nextToken());

        assertToken(JsonToken.END_ARRAY, r.nextToken());
    }
    
    /*
    /**********************************************************************
    /* Test methods, nesting depth limits
    /**********************************************************************
     */

    // Verify that the maximum nesting depth is enforced by the non-blocking
    // parser as well (that is, by `_startArrayScope()` / `_startObjectScope()`).
    public void testDeepNesting() throws Exception
    {
        byte[] data = _jsonDoc(createDeepNestedDoc(1050));
        _testDeepNesting(JSON_F, data, 0, 100, 1001, 1000);
        _testDeepNesting(JSON_F, data, 1, 7, 1001, 1000);
    }

    public void testDeepNestingCustomLimit() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(10).build())
                .build();
        byte[] data = _jsonDoc(createDeepNestedDoc(10));
        _testDeepNesting(f, data, 0, 100, 11, 10);
        _testDeepNesting(f, data, 0, 1, 11, 10);
        _testDeepNesting(f, data, 3, 3, 11, 10);
    }

    private void _testDeepNesting(JsonFactory f, byte[] data, int offset, int readSize,
            int expDepth, int expMaxDepth) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        try {
            while (r.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals(String.format("Depth (%d) exceeds the maximum allowed nesting depth (%d)",
                    expDepth, expMaxDepth), e.getMessage());
        }
        r.close();
    }

    // No false positives: a document whose maximum nesting depth is 999 (just below
    // the default maximum of 1000) must be parsed fine. NOTE: `createDeepNestedDoc(d)`
    // reaches depth `1 + 2*d`, hence `d == 499`.
    public void testDeepNestingBelowLimit() throws Exception
    {
        byte[] data = _jsonDoc(createDeepNestedDoc(499));
        _testDeepNestingBelowLimit(JSON_F, data, 0, 100, 999);
        _testDeepNestingBelowLimit(JSON_F, data, 2, 7, 999);
    }

    private void _testDeepNestingBelowLimit(JsonFactory f, byte[] data, int offset,
            int readSize, int expMaxDepth) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        int maxDepth = 0;
        while (r.nextToken() != null) {
            int depth = r.getParsingContext().getNestingDepth();
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        assertEquals(expMaxDepth, maxDepth);
        r.close();
    }

    private String createDeepNestedDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < depth; i++) {
            sb.append("{ \"a\": [");
        }
        sb.append(" \"val\" ");
        for (int i = 0; i < depth; i++) {
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }

    /*
    /**********************************************************************
    /* Test methods, fail checking
    /**********************************************************************
     */

    public void testMismatchedArray() throws Exception
    {
        byte[] data = _jsonDoc(aposToQuotes("[  }"));

        JsonFactory f = JSON_F;
        _testMismatchedArray(f, data, 0, 99);
        _testMismatchedArray(f, data, 0, 3);
        _testMismatchedArray(f, data, 0, 2);
        _testMismatchedArray(f, data, 0, 1);

        _testMismatchedArray(f, data, 1, 3);
        _testMismatchedArray(f, data, 1, 1);
    }

    private void _testMismatchedArray(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_ARRAY, r.nextToken());
        try {
            r.nextToken();
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected close marker '}': expected ']'");
        }
    }

    public void testMismatchedObject() throws Exception
    {
        byte[] data = _jsonDoc(aposToQuotes("{ ]"));

        JsonFactory f = JSON_F;
        _testMismatchedObject(f, data, 0, 99);
        _testMismatchedObject(f, data, 0, 3);
        _testMismatchedObject(f, data, 0, 2);
        _testMismatchedObject(f, data, 0, 1);

        _testMismatchedObject(f, data, 1, 3);
        _testMismatchedObject(f, data, 1, 1);
    }

    private void _testMismatchedObject(JsonFactory f,
            byte[] data, int offset, int readSize) throws IOException
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        assertToken(JsonToken.START_OBJECT, r.nextToken());
        try {
            r.nextToken();
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected close marker ']': expected '}'");
        }
    }
}
