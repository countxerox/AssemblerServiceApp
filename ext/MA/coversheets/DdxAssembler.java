package ext.MA.coversheets;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class DdxAssembler {
    private DdxAssembler() { }
    public static Map<String, Path> assemble(StreamingSoapRequest request) {
        try {
            if (request.ddx == null) throw new IllegalArgumentException("SOAP request does not contain inDDXDoc.");
            Document ddx = parse(request.ddx); Map<String, Path> results = new LinkedHashMap<>();
            for (Node node = ddx.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != Node.ELEMENT_NODE || !"PDF".equals(node.getLocalName())) continue;
                Element output = (Element) node; String name = output.getAttribute("result"); if (name.isBlank()) continue;
                List<Path> sources = new ArrayList<>();
                for (Node child = output.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() != Node.ELEMENT_NODE || !"PDF".equals(child.getLocalName())) continue;
                    String sourceName = ((Element) child).getAttribute("source");
                    Path source = results.containsKey(sourceName) ? results.get(sourceName) : request.input(sourceName);
                    Path assembledSource = isPdf(source) ? source : htmlPdf(source, request.directory);
                    for (Node operation = child.getFirstChild(); operation != null; operation = operation.getNextSibling()) {
                        if (operation.getNodeType() == Node.ELEMENT_NODE && "Watermark".equals(operation.getLocalName())) {
                            assembledSource = WatermarkApplier.apply(assembledSource, (Element) operation, request.directory);
                        }
                    }
                    sources.add(assembledSource);
                }
                if (sources.isEmpty()) throw new IllegalArgumentException("DDX result has no sources: " + name);
                Path result = Files.createTempFile(request.directory, "result-", ".pdf"); PDFMerger.merge(sources, result); results.put(name, result);
            }
            if (results.isEmpty()) throw new IllegalArgumentException("DDX does not define PDF results."); return results;
        } catch (IllegalArgumentException e) { throw e; } catch (Exception e) { throw new IllegalArgumentException("Unable to assemble invoke request.", e); }
    }
    private static Path htmlPdf(Path html, Path directory) throws Exception {
        org.jsoup.nodes.Document document = Jsoup.parse(Files.readString(html, StandardCharsets.UTF_8));
        document.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml).escapeMode(Entities.EscapeMode.xhtml).prettyPrint(false);
        Path pdf = Files.createTempFile(directory, "coversheet-", ".pdf");
        try (java.io.OutputStream output = Files.newOutputStream(pdf)) { new PdfRendererBuilder().withHtmlContent(document.html(), null).toStream(output).run(); }
        return pdf;
    }
    private static boolean isPdf(Path file) throws Exception { try (InputStream input = Files.newInputStream(file)) { return input.read() == 37 && input.read() == 80 && input.read() == 68 && input.read() == 70; } }
    private static Document parse(Path ddx) throws Exception { DocumentBuilderFactory f = DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true); f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); f.setFeature("http://xml.org/sax/features/external-general-entities", false); f.setFeature("http://xml.org/sax/features/external-parameter-entities", false); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); try (InputStream input = Files.newInputStream(ddx)) { return f.newDocumentBuilder().parse(input); } }
}
