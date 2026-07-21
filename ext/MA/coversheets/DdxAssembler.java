package ext.MA.coversheets;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public final class DdxAssembler {
    private DdxAssembler() { }

    public static Map<String, byte[]> assemble(String soap) {
        try {
            Element operation = firstOperation(parse(soap));
            Map<String, byte[]> inputs = inputs(operation);
            Document ddx = parse(new String(binary(direct(operation, "inDDXDoc")), StandardCharsets.UTF_8));
            Map<String, byte[]> results = new LinkedHashMap<>();
            for (Node node = ddx.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != Node.ELEMENT_NODE || !"PDF".equals(node.getLocalName())) continue;
                Element output = (Element) node;
                String name = output.getAttribute("result");
                if (name.isBlank()) continue;
                List<byte[]> sources = new ArrayList<>();
                for (Node child = output.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() != Node.ELEMENT_NODE || !"PDF".equals(child.getLocalName())) continue;
                    String sourceName = ((Element) child).getAttribute("source");
                    byte[] source = results.containsKey(sourceName) ? results.get(sourceName) : inputs.get(sourceName);
                    if (source == null) throw new IllegalArgumentException("DDX source is missing: " + sourceName);
                    sources.add(isPdf(source) ? source : htmlPdf(source));
                }
                if (sources.isEmpty()) throw new IllegalArgumentException("DDX result has no sources: " + name);
                results.put(name, PDFMerger.merge(sources));
            }
            if (results.isEmpty()) throw new IllegalArgumentException("DDX does not define PDF results.");
            return results;
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Unable to assemble invoke request.", e); }
    }

    private static Map<String, byte[]> inputs(Element operation) {
        Element inputs = direct(operation, "inputs");
        if (inputs == null) throw new IllegalArgumentException("SOAP request does not contain inputs.");
        Map<String, byte[]> values = new LinkedHashMap<>();
        for (Node node = inputs.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE || !"item".equals(node.getLocalName())) continue;
            Element item = (Element) node;
            Element key = direct(item, "key");
            Element value = direct(item, "value");
            if (key != null && value != null) values.put(key.getTextContent().trim(), binary(value));
        }
        return values;
    }

    private static byte[] htmlPdf(byte[] html) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new PdfRendererBuilder().withHtmlContent(new String(html, StandardCharsets.UTF_8), null).toStream(output).run();
        return output.toByteArray();
    }
    private static boolean isPdf(byte[] value) { return value.length >= 4 && value[0] == 37 && value[1] == 80 && value[2] == 68 && value[3] == 70; }
    private static byte[] binary(Element parent) {
        Element value = direct(parent, "binaryData");
        if (value == null) throw new IllegalArgumentException("BLOB does not contain binaryData.");
        return Base64.getDecoder().decode(value.getTextContent().replaceAll("\s+", ""));
    }
    private static Element firstOperation(Document document) {
        Element body = direct(document.getDocumentElement(), "Body");
        if (body == null) throw new IllegalArgumentException("SOAP request does not contain a Body element.");
        for (Node node = body.getFirstChild(); node != null; node = node.getNextSibling()) if (node.getNodeType() == Node.ELEMENT_NODE) return (Element) node;
        throw new IllegalArgumentException("SOAP Body does not contain an operation.");
    }
    private static Element direct(Element parent, String name) {
        if (parent == null) return null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getLocalName())) return (Element) node;
        return null;
    }
    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true); f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); f.setFeature("http://xml.org/sax/features/external-general-entities", false); f.setFeature("http://xml.org/sax/features/external-parameter-entities", false); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
}
