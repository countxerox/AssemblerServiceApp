package ext.MA.coversheets;

import java.io.StringReader;
import java.util.Base64;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class SoapUtils {

    public static String extractOperationName(String soap) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(soap)));
            Element body = findSoapBody(document.getDocumentElement());

            if (body == null) {
                throw new IllegalArgumentException("SOAP request does not contain a Body element.");
            }

            for (Node node = body.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    return node.getLocalName();
                }
            }

            throw new IllegalArgumentException("SOAP Body does not contain an operation.");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SOAP XML.", e);
        }
    }

    public static byte[] extractBlobByElementName(String soap, String elementName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(soap)));
            Element body = findSoapBody(document.getDocumentElement());
            if (body == null) {
                throw new IllegalArgumentException("SOAP request does not contain a Body element.");
            }

            for (Node operation = body.getFirstChild(); operation != null; operation = operation.getNextSibling()) {
                if (operation.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                for (Node child = operation.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() != Node.ELEMENT_NODE || !elementName.equals(child.getLocalName())) {
                        continue;
                    }
                    for (Node value = child.getFirstChild(); value != null; value = value.getNextSibling()) {
                        if (value.getNodeType() == Node.ELEMENT_NODE && "binaryData".equals(value.getLocalName())) {
                            return Base64.getDecoder().decode(value.getTextContent().replaceAll("\\s+", ""));
                        }
                    }
                }
            }

            throw new IllegalArgumentException("SOAP request does not contain a binaryData value for " + elementName + ".");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SOAP XML or Base64 data.", e);
        }
    }

    private static Element findSoapBody(Element envelope) {
        for (Node node = envelope.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE || !"Body".equals(node.getLocalName())) {
                continue;
            }

            String namespace = node.getNamespaceURI();
            if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespace)
                    || "http://www.w3.org/2003/05/soap-envelope".equals(namespace)) {
                return (Element) node;
            }
        }
        return null;
    }
}
