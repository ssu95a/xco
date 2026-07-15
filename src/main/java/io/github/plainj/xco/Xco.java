package io.github.plainj.xco;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.w3c.dom.Node.*;

public final class Xco implements Iterable<Xco>, Supplier<Object>, Consumer<Object> {

    final private static String Log_Prfx = "[XML] ";

    private static final String USER_DATA_KEY = "plainj.xco";

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;

    static {

        try {
            DOCUMENT_BUILDER_FACTORY = createDocumentBuilderFactory();
        }
        catch( ParserConfigurationException ex ) {
            throw new ExceptionInInitializerError( "Failed to initialize secure XML parser: " + ex.getMessage() );
        }
    }

    private final Element element;
    private final Attr    attr;

    private volatile Object value;

    /** */
    private Xco( Element element ) {
        this.element = Objects.requireNonNull( element, "'element' is null" );
        this.attr    = null;
        this.element.setUserData( USER_DATA_KEY, this, null );
        this.value   = nodeValue( firstTextNode(element) );
    }

    /** */
    private Xco( Attr attr ) {
        this.element = null;
        this.attr    = Objects.requireNonNull( attr, "'attr' is null" );
    }

    /** */
    private boolean isAttribute( ) {
        return attr != null;
    }

    /** */
    public String name( ) {
        return isAttribute() ? attr.getName() : element.getNodeName();
    }

    /** */
    public Xco e( String name )
    {
        ensureElement();

        validateName( name, "'name' is empty" );

        Element child = firstDirectChild( name );

        if( child == null ) {
            child = element.getOwnerDocument().createElement(name);
            element.appendChild(child);
        }

        return wrap(child);
    }

    /** */
    public Xco e(String name, Object value)
    {
        Xco child = e(name);
        child.set(value);
        return child;
    }

    /** */
    public Xco append( String name )
    {
        ensureElement();

        validateName(name, "'name' is empty");

        Element child = element.getOwnerDocument().createElement(name);
        element.appendChild(child);

        return wrap(child);
    }

    /** */
    public Xco append( String name, Object value )
    {
        Xco child = append(name);
        child.set(value);
        return child;
    }

    /** */
    public Xco a( String name )
    {
        ensureElement();
        validateName(name, "'name' is empty");

        Attr attribute = element.getAttributeNode(name);

        if( attribute == null ) {
            attribute = element.getOwnerDocument().createAttribute(name);
            element.setAttributeNode(attribute);
        }

        return new Xco( attribute );
    }

    /** */
    public Xco a( String name, Object value )
    {
        Xco attribute = a(name);
        attribute.set(value);
        return attribute;
    }

    /** */
    public boolean hasElement( String name )
    {

        if( isAttribute() || isNullOrEmpty(name) )
            return false;

        return firstDirectChild(name) != null;
    }

    /** */
    public boolean hasAttribute( String name )
    {
        if( isAttribute() || isNullOrEmpty(name) )
            return false;

        return element.hasAttribute(name);
    }

    /** */
    public boolean hasElements( )
    {
        if( isAttribute() )
            return false;

        for( Node child = element.getFirstChild(); child != null; child = child.getNextSibling() )
        {
            if( child.getNodeType() == ELEMENT_NODE )
                return true;
        }

        return false;
    }

    /** */
    public boolean hasAttributes( ) {
        return !isAttribute() && element.hasAttributes();
    }

    @Override
    public Object get() {
        return isAttribute() ? attr.getValue() : value;
    }

    /** */
    public <T> T value() {
        return (T)get();
    }

    /** */
    public String stringValue() {

        Object v = get();
        return v == null ? null : String.valueOf(v);
    }

    public Xco set( Object value )
    {

        if( isAttribute() ) {
            attr.setValue( toStringValue(value) );
            return this;
        }

        clearTextNodes(element);

        String text = toStringValue(value);

        if( text != null )
            element.appendChild(element.getOwnerDocument().createTextNode(text));

        this.value = value;

        return this;
    }

