package io.github.plainj.xco;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

final class JsonSupport {

    private static final String ATTR_PREFIX = "@";
    private static final String VALUE_KEY   = "#value";

    private static final Map<String, Object> PRETTY_CONFIG = Collections.singletonMap( JsonGenerator.PRETTY_PRINTING, Boolean.TRUE );

    private JsonSupport() {
    }

    /** */
    static void write( Xco xco, Object target, boolean pretty )
    {
        Objects.requireNonNull(xco, "'xco' is null");
        Objects.requireNonNull(target, "'target' is null");

        if( target instanceof Writer ) {
            write(xco, (Writer)target, pretty);
            return;
        }

        if( target instanceof OutputStream ) {
            write(xco, (OutputStream)target, pretty);
            return;
        }

        if( target instanceof File) {
            write(xco, ((File)target).toPath(), pretty);
            return;
        }

        if( target instanceof Path) {
            write(xco, (Path)target, pretty);
            return;
        }

        throw new XcoWriteException(
                "[JSON] Unsupported JSON target type: "
                        + target.getClass().getName()
        );
    }

    /** */
    private static void write(
            Xco xco,
            Writer writer,
            boolean pretty
    )
    {
        try {
            JsonGenerator generator =
                    generatorFactory(pretty).createGenerator(writer);

            writeDocument(generator, xco);
            generator.flush();
        }
        catch( XcoWriteException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoWriteException(
                    "[JSON] Error on write JSON",
                    th
            );
        }
    }

    /** */
    private static void write(
            Xco xco,
            OutputStream stream,
            boolean pretty
    )
    {
        try {
            JsonGenerator generator =
                    generatorFactory(pretty).createGenerator(stream);

            writeDocument(generator, xco);
            generator.flush();
        }
        catch( XcoWriteException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoWriteException(
                    "[JSON] Error on write JSON",
                    th
            );
        }
    }

    /** */
    private static void write(
            Xco xco,
            Path path,
            boolean pretty
    )
    {
        try( OutputStream stream = Files.newOutputStream(path) ) {
            write(xco, stream, pretty);
        }
        catch( XcoWriteException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoWriteException(
                    "[JSON] Error on write JSON to path: " + path,
                    th
            );
        }
    }

    /** */
    private static void writeDocument(
            JsonGenerator generator,
            Xco xco
    )
    {
        generator.writeStartObject();
        writeNode(generator, xco.name(), xco);
        generator.writeEnd();
    }

    /** */
    private static void writeNode( JsonGenerator generator, String name, Xco node )
    {
        if( !node.hasAttributes() && !node.hasElements() ) {
            writeScalar(generator, name, node.get());
            return;
        }
        generator.writeStartObject(name);
        writeNodeContent(generator, node);
        generator.writeEnd();
    }

    private static void writeArrayItem(
            JsonGenerator generator,
            Xco node
    )
    {
        if( !node.hasAttributes() && !node.hasElements() ) {
            writeScalar(generator, node.get());
            return;
        }

        generator.writeStartObject();
        writeNodeContent(generator, node);
        generator.writeEnd();
    }


    private static void writeNodeContent(
            JsonGenerator generator,
            Xco node
    )
    {
        writeAttributes(generator, node);

        Object value = node.get();

        if( value != null )
            writeScalar(generator, VALUE_KEY, value);

        Map<String, List<Xco>> groups = groupChildren(node);

        for( Map.Entry<String, List<Xco>> entry : groups.entrySet() ) {

            String childName = entry.getKey();
            List<Xco> children = entry.getValue();

            if( children.size() == 1 ) {
                writeNode(generator, childName, children.get(0));
                continue;
            }

            generator.writeStartArray(childName);

            for( Xco child : children )
                writeArrayItem(generator, child);

            generator.writeEnd();
        }
    }

    /** */
    private static void writeAttributes( JsonGenerator generator, Xco node )
    {
        for( Xco attribute : node.attributes() )
             writeScalar( generator, ATTR_PREFIX + attribute.name(), attribute.get() );
    }

    /** */
    private static void writeScalar(
            JsonGenerator generator,
            String name,
            Object value
    )
    {
        if( value == null ) {
            generator.writeNull(name);
            return;
        }

        if( value instanceof Boolean ) {
            generator.write(name, (Boolean)value);
            return;
        }

        if( value instanceof BigDecimal ) {
            generator.write(name, (BigDecimal)value);
            return;
        }

        if( value instanceof BigInteger ) {
            generator.write(name, (BigInteger)value);
            return;
        }

        if( value instanceof Byte ) {
            generator.write(name, ((Byte)value).intValue());
            return;
        }

        if( value instanceof Short ) {
            generator.write(name, ((Short)value).intValue());
            return;
        }

        if( value instanceof Integer ) {
            generator.write(name, ((Integer)value).intValue());
            return;
        }

        if( value instanceof Long ) {
            generator.write(name, ((Long)value).longValue());
            return;
        }

        if( value instanceof Float || value instanceof Double ) {
            generator.write(
                    name,
                    new BigDecimal(value.toString())
            );
            return;
        }

        generator.write(name, value.toString());
    }

