package io.github.plainj.xco;

import org.w3c.dom.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.w3c.dom.Node.*;

public final class Xco implements Iterable<Xco>, Supplier<Object>, Consumer<Object> {

    final private static String Log_Prfx = "[XCO] ";

    private static final String USER_DATA_KEY = "plainj.xco";


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
    public Xco e( String name, Object value)
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
        if( isAttribute() || S.isNullOrEmpty(name) )
            return false;

        return firstDirectChild(name) != null;
    }

    /** */
    public boolean hasAttribute( String name )
    {
        if( isAttribute() || S.isNullOrEmpty(name) )
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
            attr.setValue( S.toString(value) );
            return this;
        }

        clearTextNodes(element);

        String text = S.toString(value);

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

        if( S.isNullOrEmpty(name) )
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
        if( isAttribute() || S.isNullOrEmpty(name) )
            return false;

        if( name.charAt(0) == '@' ) {

            Attr attribute = element.getAttributeNode( name.substring(1) );

            if( attribute == null )
                return false;

            attribute.setValue( S.toString(value) );
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

        Node node = XPathSupport.single(element, xpathQuery);
        Xco xco = wrapNode(node);

        return xco == null ? Optional.<Xco>empty() : Optional.of(xco);
    }

    public Iterable<Xco> select(String xpathQuery) {

        ensureElement();

        List<Node> nodes = XPathSupport.select(element, xpathQuery);

        if( nodes.isEmpty() )
            return Collections.emptyList();

        List<Xco> result = new ArrayList<Xco>(nodes.size());

        for( Node node : nodes ) {
            Xco xco = wrapNode(node);

            if( xco != null )
                result.add(xco);
        }

        return result;
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

        return XPathSupport.count(element, xpathQuery);
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

    public void writeXml(Object target) {
        writeXml(target, null, false);
    }

    public void writeXml(Object target, Charset charset) {
        writeXml(target, charset, false);
    }

    public void writeXml(Object target, Charset charset, boolean declaration) {
        XmlSupport.write(node(), target, charset, declaration);
    }

    Node node() {
        ensureElement();
        return element;
    }

    public String toXml() {
        StringWriter writer = new StringWriter();
        writeXml(writer);
        return writer.toString();
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

    /** */
    private static void validateName( String name, String message )
    {
        if( S.isNullOrEmpty(name) )
            throw new IllegalArgumentException(message);
    }


    public static Xco of( String rootName )
    {
        validateName( rootName, "'rootName' is empty" );

        Document document = XmlSupport.newDocument();
        Element root = document.createElement(rootName);
        document.appendChild(root);

        return wrap(root);
    }

    public static Xco parseXml(Object source) {
        return parseXml(source, null);
    }

    public static Xco parseXml(Object source, Charset charset) {
        return wrap(XmlSupport.read(source, charset));
    }

    private static Xco wrapNode(Node node) {

        if( node == null )
            return null;

        switch( node.getNodeType() ) {
            case ELEMENT_NODE:
                return wrap((Element)node);
            case ATTRIBUTE_NODE:
                return new Xco((Attr)node);
            case TEXT_NODE:
            case CDATA_SECTION_NODE:
                return node.getParentNode() instanceof Element ? wrap((Element)node.getParentNode()) : null;
            default:
                return null;
        }
    }
}