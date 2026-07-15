package io.github.plainj.xco.internal;

import io.github.plainj.xco.Xco;
import io.github.plainj.xco.XcoException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

final class XmlSupport {

    static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    final private static String Log_Prfx = "[XML] ";

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

    private static void write( Node node, Result result, Charset charset, boolean declaration )
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
}