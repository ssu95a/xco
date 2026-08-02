package io.github.plainj.xco;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class TestXco {
    @Test
    public void shouldSelectSingleByXPath() {
        Xco x = Xco.parseXml("<user><name>Sergey</name></user>");

        Optional<Xco> name = x.single("name");

        assertTrue(name.isPresent());
        assertEquals("Sergey", name.get().get());
    }

    @Test
    public void shouldSelectAttributeByXPath() {
        Xco x = Xco.parseXml("<user id=\"100\"/>");

        Optional<Xco> id = x.single("@id");

        assertTrue(id.isPresent());
        assertEquals("100", id.get().get());
    }

    @Test
    public void shouldCountXPathNodes() {
        Xco x = Xco.parseXml("<roles><role>a</role><role>b</role></roles>");

        assertEquals(2, x.count("role"));
    }

    @Test
    public void shouldSelectManyByXPath() {
        Xco x = Xco.parseXml("<roles><role>a</role><role>b</role></roles>");

        int count = 0;
        for( Xco role : x.select("role") )
            count++;

        assertEquals(2, count);
    }

    @Test
    public void shouldMapTextNodeXPathToParentElement() {
        Xco x = Xco.parseXml("<user><name>Sergey</name></user>");

        Optional<Xco> name = x.single("name/text()");

        assertTrue(name.isPresent());
        assertEquals("name", name.get().name());
        assertEquals("Sergey", name.get().get());
    }

    @Test
    public void shouldNotCorruptRootWhenXPathSelectsCurrentNode() {

        Xco xco = Xco.parseXml("<root>value<item/></root>");

        assertEquals(0, xco.remove("."));

        assertEquals("root", xco.name());
        assertEquals("value", xco.get());
        assertEquals(1, xco.count("item"));
    }

    @Test
    public void shouldRemoveAttributeByXPath() {

        Xco xco = Xco.parseXml("<user id=\"100\" name=\"Sergey\"/>");

        assertEquals(1, xco.remove("@id"));

        assertFalse(xco.hasAttribute("id"));
        assertTrue(xco.hasAttribute("name"));
    }

    @Test
    public void shouldRemoveTextButKeepElement() {

        Xco xco = Xco.parseXml("<user><name>Sergey</name></user>");
        Xco name = xco.e("name");

        assertEquals(1, xco.remove("name/text()"));

        assertTrue(xco.hasElement("name"));
        assertNull(name.get());
    }

    @Test
    public void shouldRefreshValueAfterRemovingFirstTextNode() {

        Xco xco = Xco.parseXml(
                "<root><name>one<![CDATA[two]]></name></root>"
        );

        Xco name = xco.e("name");

        assertEquals("one", name.get());
        assertEquals(1, xco.remove("name/text()"));
        assertEquals("two", name.get());
    }

    @Test(expected = XcoPathException.class)
    public void shouldThrowSpecializedExceptionForInvalidXPath() {

        Xco xco = Xco.parseXml("<root/>");

        xco.remove("//*[");
    }
}
