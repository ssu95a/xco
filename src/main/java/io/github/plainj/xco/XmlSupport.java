package io.github.plainj.xco;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class XmlSupport {

    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    final private static String Log_Prfx = "[XML] ";

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;

    static {

        try {
            DOCUMENT_BUILDER_FACTORY = createDocumentBuilderFactory();
        }
        catch( ParserConfigurationException ex ) {
            throw new ExceptionInInitializerError("Failed to initialize secure XML parser: " + ex.getMessage());
        }
    }

    private XmlSupport() {
    }

    /** */
    static void write( Node node, Object target, Charset charset, boolean declaration )
    {
        Objects.requireNonNull( node,   "'node' is null" );
        Objects.requireNonNull( target, "'target' is null" );

        Charset effectiveCharset = charset == null ? DEFAULT_CHARSET : charset;

        if( target instanceof Writer ) {
            write(node, new StreamResult((Writer)target), effectiveCharset, declaration);
            return;
        }

        if( target instanceof OutputStream ) {
            write(node, new StreamResult((OutputStream)target), effectiveCharset, declaration);
            return;
        }

        if( target instanceof File ) {
            write(node, new StreamResult((File)target), effectiveCharset, declaration);
            return;
        }

        if( target instanceof Path ) {
            write(node, new StreamResult(((Path)target).toFile()), effectiveCharset, declaration);
            return;
        }

        throw new XcoException( Log_Prfx + "Unsupported XML target type: " + target.getClass().getName());
    }

    private static void write(Node node, Result result, Charset charset, boolean declaration )
    {

        try {

            Transformer transformer = transformerFactory().newTransformer();

            if( !declaration )
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            transformer.setOutputProperty(OutputKeys.ENCODING, charset.name());

            final Document ownerDocument = node instanceof Document ? (Document)node : node.getOwnerDocument();

            if( ownerDocument != null )
            {
                DocumentType docType = ownerDocument.getDoctype();
                if( docType != null )
                    transformer.setOutputProperty( OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId() );
            }

            transformer.transform( new DOMSource(node), result );
        }
        catch( Throwable th ) {
            throw new XcoException( Log_Prfx + "Error on write XML", th );
        }
    }

    private static TransformerFactory transformerFactory() {

        TransformerFactory factory = TransformerFactory.newInstance();

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        }
        catch( Exception ignored ) {
        }

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        }
        catch( Exception ignored ) {
        }

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        }
        catch( Exception ignored ) {
        }

        return factory;
    }

    static Element read(Object source, Charset charset) {

        Objects.requireNonNull(source, "'source' is null");

        Charset effectiveCharset = charset == null ? DEFAULT_CHARSET : charset;

        try {

            if( source instanceof CharSequence )
                return read(new InputSource(new StringReader(source.toString())));

            if( source instanceof Reader )
                return read(readerInputSource((Reader)source, effectiveCharset));

            if( source instanceof InputStream )
                return read(streamInputSource((InputStream)source, effectiveCharset));

            if( source instanceof File )
                return read((File)source, effectiveCharset);

            if( source instanceof Path )
                return read((Path)source, effectiveCharset);

            if( source instanceof URL )
                return read((URL)source, effectiveCharset);

            throw new XcoException("Unsupported XML source type: " + source.getClass().getName());
        }
        catch( XcoException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoException("Error on read XML from " + source, th);
        }
    }

    private static Element read(File file, Charset charset) {

        Objects.requireNonNull(file, "'file' is null");

        try( InputStream is = Files.newInputStream(file.toPath()) ) {
            return read(streamInputSource(is, charset));
        }
        catch( Throwable th ) {
            throw new XcoException("Error on read XML from file: " + file, th);
        }
    }

    private static Element read(Path path, Charset charset) {

        Objects.requireNonNull(path, "'path' is null");

        try( InputStream is = Files.newInputStream(path) ) {
            return read(streamInputSource(is, charset));
        }
        catch( Throwable th ) {
            throw new XcoException("Error on read XML from path: " + path, th);
        }
    }

    private static Element read(URL url, Charset charset) {

        Objects.requireNonNull(url, "'url' is null");

        try( InputStream is = url.openStream() ) {
            return read(streamInputSource(is, charset));
        }
        catch( Throwable th ) {
            throw new XcoException("Error on read XML from url: " + url, th);
        }
    }

    private static Element read(InputSource source) throws IOException, SAXException {

        org.w3c.dom.Document document = documentBuilder().parse(source);

        if( document == null || document.getDocumentElement() == null )
            throw new XcoException("Parsed XML document is empty");

        document.normalize();

        Element root = document.getDocumentElement();

        removeBlankTextNodes(root);

        return root;
    }

    private static InputSource readerInputSource(Reader reader, Charset charset) {

        InputSource source = new InputSource(reader);

        if( charset != null )
            source.setEncoding(charset.name());

        return source;
    }

    private static InputSource streamInputSource(InputStream stream, Charset charset) {

        InputSource source = new InputSource(stream);

        if( charset != null )
            source.setEncoding(charset.name());

        return source;
    }

    private static DocumentBuilder documentBuilder() {

        try {
            return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        }
        catch( Throwable th ) {
            throw new XcoException("Error on create DocumentBuilder", th);
        }
    }

    private static DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        }
        catch( IllegalArgumentException ignored ) {
        }

        factory.setNamespaceAware(false);
        factory.setValidating(false);
        factory.setIgnoringComments(true);

        return factory;
    }

    static void removeBlankTextNodes(Node node) {

        for( Node child = node.getFirstChild(); child != null; ) {

            Node next = child.getNextSibling();

            if( child.getNodeType() == Node.TEXT_NODE && S.isBlank(child.getNodeValue()) )
                node.removeChild(child);
            else if( child.getNodeType() == Node.ELEMENT_NODE )
                removeBlankTextNodes(child);

            child = next;
        }
    }

}