    @Override
    public void accept(Object value) {
        set(value);
    }

    public Object getIfPresent(String name) {

        if( isNullOrEmpty(name) )
            return null;

        if( name.charAt(0) == '@' ) {

            ensureElement();

            Attr attribute = element.getAttributeNode(name.substring(1));

            return attribute == null ? null : attribute.getValue();
        }

        ensureElement();

        Element child = firstDirectChild(name);

        return child == null ? null : wrap(child).get();
    }

    /** */
    public boolean setIfPresent( String name, Object value )
    {
        if( isAttribute() || isNullOrEmpty(name) )
            return false;

        if( name.charAt(0) == '@' ) {

            Attr attribute = element.getAttributeNode( name.substring(1) );

            if( attribute == null )
                return false;

            attribute.setValue( toStringValue(value) );
            return true;
        }

        Element child = firstDirectChild(name);

        if( child == null )
            return false;

        wrap(child).set(value);

        return true;
    }

    /** */
    public Iterable<Xco> attributes( ) {

        if( isAttribute() )
            return Collections.emptyList();

        NamedNodeMap attrs = element.getAttributes();

        if( attrs == null || attrs.getLength() == 0 )
            return Collections.emptyList();

        List<Xco> result = new ArrayList<>( attrs.getLength() );

        for( int i = 0; i < attrs.getLength(); i++ )
             result.add( new Xco((Attr)attrs.item(i)) );

        return result;
    }

    /** */
    public Optional<Xco> single(String xpathQuery) {

        if( isAttribute() )
            return Optional.empty();

        if( isNullOrEmpty(xpathQuery) )
            return Optional.empty();

        try {

            Node node = (Node)newXPath().evaluate( xpathQuery, element, XPathConstants.NODE );

            if( node == null )
                return Optional.empty();

            switch( node.getNodeType() ) {
                case ELEMENT_NODE:
                    return Optional.of(wrap((Element)node));
                case ATTRIBUTE_NODE:
                    return Optional.of(new Xco((Attr)node));
                case TEXT_NODE:
                case CDATA_SECTION_NODE:
                    return node.getParentNode() instanceof Element
                            ? Optional.of(wrap((Element)node.getParentNode()))
                            : Optional.empty();

                default:
                    return Optional.empty();
            }
        }
        catch( Throwable th ) {
            throw new XcoException( Log_Prfx + "Error on execute XPath: " + xpathQuery, th );
        }
    }

    public Iterable<Xco> select(String xpathQuery) {

        ensureElement();

        if( isNullOrEmpty(xpathQuery) )
            return Collections.emptyList();

        try {
            NodeList nodes = (NodeList)newXPath().evaluate(xpathQuery, element, XPathConstants.NODESET);

            if( nodes == null || nodes.getLength() == 0 )
                return Collections.emptyList();

            List<Xco> result = new ArrayList<Xco>(nodes.getLength());

            for( int i = 0; i < nodes.getLength(); i++ ) {
                Node node = nodes.item(i);

                if( node instanceof Element )
                    result.add(wrap((Element)node));
                else if( node instanceof Attr )
                    result.add(new Xco((Attr)node));
            }

            return result;
        }
        catch( Throwable th ) {
            throw new XcoException("Error on execute XPath: " + xpathQuery, th);
        }
    }

    public <T> Iterable<T> select(String xpathQuery, Function<Xco, T> mapper) {

        Objects.requireNonNull(mapper, "'mapper' is null");

        List<T> result = new ArrayList<T>();

        for( Xco item : select(xpathQuery) )
            result.add(mapper.apply(item));

        return result;
    }

