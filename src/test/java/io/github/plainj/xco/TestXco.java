package io.github.plainj.xco;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}
