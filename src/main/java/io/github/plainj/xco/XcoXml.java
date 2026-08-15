package io.github.plainj.xco;

import org.xml.sax.InputSource;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
    public static Xco parse( Reader reader )
    {
        Objects.requireNonNull(reader, "'reader' is null");
        return Xco.wrap( XmlSupport.read( new InputSource(reader) ) );
    }

    /**
     *
     */
    public static Xco parse( String xml )
    {
        Objects.requireNonNull(xml, "'xml' is null");
        return parse( new StringReader(xml) );
    }

    /**
     *
     */
    public static Xco parse( Path path )
    {
        Objects.requireNonNull(path, "'path' is null");

        try( InputStream stream = Files.newInputStream(path) ) {
             return parse(stream);
        }
        catch( Throwable th ) {
            throw new XcoReadException( "[XML] Error on read XML from path: " + path, th);
        }
    }

    /** */
    public static Xco parse( InputStream stream )
    {
        return parse(stream, null);
    }

    /** */
    public static Xco parse( InputStream stream, Charset charset )
    {
        Objects.requireNonNull(stream, "'stream' is null");

        InputSource source = new InputSource(stream);

        if( charset != null )
            source.setEncoding(charset.name());

        return Xco.wrap( XmlSupport.read(source) );
    }

    /** */
    public static Xco parse( File file )
    {
        Objects.requireNonNull(file, "'file' is null");

        try( InputStream stream = Files.newInputStream(file.toPath()) ) {
            return parse(stream);
        }
        catch( Throwable th ) {
            throw new XcoReadException( "[XML] Error on read XML from file: " + file, th );
        }
    }

    /** */
    public static Xco parse( URL url )
    {
        Objects.requireNonNull(url, "'url' is null");

        try( InputStream stream = url.openStream() ) {
            return parse(stream);
        }
        catch( Throwable th ) {
            throw new XcoReadException( "[XML] Error on read XML from URL: " + url, th );
        }
    }

    //
    // Save zone
    //


    @Override
    public void save( Object target )
    {
        save(target, null, false);
    }

    /** */
    public void save( Object target, Charset charset )
    {
        save(target, charset, false);
    }

    /** */
    public void save( Object target, Charset charset, boolean declaration )
    {
        Objects.requireNonNull(target, "'target' is null");

        Charset effectiveCharset = charset == null ? XmlSupport.DEFAULT_CHARSET : charset;

        if( target instanceof Writer )
        {
            save( new StreamResult((Writer)target), effectiveCharset, declaration );
            return;
        }

        if( target instanceof OutputStream ) {
            save( new StreamResult((OutputStream)target), effectiveCharset, declaration );
            return;
        }

        if( target instanceof File ) {
            save( new StreamResult((File)target), effectiveCharset, declaration );
            return;
        }

        if( target instanceof Path ) {
            save( new StreamResult(((Path)target).toFile()), effectiveCharset, declaration );
            return;
        }

        throw new XcoWriteException( "[XML] Unsupported XML target type: " + target.getClass().getName() );
    }

    /** */
    private void save( Result result, Charset charset, boolean declaration )
    {
        XmlSupport.write( xco.node(), result, charset, declaration );
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
        Objects.requireNonNull(xsd, "'xsd' is null");
        XmlSupport.validate( xco.node(), xsd );
    }

    /** */
    public void validate( Reader xsd )
    {
        Objects.requireNonNull(xsd, "'xsd' is null");

        validate( new StreamSource(xsd) );
    }


    /** */
    public void validate( InputStream xsd )
    {
        Objects.requireNonNull(xsd, "'xsd' is null");

        validate( new StreamSource(xsd) );
    }


    /** */
    public void validate( File xsd )
    {
        Objects.requireNonNull(xsd, "'xsd' is null");

        validate( new StreamSource(xsd) );
    }


    /** */
    public void validate( Path xsd )
    {
        Objects.requireNonNull(xsd, "'xsd' is null");

        validate( new StreamSource(xsd.toFile()) );
    }


    /** */
    public void validate( URL url )
    {
        Objects.requireNonNull( url, "'url' is null");

        try( InputStream stream = url.openStream() )
        {
            StreamSource source = new StreamSource(stream);
            source.setSystemId(url.toExternalForm());
            validate(source);
        }
        catch( Throwable th ) {
            throw new XcoException( "[XML] XSD validation failed from URL: " + url, th );
        }
    }

}