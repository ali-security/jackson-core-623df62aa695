package com.fasterxml.jackson.core.io;

/**
 * Simple helper class used to encapsulate content reference
 * (source object) along with its offset and length for cases
 * where parsers are created with byte[]/char[] offsets.
 * 
 * @since 2.13
 */
public class ContentReference
{
    private final Object _rawContent;
    private final int _offset;
    private final int _length;
    
    public ContentReference(Object rawContent, int offset, int length) {
        _rawContent = rawContent;
        _offset = offset;
        _length = length;
    }
    
    public ContentReference(Object rawContent) {
        this(rawContent, -1, -1);
    }
    
    public Object getRawContent() {
        return _rawContent;
    }
    
    public int contentOffset() {
        return _offset;
    }
    
    public int contentLength() {
        return _length;
    }
    
    public boolean hasTextualContent() {
        if (_rawContent instanceof CharSequence
                || _rawContent instanceof char[]
                || _rawContent instanceof byte[]) {
            return true;
        }
        return false;
    }
}

