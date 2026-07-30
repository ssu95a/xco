package io.github.plainj.xco;

public class XcoPathException extends XcoException{

    final private static String Msg_Prfx = "[XPath] ";

    public XcoPathException(String message) {
        super( Msg_Prfx + message);
    }

    public XcoPathException(String message, Throwable cause) {
        super( Msg_Prfx + message, cause);
    }
}
