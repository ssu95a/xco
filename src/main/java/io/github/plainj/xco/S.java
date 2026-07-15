package io.github.plainj.xco;

/** */
final class S {

    private S() {
    }

    /** */
    static boolean isNullOrEmpty( CharSequence value ) {
        return value == null || value.length() == 0;
    }

    /** */
    static boolean isBlank( CharSequence value )
    {
        if( value == null )
            return true;

        for( int i = 0; i < value.length(); i++ )
        {
            if( !Character.isWhitespace(value.charAt(i)) )
                return false;
        }

        return true;
    }
}