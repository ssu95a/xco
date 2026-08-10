package io.github.plainj.xco;

import org.w3c.dom.*;


import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.w3c.dom.Node.*;

public final class Xco implements Iterable<Xco>, Supplier<Object>, Consumer<Object> {

    private static final String XCO_DATA_KEY = "plainj.xco";

    private final Element element;
    private final Attr    attr;

    private volatile Object value;

    /** */
    private Xco( Element element ) {
        this.element = Objects.requireNonNull( element, "'element' is null" );
        this.attr    = null;
        this.element.setUserData(XCO_DATA_KEY, this, null );
        this.value   = DomSupport.nodeValue( DomSupport.firstTextNode(element) );
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

        DomSupport.clearTextNodes(element);

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

        if( parent instanceof Element )
        {
            element.setUserData(XCO_DATA_KEY, null, null);
            parent.removeChild(element);
            value = null;
            return this;
        }

        throw new XcoException( "[XCO] Node '" + name() + "' cannot be removed");
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
    Node node() {
        ensureElement();
        return element;
    }

    @Override
    public Iterator<Xco> iterator() {

        if( isAttribute() )
            return Collections.emptyIterator();

        final Iterator<Element> iterator = DomSupport.directElementIterator(element);

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
        return isAttribute() ? "XcoAttr{" + name() + "=" + get() + "}" : "Xco{" + name() + "=" + get() + "}";
    }

    /* **/
    static Xco wrap( Element element )
    {
        Xco xco = (Xco)element.getUserData(XCO_DATA_KEY);

        if( xco != null )
            return xco;

        return new Xco( element );
    }


    /** */
    private void ensureElement() {
        if( isAttribute() )
            throw new XcoException( "[XCO] Attribute '" + name() + "' does not support element operation" );
    }

    /** */
    private Element firstDirectChild( String name )
    {
        for( Node child = element.getFirstChild(); child != null; child = child.getNextSibling() ) {
            if( child.getNodeType() == ELEMENT_NODE && name.equals(child.getNodeName()) )
                return (Element)child;
        }

        return null;
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

        Document document = DomSupport.newDocument();
        Element root = document.createElement(rootName);
        document.appendChild(root);

        return wrap(root);
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

    /** */
    public int remove( String xpathQuery )
    {
        if( isAttribute() || S.isNullOrEmpty(xpathQuery) )
            return 0;

        List<Node> nodes = XPathSupport.select( element, xpathQuery );

        int removed = 0;

        for( Node node : nodes ) {
            if( removeNode(node) )
                removed++;
        }

        return removed;
    }

    /** */
    private static boolean removeNode( Node node )
    {
        if( node == null )
            return false;

        switch( node.getNodeType() ) {
            case ATTRIBUTE_NODE:
                Attr attr = (Attr)node;
                Element owner = attr.getOwnerElement();

                if( owner == null )
                    return false;

                owner.removeAttributeNode(attr);
                return true;

            case ELEMENT_NODE:

                Node parent = node.getParentNode();
                if( !(parent instanceof Element) )
                    return false;

                Xco xco = (Xco)node.getUserData(XCO_DATA_KEY);

                if( xco != null ) {
                    xco.value = null;
                }

                node.setUserData(XCO_DATA_KEY, null, null);


                parent.removeChild(node);
                return true;

            case TEXT_NODE:
            case CDATA_SECTION_NODE:

                Node textParent = node.getParentNode();

                if( !(textParent instanceof Element) )
                    return false;

                textParent.removeChild(node);

                Xco parentXco = (Xco)textParent.getUserData(XCO_DATA_KEY);

                if( parentXco != null )
                    parentXco.value = DomSupport.nodeValue( DomSupport.firstTextNode( (Element)textParent) );

                return true;

            default:
                return false;
        }
    }

    /** */
    public XcoXml xml() {
        return new XcoXml(this);
    }

    /** */
    public XcoJson json() {
        return new XcoJson(this);
    }
}