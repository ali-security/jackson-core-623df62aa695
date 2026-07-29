package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.json.JsonReadFeature;

/**
 * Set of additional unit for verifying array parsing, specifically
 * edge cases.
 */
public class ArrayParsingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testValidEmpty() throws Exception
    {
        final String DOC = "[   \n  ]";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();
    }

    public void testInvalidEmptyMissingClose() throws Exception
    {
        final String DOC = "[ ";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected a parsing error for missing array close marker");
        } catch (JsonParseException jex) {
            verifyException(jex, "expected close marker for ARRAY");
        }
        jp.close();
    }

    public void testInvalidMissingFieldName() throws Exception
    {
        final String DOC = "[  : 3 ] ";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected a parsing error for odd character");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unexpected character");
        }
        jp.close();
    }

    public void testInvalidExtraComma() throws Exception
    {
        final String DOC = "[ 24, ] ";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(24, jp.getIntValue());

        try {
            jp.nextToken();
            fail("Expected a parsing error for missing array close marker");
        } catch (JsonParseException jex) {
            verifyException(jex, "expected a value");
        }
        jp.close();
    }
    
    /**
     * Tests the missing value as 'null' in an array 
     * This needs enabling of the Feature.ALLOW_MISSING_VALUES in JsonParser
     * This tests both Stream based parsing and the Reader based parsing
     * @throws Exception
     */
    public void testMissingValueAsNullByEnablingFeature() throws Exception
    {
    	_testMissingValueByEnablingFeature(true);
    	_testMissingValueByEnablingFeature(false);
    }

    /**
     * Tests the missing value in an array by not enabling 
     * the Feature.ALLOW_MISSING_VALUES
     * @throws Exception
     */
    public void testMissingValueAsNullByNotEnablingFeature() throws Exception
    {
    	_testMissingValueNotEnablingFeature(true);
    	_testMissingValueNotEnablingFeature(false);
    }
    
    /**
     * Tests the not missing any value in an array by enabling the 
     * Feature.ALLOW_MISSING_VALUES in JsonParser
     * This tests both Stream based parsing and the Reader based parsing for not missing any value
     * @throws Exception
     */
    public void testNotMissingValueByEnablingFeature() throws Exception
    {
        _testNotMissingValueByEnablingFeature(true);
        _testNotMissingValueByEnablingFeature(false);
    }
    
    public void testDeepNesting() throws Exception
    {
        final String DOC = createDeepNestedDoc(1050);
        JsonParser jp = createParserUsingStream(new JsonFactory(), DOC, "UTF-8");
        try {
            JsonToken jt;
            while ((jt = jp.nextToken()) != null) {

            }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)", e.getMessage());
        }
        jp.close();
    }

    // Same as `testDeepNesting` above but exercising every blocking backend
    // (byte-based, throttled byte-based, char/Reader-based and `DataInput`-based),
    // since the nesting depth check is applied by each of them separately.
    public void testDeepNestingAllModes() throws Exception
    {
        final String DOC = createDeepNestedDoc(1050);
        for (int mode : ALL_MODES) {
            JsonParser jp = createParser(new JsonFactory(), mode, DOC);
            try {
                while (jp.nextToken() != null) { }
                fail("expected StreamConstraintsException (mode: "+mode+")");
            } catch (StreamConstraintsException e) {
                assertEquals("Depth (1001) exceeds the maximum allowed nesting depth (1000)",
                        e.getMessage());
            }
            jp.close();
        }
    }

    // Verify there are no false positives: a document whose maximum nesting depth
    // is 999 (that is, just below the default maximum of 1000) must parse fine.
    // NOTE: `createDeepNestedDoc(d)` reaches depth `1 + 2*d`, hence `d == 499`.
    public void testDeepNestingBelowLimitAllModes() throws Exception
    {
        for (int mode : ALL_MODES) {
            _verifyNestingDepth(new JsonFactory(), mode, 499, 999);
        }
    }

    // And verify the limit is actually configurable, and enforced at the
    // configured (not just the default) value.
    public void testDeepNestingCustomLimitAllModes() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(10).build())
                .build();
        // depth of 21, so well past the configured maximum of 10
        final String DOC = createDeepNestedDoc(10);
        for (int mode : ALL_MODES) {
            JsonParser jp = createParser(f, mode, DOC);
            try {
                while (jp.nextToken() != null) { }
                fail("expected StreamConstraintsException (mode: "+mode+")");
            } catch (StreamConstraintsException e) {
                assertEquals("Depth (11) exceeds the maximum allowed nesting depth (10)",
                        e.getMessage());
            }
            jp.close();
        }

        // ... but a document that stays within the configured limit is fine
        // (`createDeepNestedDoc(4)` reaches depth 9)
        for (int mode : ALL_MODES) {
            _verifyNestingDepth(f, mode, 4, 9);
        }
    }

    // Reads through a document produced by `createDeepNestedDoc(depth)` verifying that
    // no constraint is violated, and that the maximum nesting depth seen is the expected
    // one. NOTE: deliberately does not read past the last token, since `DataInput`-backed
    // parsers do not necessarily report clean end-of-input (see [core#325]).
    private void _verifyNestingDepth(JsonFactory f, int mode, int depth, int expMaxDepth)
        throws Exception
    {
        JsonParser jp = createParser(f, mode, createDeepNestedDoc(depth));
        // START_ARRAY, then (START_OBJECT, FIELD_NAME, START_ARRAY) * depth,
        // then VALUE_STRING, then (END_ARRAY, END_OBJECT) * depth, then END_ARRAY
        final int expTokens = 5 * depth + 3;
        int maxDepth = 0;
        JsonToken t = null;
        for (int i = 0; i < expTokens; ++i) {
            t = jp.nextToken();
            if (t == null) {
                fail("Unexpected end-of-input at token #"+i+" (mode: "+mode+")");
            }
            int d = jp.getParsingContext().getNestingDepth();
            if (d > maxDepth) {
                maxDepth = d;
            }
        }
        assertToken(JsonToken.END_ARRAY, t);
        assertEquals("mode: "+mode, expMaxDepth, maxDepth);
        jp.close();
    }

    public void testMaxNestingDepthConfig() throws Exception
    {
        assertEquals(1000, StreamReadConstraints.DEFAULT_MAX_DEPTH);
        assertEquals(1000, StreamReadConstraints.builder().build().getMaxNestingDepth());
        assertEquals(25, StreamReadConstraints.builder().maxNestingDepth(25).build().getMaxNestingDepth());
        try {
            StreamReadConstraints.builder().maxNestingDepth(-1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            verifyException(e, "Cannot set maxNestingDepth to a negative value");
        }
    }

    private void _testMissingValueByEnablingFeature(boolean useStream) throws Exception {
        String DOC = "[ \"a\",,,,\"abc\", ] ";

        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .build();
        JsonParser jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
   			          : createParserUsingReader(f, DOC);
        
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("a", jp.getValueAsString());
        
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
             
        jp.close();

        // And another take
        DOC = "[,] ";
        jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
                : createParserUsingReader(f, DOC);

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());

        jp.close();
    }
    
    private void _testMissingValueNotEnablingFeature(boolean useStream) throws Exception {
    	final String DOC = "[ \"a\",,\"abc\"] ";

    	JsonFactory f = new JsonFactory();
    	
        JsonParser jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
   			          : createParserUsingReader(f, DOC);
        
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("a", jp.getValueAsString());
        try {
	        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
	        fail("Expecting exception here");
        }
        catch(JsonParseException ex){
        	verifyException(ex, "expected a valid value", "expected a value");
        }
        jp.close();
    }
    
    private void _testNotMissingValueByEnablingFeature(boolean useStream) throws Exception {
        final String DOC = "[ \"a\",\"abc\"] ";

        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
                .build();
        JsonParser jp = useStream ? createParserUsingStream(f, DOC, "UTF-8")
   			          : createParserUsingReader(f, DOC);
        
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("a", jp.getValueAsString());
        
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
             
        jp.close();
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
}
