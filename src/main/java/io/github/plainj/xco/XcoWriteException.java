package io.github.plainj.xco;

public class XcoWriteException extends XcoException{

    public XcoWriteException(String message, Throwable cause) {
        super( message, cause);
    }

    public XcoWriteException(String message) { super(  message);  }
}
