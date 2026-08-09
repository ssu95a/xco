package io.github.plainj.xco;

import java.io.StringWriter;
import java.util.Objects;

/** */
public final class XcoJson implements XcoFormat {

    private final Xco xco;

    /** */
    XcoJson( Xco xco ) {
        this.xco = Objects.requireNonNull(xco, "'xco' is null");
    }

    /** */
    public static Xco parse( Object source ) {
        return JsonSupport.read(source);
    }

    /** */
    public static Xco parse( String rootName, Object source ) {
        return JsonSupport.read(rootName, source);
    }

    /** */
    @Override
    public void save( Object target ) {
        save(target, false);
    }

    /** */
    public void save( Object target, boolean pretty ) {
        JsonSupport.write(xco, target, pretty);
    }

    /** */
    @Override
    public String text( ) {
        return text(false);
    }

    /** */
    public String text( boolean pretty )
    {
        StringWriter writer = new StringWriter();

        save(writer, pretty);

        return writer.toString();
    }
}