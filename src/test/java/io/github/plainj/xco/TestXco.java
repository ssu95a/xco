package io.github.plainj.xco;

import org.junit.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.*;

public class TestXco {
    @Test
    public void shouldSelectSingleByXPath() {
        Xco x = XcoXml.parse("<user><name>Sergey</name></user>");

        Optional<Xco> name = x.single("name");

        assertTrue(name.isPresent());
        assertEquals("Sergey", name.get().get());
    }

    @Test
    public void shouldSelectAttributeByXPath() {
        Xco x = XcoXml.parse("<user id=\"100\"/>");

        Optional<Xco> id = x.single("@id");

        assertTrue(id.isPresent());
        assertEquals("100", id.get().get());
    }

    @Test
    public void shouldCountXPathNodes() {
        Xco x = XcoXml.parse("<roles><role>a</role><role>b</role></roles>");

        assertEquals(2, x.count("role"));
    }

    @Test
    public void shouldSelectManyByXPath() {
        Xco x = XcoXml.parse("<roles><role>a</role><role>b</role></roles>");

        int count = 0;
        for( Xco role : x.select("role") )
            count++;

        assertEquals(2, count);
    }

    @Test
    public void shouldMapTextNodeXPathToParentElement() {
        Xco x = XcoXml.parse("<user><name>Sergey</name></user>");

        Optional<Xco> name = x.single("name/text()");

        assertTrue(name.isPresent());
        assertEquals("name", name.get().name());
        assertEquals("Sergey", name.get().get());
    }

    @Test
    public void shouldNotCorruptRootWhenXPathSelectsCurrentNode() {

        Xco xco = XcoXml.parse("<root>value<item/></root>");

        assertEquals(0, xco.remove("."));

        assertEquals("root", xco.name());
        assertEquals("value", xco.get());
        assertEquals(1, xco.count("item"));
    }

    @Test
    public void shouldRemoveAttributeByXPath() {

        Xco xco = XcoXml.parse("<user id=\"100\" name=\"Sergey\"/>");

        assertEquals(1, xco.remove("@id"));

        assertFalse(xco.hasAttribute("id"));
        assertTrue(xco.hasAttribute("name"));
    }

    @Test
    public void shouldRemoveTextButKeepElement() {

        Xco xco = XcoXml.parse("<user><name>Sergey</name></user>");
        Xco name = xco.e("name");

        assertEquals(1, xco.remove("name/text()"));

        assertTrue(xco.hasElement("name"));
        assertNull(name.get());
    }

    @Test
    public void shouldRefreshValueAfterRemovingFirstTextNode() {

        Xco xco = XcoXml.parse(
                "<root><name>one<![CDATA[two]]></name></root>"
        );

        Xco name = xco.e("name");

        assertEquals("one", name.get());
        assertEquals(1, xco.remove("name/text()"));
        assertEquals("two", name.get());
    }

    @Test(expected = XcoPathException.class)
    public void shouldThrowSpecializedExceptionForInvalidXPath() {

        Xco xco = XcoXml.parse("<root/>");

        xco.remove("//*[");
    }

    // JSON //
    @Test
    public void shouldWriteScalarJson() {
        assertEquals( "{\"value\":100}", Xco.of("value").set(100).json().text() );
    }

    @Test
    public void shouldWriteAttributesAndChildren() {

        Xco xco = Xco.of("user");

        xco.a("id", 100);
        xco.e("name", "Sergey");

        assertEquals(
                "{\"user\":{\"@id\":\"100\",\"name\":\"Sergey\"}}",
                xco.json().text()
        );
    }

    @Test
    public void shouldWriteRepeatedChildrenAsArray() {

        Xco xco = Xco.of("roles");

        xco.append("role", "admin");
        xco.append("role", "user");

        assertEquals(
                "{\"roles\":{\"role\":[\"admin\",\"user\"]}}",
                xco.json().text()
        );
    }

