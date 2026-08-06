package ext.MA.coversheets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

final class StreamingSoapRequest implements AutoCloseable {
    final String operation;
    final Path directory;
    final Path ddx;
    final Map<String, Path> inputs;
    private StreamingSoapRequest(String operation, Path directory, Path ddx, Map<String, Path> inputs) { this.operation = operation; this.directory = directory; this.ddx = ddx; this.inputs = inputs; }

    static StreamingSoapRequest parse(InputStream input) {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("assembler-service-");
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            XMLStreamReader reader = factory.createXMLStreamReader(input);
            String operation = null; Path ddx = null; Map<String, Path> inputs = new LinkedHashMap<>();
            while (reader.hasNext()) {
                if (reader.next() != XMLStreamConstants.START_ELEMENT) continue;
                String name = reader.getLocalName();
                if ("Body".equals(name)) { operation = nextStartName(reader); }
                else if ("inDDXDoc".equals(name) || "ddx".equals(name)) ddx = readContainer(reader, directory);
                else if ("inDoc".equals(name)) inputs.put("inDoc", readContainer(reader, directory));
                else if ("inputs".equals(name)) readInputs(reader, directory, inputs);
            }
            reader.close();
            if (operation == null) throw new IllegalArgumentException("SOAP Body does not contain an operation.");
            return new StreamingSoapRequest(operation, directory, ddx, inputs);
        } catch (Exception e) {
            delete(directory);
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            throw new IllegalArgumentException("Invalid SOAP XML.", e);
        }
    }

    Path input(String name) { Path path = inputs.get(name); if (path == null) throw new IllegalArgumentException("SOAP request does not contain input: " + name); return path; }
    private static String nextStartName(XMLStreamReader reader) throws Exception { while (reader.hasNext()) if (reader.next() == XMLStreamConstants.START_ELEMENT) return reader.getLocalName(); throw new IllegalArgumentException("SOAP Body does not contain an operation."); }
    private static void readInputs(XMLStreamReader reader, Path dir, Map<String, Path> inputs) throws Exception { while (reader.hasNext()) { int event = reader.next(); if (event == XMLStreamConstants.START_ELEMENT && "item".equals(reader.getLocalName())) readItem(reader, dir, inputs); else if (event == XMLStreamConstants.END_ELEMENT && "inputs".equals(reader.getLocalName())) return; } }
    private static void readItem(XMLStreamReader reader, Path dir, Map<String, Path> inputs) throws Exception { String key = null; Path blob = null; while (reader.hasNext()) { int event = reader.next(); if (event == XMLStreamConstants.START_ELEMENT && "key".equals(reader.getLocalName())) key = reader.getElementText().trim(); else if (event == XMLStreamConstants.START_ELEMENT && "binaryData".equals(reader.getLocalName())) blob = decode(reader, dir); else if (event == XMLStreamConstants.END_ELEMENT && "item".equals(reader.getLocalName())) { if (key != null && blob != null) inputs.put(key, blob); return; } } }
    private static Path readContainer(XMLStreamReader reader, Path dir) throws Exception { while (reader.hasNext()) { int event = reader.next(); if (event == XMLStreamConstants.START_ELEMENT && "binaryData".equals(reader.getLocalName())) return decode(reader, dir); if (event == XMLStreamConstants.END_ELEMENT) break; } throw new IllegalArgumentException("BLOB does not contain binaryData."); }
    private static Path decode(XMLStreamReader reader, Path dir) throws Exception { Path target = Files.createTempFile(dir, "blob-", ".bin"); try (OutputStream output = Files.newOutputStream(target)) { StringBuilder carry = new StringBuilder(4); while (reader.hasNext()) { int event = reader.next(); if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA || event == XMLStreamConstants.SPACE) { String text = reader.getText(); StringBuilder clean = new StringBuilder(carry); for (int i = 0; i < text.length(); i++) if (!Character.isWhitespace(text.charAt(i))) clean.append(text.charAt(i)); int length = clean.length() - clean.length() % 4; if (length > 0) output.write(Base64.getDecoder().decode(clean.substring(0, length))); carry.setLength(0); carry.append(clean.substring(length)); } else if (event == XMLStreamConstants.END_ELEMENT && "binaryData".equals(reader.getLocalName())) { if (carry.length() != 0) output.write(Base64.getDecoder().decode(carry.toString())); return target; } } } throw new IllegalArgumentException("Unterminated binaryData."); }
    @Override public void close() { delete(directory); }
    private static void delete(Path directory) { if (directory == null) return; try (java.util.stream.Stream<Path> paths = Files.walk(directory)) { paths.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } }); } catch (IOException ignored) { } }
}
