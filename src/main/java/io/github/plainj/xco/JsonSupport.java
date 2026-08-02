package io.github.plainj.xco;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

final class JsonSupport {

    private static final String ATTR_PREFIX = "@";
    private static final String VALUE_KEY   = "#value";

    private static final Map<String, Object> PRETTY_CONFIG =
            Collections.<String, Object>singletonMap(
                    JsonGenerator.PRETTY_PRINTING,
                    Boolean.TRUE
            );

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
}