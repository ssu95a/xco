package io.github.plainj.xco;

/** */
public class XcoException extends RuntimeException {

    public XcoException(String message) {
        super(message);
    }

    public XcoException(String message, Throwable cause) {
        super(message, cause);
    }

    public XcoException(Throwable cause) {
        super(cause);
    }

    /** */
    public static XcoException toXcoException( String context, Throwable th )
    {
        if( th instanceof XcoException )
            return (XcoException)th;

        if( th instanceof NullPointerException )
            return new XcoException("NPE happened at: " + context );

        return new XcoException(th);
    }
}
