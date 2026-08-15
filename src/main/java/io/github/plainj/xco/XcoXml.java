package io.github.plainj.xco;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

/** */
public final class XcoXml implements XcoFormat {

    private static final String STRIP_NAMESPACES_XSLT = "/io/github/plainj/xco/strip-namespaces.xsl";

    private final Xco xco;

    /**
     * c'tor
     */
    XcoXml(Xco xco) {
        this.xco = Objects.requireNonNull(xco, "'xco' is null");
    }

    /**
     *
     */
    public static Xco parse(Object source) {
        return Xco.wrap(XmlSupport.read(source, null));
    }

    /**
     *
     */
    public static Xco parse( Object source, Charset charset ) {
        return Xco.wrap( XmlSupport.read(source, charset) );
    }

    /**
     *
     */
    @Override
    public void save( Object target ) {
        save(target, null, false);
    }

    /**
     *
     */
    public void save( Object target, Charset charset ) {
        save(target, charset, false);
    }

    /**
     *
     */
    public void save( Object target, Charset charset, boolean declaration )
    {
        XmlSupport.write( xco.node(), target, charset, declaration );
    }

    /**
     *
     */
    @Override
    public String text() {
        return text(null, false);
    }

    /**
     *
     */
    public String text(boolean declaration) {
        return text(null, declaration);
    }

    /**
     *
     */
    public String text(Charset charset) {
        return text(charset, false);
    }

    /**
     *
     */
    public String text( Charset charset, boolean declaration )
    {
        StringWriter writer = new StringWriter();
        save(writer, charset, declaration);
        return writer.toString();
    }


    //
    // Transform zone
    //

    /** */
    public Xco transform( Source xslt )
    {
        Objects.requireNonNull( xslt, "'xslt' is null");

        return Xco.wrap( XmlSupport.transform( xco.node(), xslt ) );
    }

    /** */
    public Xco transform( Reader xslt )
    {
        Objects.requireNonNull(xslt, "'xslt' is null");

        return transform(new StreamSource(xslt));
    }

    /** */
    public Xco transform( InputStream xslt )
    {
        Objects.requireNonNull(xslt, "'xslt' is null");

        return transform(new StreamSource(xslt));
    }

    /** */
    public Xco transform( File xslt )
    {
        Objects.requireNonNull(xslt, "'xslt' is null");

        return transform(new StreamSource(xslt));
    }

    /** */
    public Xco transform( Path xslt )
    {
        Objects.requireNonNull(xslt, "'xslt' is null");

        return transform(new StreamSource(xslt.toFile()));
    }

    /** */
    public Xco transform( URL xslt )
    {
        Objects.requireNonNull(xslt, "'xslt' is null");

        try( InputStream stream = xslt.openStream() )
        {
            StreamSource source = new StreamSource(stream);
            source.setSystemId(xslt.toExternalForm());
            return transform(source);
        }
        catch( Throwable th ) {
            throw new XcoException( "[XML] Error on XSLT transform from URL: " + xslt, th );
        }
    }


    /** */
    public Xco copyWithoutNamespaces()
    {
        URL resource = XcoXml.class.getResource( STRIP_NAMESPACES_XSLT );

        if( resource == null )
            throw new XcoException( "[XML] Resource not found: " + STRIP_NAMESPACES_XSLT );

        return transform(resource);
    }

    //
    // XSD validate zone
    //

    /** */
    public void validate( Source xsd ) {
        XmlSupport.validate( xco.node(), xsd );
    }

//    /** */
//    validate(Reader)
//    validate(InputStream)
//    validate(File)
//    validate(Path)
//    validate(URL)
}