    public int count(String xpathQuery) {

        ensureElement();

        if( isNullOrEmpty(xpathQuery) )
            return 0;

        String query = xpathQuery.trim();

        if( !query.startsWith("count") )
            query = "count(" + query + ")";

        try {
            Double number = (Double)newXPath().evaluate(query, element, XPathConstants.NUMBER);
            return number.intValue();
        }
        catch( Throwable th ) {
            throw new XcoException("Error on execute XPath count: " + xpathQuery, th);
        }
    }

    public Xco remove() {

        if( isAttribute() ) {
            attr.getOwnerElement().removeAttributeNode(attr);
            return this;
        }

        Node parent = element.getParentNode();

        if( parent instanceof Element || parent instanceof Document ) {
            element.setUserData(USER_DATA_KEY, null, null);
            parent.removeChild(element);
            value = null;
            return this;
        }

        throw new XcoException( Log_Prfx + "Node '" + name() + "' cannot be removed");
    }

    /** */
    public Xco removeAll( )
    {
        if( isAttribute() )
            return this;

        while( element.hasChildNodes() )
               element.removeChild( element.getFirstChild() );

        removeAttributes();

        value = null;

        return this;
    }

    /** */
    public Xco removeAttributes() {

        if( isAttribute() )
            return this;

        NamedNodeMap attrs = element.getAttributes();

        List<Attr> list = new ArrayList<Attr>(attrs.getLength());

        for( int i = 0; i < attrs.getLength(); i++ )
            list.add((Attr)attrs.item(i));

        for( Attr attr : list )
             element.removeAttributeNode(attr);

        return this;
    }

    /** */
    public String toXml() {

        StringWriter writer = new StringWriter();
        writeXml(writer, null, false);
        return writer.toString();
    }

    /** */
    public void writeXml(Writer writer) {
        writeXml(writer, null, false);
    }

    public void writeXml(OutputStream stream) {
        writeXml(stream, null, false);
    }

    public void writeXml(Writer writer, Charset charset, boolean declaration) {

        Objects.requireNonNull(writer, "'writer' is null");

        writeXml(new StreamResult(writer), charset, declaration);
    }

    public void writeXml(OutputStream stream, Charset charset, boolean declaration) {

        Objects.requireNonNull(stream, "'stream' is null");

        writeXml(new StreamResult(stream), charset, declaration);
    }

    private void writeXml(StreamResult result, Charset charset, boolean declaration) {

        ensureElement();

        try {
            Transformer transformer = transformerFactory().newTransformer();

            if( !declaration )
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            if( charset != null )
                transformer.setOutputProperty(OutputKeys.ENCODING, charset.name());

            transformer.transform(new DOMSource(element), result);
        }
        catch( Throwable th ) {
            throw new XcoException("Error on write XML", th);
        }
    }

