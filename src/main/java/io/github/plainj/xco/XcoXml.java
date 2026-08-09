package io.github.plainj.xco;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Objects;

/** */
public final class XcoXml implements XcoFormat {

    private final Xco xco;

    /** */
    XcoXml( Xco xco ) {
        this.xco = Objects.requireNonNull(xco, "'xco' is null");
    }

    /** */
    public static Xco parse( Object source ) {
        return Xco.wrap(XmlSupport.read(source, null));
    }

    /** */
    public static Xco parse( Object source, Charset charset ) {
        return Xco.wrap(XmlSupport.read(source, charset));
    }

    /** */
    @Override
    public void save( Object target ) {
        save(target, null, false);
    }

    /** */
    public void save( Object target, Charset charset ) {
        save(target, charset, false);
    }

    /** */
    public void save(
            Object target,
            Charset charset,
            boolean declaration
    ) {
        XmlSupport.write(
                xco.node(),
                target,
                charset,
                declaration
        );
    }

    /** */
    @Override
    public String text( ) {
        return text(null, false);
    }

    /** */
    public String text( boolean declaration ) {
        return text(null, declaration);
    }

    /** */
    public String text( Charset charset ) {
        return text(charset, false);
    }

    /** */
    public String text(
            Charset charset,
            boolean declaration
    )
    {
        StringWriter writer = new StringWriter();

        save(writer, charset, declaration);

        return writer.toString();
    }
}