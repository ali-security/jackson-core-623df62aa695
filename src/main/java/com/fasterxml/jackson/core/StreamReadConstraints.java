package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.exc.StreamConstraintsException;

/**
 * The constraints for streaming reads.
 */
public class StreamReadConstraints {

    /**
     * Default setting for maximum depth: see {@link Builder#maxNestingDepth(int)} for details.
     */
    public static final int DEFAULT_MAX_DEPTH = 1000;

    private static final int DEFAULT_MAX_NUM_LEN = 1000;

    protected final int _maxNestingDepth;
    private final int _maxNumLen;

    public static final StreamReadConstraints UNLIMITED = new StreamReadConstraints(DEFAULT_MAX_DEPTH, Integer.MAX_VALUE);

    public static final class Builder {
        private int maxNestingDepth;
        private int _maxNumLen = StreamReadConstraints.DEFAULT_MAX_NUM_LEN;

        /**
         * Sets the maximum nesting depth. The depth is a count of objects and arrays that have not
         * been closed, `{` and `[` respectively.
         *
         * @param maxNestingDepth the maximum depth
         *
         * @return this builder
         * @throws IllegalArgumentException if the maxNestingDepth is set to a negative value
         *
         * @since 2.15
         */
        public Builder maxNestingDepth(final int maxNestingDepth) {
            if (maxNestingDepth < 0) {
                throw new IllegalArgumentException("Cannot set maxNestingDepth to a negative value");
            }
            this.maxNestingDepth = maxNestingDepth;
            return this;
        }

        /**
         * Sets the maximum number length (in chars or bytes, depending on input context).
         * The default is 1000 (since Jackson 2.14).
         * @param maxNumLen the maximum number length (in chars or bytes, depending on input context)
         * @return this builder
         */
        public Builder withMaxNumberLength(int maxNumLen) {
            _maxNumLen = maxNumLen;
            return this;
        }


        Builder() {
            this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_NUM_LEN);
        }

        Builder(final int maxNestingDepth, final int maxNumLen) {
            this.maxNestingDepth = maxNestingDepth;
            this._maxNumLen = maxNumLen;
        }

        public StreamReadConstraints build() {
            return new StreamReadConstraints(maxNestingDepth, _maxNumLen);
        }
    }

    public static Builder builder() {
        return new Builder();
    }


    private StreamReadConstraints(int maxNestingDepth, int maxNumLen) {
        _maxNestingDepth = maxNestingDepth;
        _maxNumLen = maxNumLen;
    }

    /**
     * Accessor for maximum depth.
     * see {@link Builder#maxNestingDepth(int)} for details.
     *
     * @return Maximum allowed depth
     */
    public int getMaxNestingDepth() {
        return _maxNestingDepth;
    }

    public int getMaxNumberLength() {
        return _maxNumLen;
    }

    /**
     * Convenience method that can be used to verify that the
     * nesting depth does not exceed the maximum specified by this
     * constraints object: if it does, a
     * {@link StreamConstraintsException}
     * is thrown.
     *
     * @param depth count of unclosed objects and arrays
     *
     * @throws StreamConstraintsException If depth exceeds maximum
     */
    public void validateNestingDepth(int depth) throws StreamConstraintsException
    {
        if (depth > _maxNestingDepth) {
            throw new StreamConstraintsException(String.format("Depth (%d) exceeds the maximum allowed nesting depth (%d)",
                    depth, _maxNestingDepth));
        }
    }

    public void validateIntegerLength(int length) throws StreamConstraintsException
    {
        if (length > _maxNumLen) {
            throw new StreamConstraintsException(
                String.format("Number value length (%d) exceeds the maximum allowed (%d)",
                    length, _maxNumLen));
        }
    }

    public void validateFPLength(int length) throws StreamConstraintsException
    {
        if (length > _maxNumLen) {
            throw new StreamConstraintsException(
                String.format("Number value length (%d) exceeds the maximum allowed (%d)",
                    length, _maxNumLen));
        }
    }

}
