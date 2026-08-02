package io.github.plainj.xco;

import org.junit.Test;

import java.math.BigDecimal;
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

    // JSON //
    @Test
    public void shouldWriteScalarJson() {
        assertEquals( "{\"value\":100}", Xco.of("value").set(100).toJson() );
    }

    @Test
    public void shouldWriteAttributesAndChildren() {

        Xco xco = Xco.of("user");

        xco.a("id", 100);
        xco.e("name", "Sergey");

        assertEquals(
                "{\"user\":{\"@id\":\"100\",\"name\":\"Sergey\"}}",
                xco.toJson()
        );
    }

    @Test
    public void shouldWriteRepeatedChildrenAsArray() {

        Xco xco = Xco.of("roles");

        xco.append("role", "admin");
        xco.append("role", "user");

        assertEquals(
                "{\"roles\":{\"role\":[\"admin\",\"user\"]}}",
                xco.toJson()
        );
    }

    @Test
    public void shouldWriteValueAndAttributes() {

        Xco xco = Xco.of("message");

        xco.a("lang", "ru");
        xco.set("Привет");

        assertEquals(
                "{\"message\":{\"@lang\":\"ru\",\"#value\":\"Привет\"}}",
                xco.toJson()
        );
    }

    @Test
    public void shouldPreserveRuntimeScalarTypes() {

        Xco xco = Xco.of("data");

        xco.e("enabled", true);
        xco.e("count", 10);
        xco.e("price", new BigDecimal("12.50"));

        assertEquals(
                "{\"data\":{\"enabled\":true,\"count\":10,\"price\":12.50}}",
                xco.toJson()
        );
    }

    @Test
    public void shouldReadScalarJson() {

        Xco xco = Xco.parseJson("{\"value\":100}");

        assertEquals("value", xco.name());
        assertEquals(100, xco.get());
    }

    @Test
    public void shouldReadAttributesAndChildren() {

        Xco xco = Xco.parseJson(
                "{\"user\":{\"@id\":\"100\",\"name\":\"Sergey\"}}"
        );

        assertEquals("100", xco.getIfPresent("@id"));
        assertEquals("Sergey", xco.getIfPresent("name"));
    }

    @Test
    public void shouldReadRepeatedChildrenFromArray() {

        Xco xco = Xco.parseJson(
                "{\"roles\":{\"role\":[\"admin\",\"user\"]}}"
        );

        assertEquals(2, xco.count("role"));
        assertEquals("admin", xco.single("role[1]").get().get());
        assertEquals("user", xco.single("role[2]").get().get());
    }

    @Test
    public void shouldReadValueWithAttributes() {

        Xco xco = Xco.parseJson(
                "{\"message\":{\"@lang\":\"ru\",\"#value\":\"Привет\"}}"
        );

        assertEquals("Привет", xco.get());
        assertEquals("ru", xco.getIfPresent("@lang"));
    }

    @Test
    public void shouldRoundTripJson() {

        String json =
                "{\"user\":{" +
                        "\"@id\":\"100\"," +
                        "\"name\":\"Sergey\"," +
                        "\"role\":[\"admin\",\"user\"]," +
                        "\"active\":true," +
                        "\"count\":10" +
                        "}}";

        assertEquals(
                json,
                Xco.parseJson(json).toJson()
        );
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectMultipleRootFields() {
        Xco.parseJson("{\"a\":1,\"b\":2}");
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectRootArray() {
        Xco.parseJson("[1,2]");
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectNestedArrays() {
        Xco.parseJson("{\"root\":{\"item\":[[1,2]]}}");
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectUnknownReservedField() {
        Xco.parseJson("{\"root\":{\"#unknown\":1}}");
    }
}