    @Test
    public void shouldWriteValueAndAttributes() {

        Xco xco = Xco.of("message");

        xco.a("lang", "ru");
        xco.set("Привет");

        assertEquals(
                "{\"message\":{\"@lang\":\"ru\",\"#value\":\"Привет\"}}",
                xco.json().text()
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
                xco.json().text()
        );
    }

    @Test
    public void shouldReadScalarJson() {

        Xco xco = XcoJson.parse("{\"value\":100}");

        assertEquals("value", xco.name());
        assertEquals(100, xco.get());
    }

    @Test
    public void shouldReadAttributesAndChildren() {

        Xco xco = XcoJson.parse(
                "{\"user\":{\"@id\":\"100\",\"name\":\"Sergey\"}}"
        );

        assertEquals("100", xco.getIfPresent("@id"));
        assertEquals("Sergey", xco.getIfPresent("name"));
    }

    @Test
    public void shouldReadRepeatedChildrenFromArray() {

        Xco xco = XcoJson.parse(
                "{\"roles\":{\"role\":[\"admin\",\"user\"]}}"
        );

        assertEquals(2, xco.count("role"));
        assertEquals("admin", xco.single("role[1]").get().get());
        assertEquals("user", xco.single("role[2]").get().get());
    }

    @Test
    public void shouldReadValueWithAttributes() {

        Xco xco = XcoJson.parse(
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
                XcoJson.parse(json).json().text()
        );
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectMultipleRootFields() {
        XcoJson.parse("{\"a\":1,\"b\":2}");
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectRootArray() {
        XcoJson.parse("[1,2]");
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectNestedArrays() {
        XcoJson.parse("{\"root\":{\"item\":[[1,2]]}}");
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectUnknownReservedField() {
        XcoJson.parse("{\"root\":{\"#unknown\":1}}");
    }

    @Test
    public void shouldReadJsonWithExplicitRoot() {

        Xco xco = XcoJson.parse(
                "user",
                "{\"name\":\"Sergey\",\"active\":true}"
        );

        assertEquals("user", xco.name());
        assertEquals("Sergey", xco.getIfPresent("name"));
        assertEquals(true, xco.getIfPresent("active"));
    }

    @Test
    public void shouldReadScalarWithExplicitRoot() {

        Xco xco = XcoJson.parse("value", "100");

        assertEquals("value", xco.name());
        assertEquals(100, xco.get());
    }

    @Test(expected = XcoReadException.class)
    public void shouldRejectArrayWithExplicitRoot() {
        XcoJson.parse("root", "[1,2]");
    }

    @Test
    public void shouldConvertJsonToXml() {

        Xco xco = XcoJson.parse(
                "{\"user\":{\"name\":\"Sergey\"}}"
        );

        assertEquals(
                "<user><name>Sergey</name></user>",
                xco.xml().text()
        );
    }

    @Test
    public void shouldConvertXmlToJson() {

        Xco xco = XcoXml.parse(
                "<user><name>Sergey</name></user>"
        );

        assertEquals(
                "{\"user\":{\"name\":\"Sergey\"}}",
                xco.json().text()
        );
    }

    @Test
    public void shouldPrettyPrintJson() {

        Xco xco = Xco.of("user").e("name", "Sergey");

        String json = xco.json().text(true);

        assertTrue(json.contains("\n"));
    }

    @Test
    public void shouldTransformXmlWithXslt()
    {
        Xco xco = XcoXml.parse(
                "<user><name>Sergey</name></user>"
        );

        String xslt =
                "<?xml version=\"1.0\"?>" +
                        "<xsl:stylesheet version=\"1.0\" " +
                        "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
                        "  <xsl:template match=\"/\">" +
                        "    <person>" +
                        "      <fullName>" +
                        "        <xsl:value-of select=\"user/name\"/>" +
                        "      </fullName>" +
                        "    </person>" +
                        "  </xsl:template>" +
                        "</xsl:stylesheet>";

        Xco result = xco.xml().transform(
                new StringReader(xslt)
        );

        assertEquals(
                "<person><fullName>Sergey</fullName></person>",
                result.xml().text()
        );
    }

    @Test(expected = XcoException.class)
    public void shouldRejectInvalidXslt()
    {
        Xco.of("root")
                .xml()
                .transform(
                        new StringReader("<invalid>")
                );
    }

    @Test
    public void shouldTransformAttributes()
    {
        Xco xco = XcoXml.parse(
                "<user id=\"100\"/>"
        );

        // XSLT: @id -> <id>100</id>
    }

    @Test
    public void shouldCopyWithoutNamespaces()
    {
        Xco source = XcoXml.parse(
                "<ns:user xmlns:ns=\"urn:test\" ns:id=\"100\">" +
                        "<ns:name>Sergey</ns:name>" +
                        "</ns:user>"
        );

        Xco result = source.xml().copyWithoutNamespaces();

        assertEquals(
                "<user id=\"100\"><name>Sergey</name></user>",
                result.xml().text()
        );

        assertTrue(
                source.xml().text().contains("ns:user")
        );
    }

    @Test
    public void shouldCopyWithoutDefaultNamespaces()
    {
        Xco source = XcoXml.parse (
            "<user xmlns=\"urn:test\"><name>Sergey</name></user>"
        );

        Xco result = source.xml().copyWithoutNamespaces();

        assertEquals(
                "<user><name>Sergey</name></user>",
                result.xml().text()
        );
    }

    @Test
    public void shouldValidateXmlAgainstXsd()
    {
        String xml =
                "<user>" +
                        "  <name>Sergey</name>" +
                        "  <age>30</age>" +
                        "</user>";

        String xsd =
                "<?xml version=\"1.0\"?>" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
                        "  <xs:element name=\"user\">" +
                        "    <xs:complexType>" +
                        "      <xs:sequence>" +
                        "        <xs:element name=\"name\" type=\"xs:string\"/>" +
                        "        <xs:element name=\"age\" type=\"xs:int\"/>" +
                        "      </xs:sequence>" +
                        "    </xs:complexType>" +
                        "  </xs:element>" +
                        "</xs:schema>";

        Xco xco = XcoXml.parse(xml);

        xco.xml().validate(
                new StringReader(xsd)
        );
    }

    @Test(expected = XcoValidationException.class)
    public void shouldRejectXmlThatDoesNotMatchXsd()
    {
        String xml =
                "<user>" +
                        "  <name>Sergey</name>" +
                        "  <age>not-a-number</age>" +
                        "</user>";

        String xsd =
                "<?xml version=\"1.0\"?>" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" +
                        "  <xs:element name=\"user\">" +
                        "    <xs:complexType>" +
                        "      <xs:sequence>" +
                        "        <xs:element name=\"name\" type=\"xs:string\"/>" +
                        "        <xs:element name=\"age\" type=\"xs:int\"/>" +
                        "      </xs:sequence>" +
                        "    </xs:complexType>" +
                        "  </xs:element>" +
                        "</xs:schema>";

        Xco xco = XcoXml.parse(xml);

        xco.xml().validate(
                new StringReader(xsd)
        );
    }

    @Test
    public void shouldValidateNamespacedXmlAgainstXsd()
    {
        String xml =
                "<t:user xmlns:t=\"urn:test\">" +
                        "  <t:name>Sergey</t:name>" +
                        "</t:user>";

        String xsd =
                "<?xml version=\"1.0\"?>" +
                        "<xs:schema " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:t=\"urn:test\" " +
                        "targetNamespace=\"urn:test\" " +
                        "elementFormDefault=\"qualified\">" +

                        "  <xs:element name=\"user\">" +
                        "    <xs:complexType>" +
                        "      <xs:sequence>" +
                        "        <xs:element name=\"name\" type=\"xs:string\"/>" +
                        "      </xs:sequence>" +
                        "    </xs:complexType>" +
                        "  </xs:element>" +

                        "</xs:schema>";

        Xco xco = XcoXml.parse(xml);

        xco.xml().validate(
                new StringReader(xsd)
        );
    }
}
