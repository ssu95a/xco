package io.github.plainj.xco;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Objects;

final class XmlSupport {

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;

    static {

        try {
            DOCUMENT_BUILDER_FACTORY = createDocumentBuilderFactory();
        }
        catch( ParserConfigurationException ex ) {
            throw new ExceptionInInitializerError( "Failed to initialize secure XML parser: " + ex.getMessage() );
        }
    }

    /** */
    private XmlSupport() {
    }

    /** */
    private static DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
        factory.setFeature( "http://xml.org/sax/features/external-general-entities", false );
        factory.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );
        factory.setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
        factory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        }
        catch( IllegalArgumentException ignored ) {
        }

        factory.setNamespaceAware( true );
        factory.setValidating    ( false);
        factory.setIgnoringComments(true);

        return factory;
    }


    /** */
    static void write( Node node, Result result, Charset charset, boolean declaration )
    {
        Objects.requireNonNull( node, "'node' is null");
        Objects.requireNonNull( result, "'result' is null");
        Objects.requireNonNull( charset, "'charset' is null");

        try {

            Transformer transformer = transformerFactory().newTransformer();

            if( !declaration )
                transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );

            transformer.setOutputProperty( OutputKeys.ENCODING, charset.name() );

            Document ownerDocument = node instanceof Document ? (Document)node : node.getOwnerDocument();

            if( ownerDocument != null )
            {
                DocumentType docType = ownerDocument.getDoctype();

                if( docType != null )
                    transformer.setOutputProperty( OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId() );
            }

            transformer.transform( new DOMSource(node), result );
        }
        catch( Throwable th ) {
            throw new XcoWriteException( "[XML] Error on write XML", th );
        }
    }

    /** */
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

        factory.setErrorListener(new ErrorListener() {

            @Override
            public void warning( TransformerException ex ) throws TransformerException {
                // ignore
            }

            @Override
            public void error( TransformerException ex ) throws TransformerException
            {
                throw ex;
            }

            @Override
            public void fatalError( TransformerException ex ) throws TransformerException
            {
                throw ex;
            }
        });

        return factory;
    }


    /** */
    static Element read( InputSource source )
    {
        Objects.requireNonNull(source, "'source' is null");

        try {

            Document document = documentBuilder().parse(source);

            if( document == null || document.getDocumentElement() == null )
                throw new XcoReadException( "[XML] Parsed XML document is empty" );

            document.normalize();

            Element root = document.getDocumentElement();

            removeBlankTextNodes(root);

            return root;
        }
        catch( XcoReadException ex ) {
            throw ex;
        }
        catch( Throwable th ) {
            throw new XcoReadException("[XML] Error on parse XML", th );
        }
    }


    /** */
    private static DocumentBuilder documentBuilder() {

        try {
            return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        }
        catch( Throwable th ) {
            throw new XcoException("[XML] Error on create DocumentBuilder", th);
        }
    }


    /** */
    static void removeBlankTextNodes( Node node ) {

        for( Node child = node.getFirstChild(); child != null; ) {

            Node next = child.getNextSibling();

            if( child.getNodeType() == Node.TEXT_NODE && S.isBlank(child.getNodeValue()) )
                node.removeChild(child);
            else if( child.getNodeType() == Node.ELEMENT_NODE )
                removeBlankTextNodes(child);

            child = next;
        }
    }

    /** */
    static Element transform( Node node, Source xslt )
    {
        Objects.requireNonNull( node, "'node' is null" );
        Objects.requireNonNull( xslt, "'xslt' is null" );

        try {

            Transformer transformer = transformerFactory().newTransformer(xslt);

            DOMResult result = new DOMResult();

            transformer.transform( new DOMSource(node), result );

            Node resultNode = result.getNode();

            if( resultNode instanceof Document )
            {
                Element root = ((Document)resultNode).getDocumentElement();
                if( root != null )
                    return root;
            }

            if( resultNode instanceof Element )
                return (Element)resultNode;

            throw new XcoException( "[XML] XSLT result has no root element" );
        }
        catch( Throwable th ) {
            throw new XcoException( "[XML] Error on XSLT transform", th );
        }
    }

    /** */
    static void validate( Node node, Source xsd )
    {
        try {

            SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Schema schema = factory.newSchema(xsd);

            Validator validator = schema.newValidator();
            validator.validate( new DOMSource(node) );
        }
        catch (Throwable th) {
            throw new XcoValidationException( "[XML] XSD validation failed", th );
        }
    }

}