    /** */
    private static void writeScalar(
            JsonGenerator generator,
            Object value
    )
    {
        if( value == null ) {
            generator.writeNull();
            return;
        }

        if( value instanceof Boolean ) {
            generator.write((Boolean)value);
            return;
        }

        if( value instanceof BigDecimal ) {
            generator.write((BigDecimal)value);
            return;
        }

        if( value instanceof BigInteger ) {
            generator.write((BigInteger)value);
            return;
        }

        if( value instanceof Byte ) {
            generator.write(((Byte)value).intValue());
            return;
        }

        if( value instanceof Short ) {
            generator.write(((Short)value).intValue());
            return;
        }

        if( value instanceof Integer ) {
            generator.write(((Integer)value).intValue());
            return;
        }

        if( value instanceof Long ) {
            generator.write(((Long)value).longValue());
            return;
        }

        if( value instanceof Float || value instanceof Double ) {
            generator.write(
                    new BigDecimal(value.toString())
            );
            return;
        }

        generator.write(value.toString());
    }
    private static Map<String, List<Xco>> groupChildren(Xco node )
    {
        Map<String, List<Xco>> groups =
                new LinkedHashMap<String, List<Xco>>();

        for( Xco child : node ) {
            List<Xco> children = groups.get(child.name());

            if( children == null ) {
                children = new ArrayList<Xco>();
                groups.put(child.name(), children);
            }

            children.add(child);
        }

        return groups;
    }

    /** */
    private static JsonGeneratorFactory generatorFactory(boolean pretty )
    {
        return Json.createGeneratorFactory(
                pretty
                        ? PRETTY_CONFIG
                        : Collections.<String, Object>emptyMap()
        );
    }

    /** */
    static Xco read( Object source )
    {
        Objects.requireNonNull(source, "'source' is null");

        try {
            if( source instanceof CharSequence )
                return read(new StringReader(source.toString()));

            if( source instanceof Reader )
                return read((Reader)source);

            if( source instanceof InputStream )
                return read((InputStream)source);

            if( source instanceof byte[] )
                return read(new ByteArrayInputStream((byte[])source));

            if( source instanceof File )
                return read(((File)source).toPath());

            if( source instanceof Path )
                return read((Path)source);

            if( source instanceof URL )
                return read((URL)source);

            throw new XcoReadException(
                    "[JSON] Unsupported JSON source type: "
                            + source.getClass().getName()
            );
        }
        catch( XcoReadException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoReadException(
                    "[JSON] Error on read JSON",
                    th
            );
        }
    }

    /** */
    private static Xco read( Reader reader )
    {
        try {
            JsonReader jsonReader = Json.createReader(reader);
            return readDocument(jsonReader.readObject());
        }
        catch( XcoReadException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoReadException(
                    "[JSON] Error on read JSON from Reader",
                    th
            );
        }
    }

    /** */
    private static Xco read( InputStream stream )
    {
        try {
            JsonReader jsonReader = Json.createReader(stream);
            return readDocument(jsonReader.readObject());
        }
        catch( XcoReadException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoReadException(
                    "[JSON] Error on read JSON from InputStream",
                    th
            );
        }
    }

    /** */
    private static Xco read( Path path )
    {
        try( InputStream stream = Files.newInputStream(path) ) {
            return read(stream);
        }
        catch( XcoReadException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoReadException(
                    "[JSON] Error on read JSON from path: " + path,
                    th
            );
        }
    }

    /** */
    private static Xco read( URL url )
    {
        try( InputStream stream = url.openStream() ) {
            return read(stream);
        }
        catch( XcoReadException ex ) {
            throw ex;
        }
        catch( Throwable th ) { throw new XcoReadException( "[JSON] Error on read JSON from URL: " + url, th ); }
    }

