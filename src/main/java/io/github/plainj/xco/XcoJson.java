package io.github.plainj.xco;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** */
public final class XcoJson implements XcoFormat {

    private final Xco xco;

    /**
     *
     */
    XcoJson(Xco xco) {
        this.xco = Objects.requireNonNull(xco, "'xco' is null");
    }


    /**
     *
     */
    private static JsonValue readJsonValue(Reader reader) {
        Objects.requireNonNull(reader, "'reader' is null");

        try {
            JsonReader jsonReader = Json.createReader(reader);
            return jsonReader.readValue();
        } catch (Throwable th) {
            throw new XcoReadException("[JSON] Error on read JSON from Reader", th);
        }
    }


    /**
     *
     */
    private static JsonValue readJsonValue(InputStream stream) {
        Objects.requireNonNull(stream, "'stream' is null");
        try {
            JsonReader jsonReader = Json.createReader(stream);
            return jsonReader.readValue();
        } catch (Throwable th) {
            throw new XcoReadException("[JSON] Error on read JSON from InputStream", th);
        }
    }

    private static Xco parse( String rootName, JsonValue value )
    {
        if( S.isNullOrEmpty(rootName) )
            return JsonSupport.read(value);
        return JsonSupport.read(rootName, value);
    }


    /** */
    public static Xco parse( String rootName, Reader reader )
    {
        return parse( rootName, readJsonValue(reader) );
    }
    public static Xco parse( Reader reader )
    {
        return parse( null, reader );
    }


    /** */
    public static Xco parse( String json ) {
        return parse(null, json);
    }
    /** */
    public static Xco parse( String rootName, String json ) {
        Objects.requireNonNull(json, "'json' is null");
        return parse(rootName, new StringReader(json));
    }


    /** */
    public static Xco parse(InputStream stream) {
        return parse(null, stream);
    }
    /** */
    public static Xco parse(String rootName, InputStream is) {
        return parse( rootName, readJsonValue(is) );
    }


    /** */
    public static Xco parse( byte[] bytes ) {
        return parse(null,bytes);
    }
    /** */
    public static Xco parse( String rootName, byte[] bytes ) {
        Objects.requireNonNull(bytes, "'bytes' is null");
        return parse( rootName, new ByteArrayInputStream(bytes) );
    }


    /** */
    public static Xco parse( File file ) {
        return parse( null, file );
    }
    /** */
    public static Xco parse( String rootName, File file )
    {
        Objects.requireNonNull( file, "'file' is null");

        try( InputStream stream = Files.newInputStream(file.toPath()) ) {
            return parse(rootName, stream);
        }
        catch( Throwable th ) {
            throw new XcoReadException( "[JSON] Error on read JSON from file: " + file, th );
        }
    }


    /** */
    public static Xco parse( Path path ) {
        return parse( null, path );
    }
    /** */
    public static Xco parse( String rootName, Path path )
    {
        Objects.requireNonNull( path, "'path' is null" );

        try( InputStream stream = Files.newInputStream(path) ) {
            return parse(rootName, stream);
        }
        catch( Throwable th ) {
            throw new XcoReadException( "[JSON] Error on read JSON from path: " + path, th );
        }
    }


    /** */
    public static Xco parse( URL url ) {
        return parse( null, url );
    }
    /** */
    public static Xco parse( String rootName, URL url )
    {
        Objects.requireNonNull(url, "'url' is null");

        try( InputStream stream = url.openStream() ) {
            return parse(rootName, stream);
        }
        catch( Throwable th ) {
            throw new XcoReadException( "[JSON] Error on read JSON from URL: " + url, th );
        }
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