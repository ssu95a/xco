package io.github.plainj.xco;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.w3c.dom.Node.*;

/** */
final class DomSupport {

    private DomSupport( ) {
    }

    /** */
    static Iterator<Element> directElementIterator( final Element element )
    {
        return new Iterator<Element>() {

            private Node next = firstElement( element.getFirstChild() );

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Element next() {

                if( next == null )
                    throw new NoSuchElementException();

                Element result = (Element)next;
                next = firstElement( next.getNextSibling() );

                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("'remove' not supported");
            }
        };
    }

    /** */
    static Node firstElement( Node node )
    {
        while( node != null && node.getNodeType() != ELEMENT_NODE )
               node = node.getNextSibling();
        return node;
    }

    /** */
    static Node firstTextNode( Element element )
    {
        for( Node node = element.getFirstChild(); node != null; node = node.getNextSibling() )
        {
            short type = node.getNodeType();

            if( type == TEXT_NODE || type == CDATA_SECTION_NODE )
                return node;
        }

        return null;
    }

    /** */
    static String nodeValue( Node node ) {
        return node == null ? null : node.getNodeValue();
    }

    /** */
    static void clearTextNodes( Element element )
    {
        for( Node child = element.getFirstChild(); child != null; ) {

            Node next = child.getNextSibling();
            short type = child.getNodeType();

            if( type == TEXT_NODE || type == CDATA_SECTION_NODE )
                element.removeChild(child);

            child = next;
        }
    }

    /** */
    static Document newDocument()
    {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        }
        catch( ParserConfigurationException ex ) {
            throw new XcoException( "[XCO] Error on create DOM document", ex );
        }
    }
}