    /** */
    private static Xco readDocument( JsonObject document )
    {
        if( document == null || document.size() != 1 ) {
            throw new XcoReadException(
                    "[JSON] JSON document must contain exactly one root field"
            );
        }

        Map.Entry<String, JsonValue> root =
                document.entrySet().iterator().next();

        String rootName = root.getKey();

        if( S.isNullOrEmpty(rootName) ) {
            throw new XcoReadException(
                    "[JSON] Root field name is empty"
            );
        }

        Xco xco = Xco.of(rootName);

        readValue(xco, root.getValue());

        return xco;
    }

    /** */
    private static void readValue( Xco xco, JsonValue value )
    {
        if( value == null || value == JsonValue.NULL ) {
            xco.set(null);
            return;
        }

        switch( value.getValueType() ) {
            case OBJECT:
                readObject(xco, value.asJsonObject());
                return;

            case ARRAY:
                throw new XcoReadException(
                        "[JSON] Array is not allowed here: " + xco.name()
                );

            case STRING:
                xco.set(((JsonString)value).getString());
                return;

            case NUMBER:
                xco.set(numberValue((JsonNumber)value));
                return;

            case TRUE:
                xco.set(Boolean.TRUE);
                return;

            case FALSE:
                xco.set(Boolean.FALSE);
                return;

            case NULL:
                xco.set(null);
                return;

            default:
                throw new XcoReadException(
                        "[JSON] Unsupported JSON value for: " + xco.name()
                );
        }
    }

    /** */
    private static void readObject( Xco xco, JsonObject object )
    {
        for( Map.Entry<String, JsonValue> entry : object.entrySet() )
        {
            final String    name  = entry.getKey();
            final JsonValue value = entry.getValue();

            if( S.isNullOrEmpty(name) )
                throw new XcoReadException( "[JSON] Object field name is empty" );

            /*
             * Attribute:
             *
             * "@id": "100"
             */
            if( name.startsWith(ATTR_PREFIX) )
            {
                String attributeName = name.substring( ATTR_PREFIX.length() );

                if( S.isNullOrEmpty(attributeName) )
                    throw new XcoReadException( "[JSON] Attribute name is empty" );

                if( value == null || value == JsonValue.NULL )
                    throw new XcoReadException( "[JSON] Attribute '" + attributeName + "' cannot be null" );


                switch( value.getValueType() ) {
                    case STRING:
                        xco.a( attributeName, ((JsonString)value).getString() );
                        break;
                    case NUMBER:
                        xco.a( attributeName, numberValue((JsonNumber)value) );
                        break;
                    case TRUE:
                        xco.a(attributeName, Boolean.TRUE);
                        break;
                    case FALSE:
                        xco.a(attributeName, Boolean.FALSE);
                        break;
                    default:
                        throw new XcoReadException( "[JSON] Attribute '" + attributeName + "' must be scalar" );
                }

                continue;
            }

            /*
             * Element's own value:
             *
             * "#value": "text"
             */
            if( VALUE_KEY.equals(name) )
            {
                if( value == null || value == JsonValue.NULL )
                {
                    xco.set(null);
                    continue;
                }

                switch( value.getValueType() )
                {
                    case STRING:
                        xco.set(((JsonString)value).getString());
                        break;
                    case NUMBER:
                        xco.set(numberValue((JsonNumber)value));
                        break;
                    case TRUE:
                        xco.set(Boolean.TRUE);
                        break;
                    case FALSE:
                        xco.set(Boolean.FALSE);
                        break;
                    default:
                        throw new XcoReadException( "[JSON] '" + VALUE_KEY + "' must be scalar" );
                }

                continue;
            }

            /*
             * Other #names are reserved.
             */
            if( name.charAt(0) == '#' )
                throw new XcoReadException( "[JSON] Unsupported reserved field: " + name );

            /*
             * Array becomes repeated child elements.
             */
            if( value != null && value.getValueType() == JsonValue.ValueType.ARRAY )
            {
                JsonArray array = (JsonArray)value;

                for( JsonValue item : array )
                {
                    if( item != null && item.getValueType() == JsonValue.ValueType.ARRAY )
                        throw new XcoReadException( "[JSON] Nested array is not supported: " + name );

                    Xco child = xco.append(name);
                    readValue(child, item);
                }

                continue;
            }

            /*
             * Ordinary scalar or object child.
             */
            Xco child = xco.e(name);
            readValue(child, value);
        }
    }

    /** */
    private static Number numberValue( JsonNumber number )
    {
        Objects.requireNonNull( number, "'number' is null" );

        if( !number.isIntegral() )
            return number.bigDecimalValue();

        BigInteger integer = number.bigIntegerValueExact();

        if( integer.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0
                && integer.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 ) {
            return integer.intValue();
        }

        if( integer.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0
                && integer.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 ) {
            return integer.longValue();
        }

        return integer;
    }
}