    @Override
    public Iterator<Xco> iterator() {

        if( isAttribute() )
            return Collections.emptyIterator();

        final Iterator<Element> iterator = directElementIterator(element);

        return new Iterator<Xco>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Xco next() {
                return wrap( iterator.next() );
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("'remove' not supported");
            }
        };
    }

    @Override
    public String toString() {
        return isAttribute()
                ? "XcoAttr{" + name() + "=" + get() + "}"
                : "Xco{" + name() + "=" + get() + "}";
    }

    private static Xco wrap( Element element )
    {
        Xco xco = (Xco)element.getUserData(USER_DATA_KEY);

        if( xco != null )
            return xco;

        return new Xco( element );
    }


    /** */
    private void ensureElement() {
        if( isAttribute() )
            throw new XcoException( Log_Prfx + "Attribute '" + name() + "' does not support element operation" );
    }

    private Element firstDirectChild( String name )
    {
        for( Node child = element.getFirstChild(); child != null; child = child.getNextSibling() ) {
            if( child.getNodeType() == ELEMENT_NODE && name.equals(child.getNodeName()) )
                return (Element)child;
        }

        return null;
    }

    private static Iterator<Element> directElementIterator( final Element element )
    {

        return new Iterator<Element>() {

            private Node next = firstElement(element.getFirstChild());

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Element next() {

                if( next == null )
                    throw new NoSuchElementException();

                Element result = (Element)next;
                next = firstElement(next.getNextSibling());

                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("'remove' not supported");
            }
        };
    }

    /** */
    private static Node firstElement(Node node) {

        while( node != null && node.getNodeType() != ELEMENT_NODE )
               node = node.getNextSibling();

        return node;
    }

    private static Node firstTextNode( Element element )
    {
        for( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
        {
            short type = node.getNodeType();

            if( type == TEXT_NODE || type == CDATA_SECTION_NODE )
                return node;
        }

        return null;
    }

    private static String nodeValue( Node node ) {
        return node == null ? null : node.getNodeValue();
    }

    private static void clearTextNodes(Element element) {

        for( Node child = element.getFirstChild(); child != null; ) {

            Node next = child.getNextSibling();
            short type = child.getNodeType();

            if( type == TEXT_NODE || type == CDATA_SECTION_NODE )
                element.removeChild(child);

            child = next;
        }
    }

    private static void removeBlankTextNodes(Node node) {

        for( Node child = node.getFirstChild(); child != null; ) {

            Node next = child.getNextSibling();

            if( child.getNodeType() == TEXT_NODE && isBlank(child.getNodeValue()) )
                node.removeChild(child);
            else if( child.getNodeType() == ELEMENT_NODE )
                removeBlankTextNodes(child);

            child = next;
        }
    }

    private static boolean isBlank( CharSequence value )
    {
        if( value == null )
            return true;

        for( int i = 0; i < value.length(); i++ )
        {
            if( !Character.isWhitespace(value.charAt(i)) )
                return false;
        }

        return true;
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static void validateName( String name, String message )
    {
        if( isNullOrEmpty(name) )
            throw new IllegalArgumentException(message);
    }

    private static String toStringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static XPath newXPath() {
        return XPathFactory.newInstance().newXPath();
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

    public static Xco of( String rootName )
    {
        validateName(rootName, "'rootName' is empty");

        Document document = documentBuilder().newDocument();
        Element root = document.createElement(rootName);
        document.appendChild(root);

        return wrap(root);
    }

    public static Xco parseXml(String xml) {

        Objects.requireNonNull(xml, "'xml' is null");

        return parseXml(new StringReader(xml));
    }

    public static Xco parseXml(Reader reader) {

        Objects.requireNonNull(reader, "'reader' is null");

        try {
            return parseXml(new InputSource(reader));
        }
        catch( Throwable th ) {
            throw new XcoException("Error on parse XML from Reader", th);
        }
    }

    public static Xco parseXml(InputStream stream) {

        Objects.requireNonNull(stream, "'stream' is null");

        try {
            return parseXml(new InputSource(stream));
        }
        catch( Throwable th ) {
            throw new XcoException("Error on parse XML from InputStream", th);
        }
    }

    public static Xco parseXml(File file) {

        Objects.requireNonNull(file, "'file' is null");

        try( InputStream is = new FileInputStream(file) ) {
            return parseXml(is);
        }
        catch( Throwable th ) {
            throw new XcoException("Error on parse XML from file: " + file, th);
        }
    }

    public static Xco parseXml(URL url) {

        Objects.requireNonNull(url, "'url' is null");

        try( InputStream is = url.openStream() ) {
            return parseXml(is);
        }
        catch( Throwable th ) {
            throw new XcoException("Error on parse XML from url: " + url, th);
        }
    }

    private static Xco parseXml(InputSource source) throws IOException, SAXException {

        Document document = documentBuilder().parse(source);

        if( document == null || document.getDocumentElement() == null )
            throw new XcoException("Parsed XML document is empty");

        document.normalize();
        removeBlankTextNodes(document.getDocumentElement());

        return wrap(document.getDocumentElement());
    }


}