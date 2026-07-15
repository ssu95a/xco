package io.github.plainj.xco;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** */
final class XPathSupport {

    final private static String Log_Prfx = "[XPath] ";

    private XPathSupport() {
    }

    /** */
    static Node single( Node context, String query )
    {
        Objects.requireNonNull(context, "'context' is null");

        if( S.isNullOrEmpty(query) )
            return null;

        try {
            return (Node)newXPath().evaluate(query, context, XPathConstants.NODE);
        }
        catch( Throwable th ) {
            throw new XcoException(Log_Prfx + "Error on execute XPath: " + query, th);
        }
    }

    /** */
    static List<Node> select( Node context, String query )
    {
        Objects.requireNonNull(context, "'context' is null");

        if( S.isNullOrEmpty(query) )
            return Collections.emptyList();

        try {

            NodeList nodes = (NodeList)newXPath().evaluate(query, context, XPathConstants.NODESET);

            if( nodes == null || nodes.getLength() == 0 )
                return Collections.emptyList();

            List<Node> result = new ArrayList<Node>(nodes.getLength());

            for( int i = 0; i < nodes.getLength(); i++ )
                result.add(nodes.item(i));

            return result;
        }
        catch( Throwable th ) {
            throw new XcoException(Log_Prfx + "Error on execute XPath: " + query, th);
        }
    }

    /** */
    static int count(Node context, String query) {

        Objects.requireNonNull(context, "'context' is null");

        if( S.isNullOrEmpty(query) )
            return 0;

        String countQuery = query.trim();

        if( !countQuery.startsWith("count(") )
            countQuery = "count(" + countQuery + ")";

        try {
            Double number = (Double)newXPath().evaluate(countQuery, context, XPathConstants.NUMBER);
            return number == null ? 0 : number.intValue();
        }
        catch( Throwable th ) {
            throw new XcoException(Log_Prfx + "Error on execute XPath count: " + query, th);
        }
    }

    /** */
    private static XPath newXPath() {
        return XPathFactory.newInstance().newXPath();
    }
}