package io.github.plainj.xco;

public class XcoWriteException extends XcoException{

    final private static String Msg_Prfx = "[XML] ";

    public XcoWriteException(String message, Throwable cause) {
        super( Msg_Prfx + message, cause);
    }

    public XcoWriteException(String message) {
        super( Msg_Prfx + message);
    }
}
