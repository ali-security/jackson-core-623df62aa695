package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * Number Length Constraint Bypass in Non-Blocking (Async) JSON Parsers
 *
 * Tests for GHSA-72hv-8253-57qq: The non-blocking (async) JSON parser bypasses
 * the maxNumberLength constraint, allowing arbitrarily long numbers that can
 * lead to DoS via memory/CPU exhaustion.
 *
 * @since 2.12.7
 */
public class AsyncLargeNumberReadTest
    extends BaseTest
{
    private static final int TEST_NUMBER_LENGTH = 2000; // StreamReadConstraints.DEFAULT_MAX_NUM_LEN * 2;

    private final JsonFactory JSON_F = newStreamFactory();

    public void testAsyncParserFailsTooLongInt() throws Exception {
        byte[] payload = buildPayloadWithLongInteger(TEST_NUMBER_LENGTH);

        JsonParser p = JSON_F.createNonBlockingByteArrayParser();
        try {
            ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p.getNonBlockingInputFeeder();
            byteArrayFeeder.feedInput(payload, 0, payload.length);
            byteArrayFeeder.endOfInput();

            _asyncParserFailsTooLongNumber(p, JsonToken.VALUE_NUMBER_INT);
            fail("Async parser must reject a " + TEST_NUMBER_LENGTH + "-digit number");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length (");
            verifyException(e, "exceeds the maximum allowed");
        } finally {
            p.close();
        }
    }

    public void testAsyncParserFailsTooLongDecimal() throws Exception {
        byte[] payload = buildPayloadWithLongDecimal(TEST_NUMBER_LENGTH);

        JsonParser p = JSON_F.createNonBlockingByteArrayParser();
        try {
            ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p.getNonBlockingInputFeeder();
            byteArrayFeeder.feedInput(payload, 0, payload.length);
            byteArrayFeeder.endOfInput();

            _asyncParserFailsTooLongNumber(p, JsonToken.VALUE_NUMBER_FLOAT);
            fail("Async parser must reject a " + TEST_NUMBER_LENGTH + "-digit decimal");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length (");
            verifyException(e, "exceeds the maximum allowed");
        } finally {
            p.close();
        }
    }

    public void testAsyncParserFailsTooLongDecimalWithExponent() throws Exception {
        byte[] payload = buildPayloadWithLongExponent(TEST_NUMBER_LENGTH);

        JsonParser p = JSON_F.createNonBlockingByteArrayParser();
        try {
            ByteArrayFeeder byteArrayFeeder = (ByteArrayFeeder) p.getNonBlockingInputFeeder();
            byteArrayFeeder.feedInput(payload, 0, payload.length);
            byteArrayFeeder.endOfInput();

            _asyncParserFailsTooLongNumber(p, JsonToken.VALUE_NUMBER_FLOAT);
            fail("Async parser must reject a " + TEST_NUMBER_LENGTH + "-digit exponent");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Number value length (");
            verifyException(e, "exceeds the maximum allowed");
        } finally {
            p.close();
        }
    }

    private void _asyncParserFailsTooLongNumber(JsonParser p, JsonToken tokenMatch) throws Exception {
        boolean foundNumber = false;
        while (p.nextToken() != null) {
            if (p.currentToken() == tokenMatch) {
                foundNumber = true;
                String numberText = p.getText();
                assertEquals("Async parser silently accepted all " + TEST_NUMBER_LENGTH + " digits",
                        TEST_NUMBER_LENGTH, numberText.length());
            }
        }
        // If we reach here without exception, the test has failed
        assertTrue("Number should have been found", foundNumber);
    }

    private byte[] buildPayloadWithLongInteger(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        sb.append('}');
        return utf8Bytes(sb.toString());
    }

    private byte[] buildPayloadWithLongDecimal(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":0.");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        sb.append('}');
        return utf8Bytes(sb.toString());
    }

    private byte[] buildPayloadWithLongExponent(int numDigits) {
        StringBuilder sb = new StringBuilder(numDigits + 10);
        sb.append("{\"v\":1.1E");
        for (int i = 0; i < numDigits; i++) {
            sb.append((char) ('1' + (i % 9)));
        }
        sb.append('}');
        return utf8Bytes(sb.toString());
    }
}
