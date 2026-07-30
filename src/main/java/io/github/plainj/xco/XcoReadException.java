package io.github.plainj.xco;

/** */
public class XcoReadException extends XcoException {

    final private static String Msg_Prfx = "[XML] ";

    public XcoReadException(String message) {
        super(Msg_Prfx + message);
    }

    public XcoReadException(String message, Throwable cause) {
        super(Msg_Prfx + message, cause);
    }
}
