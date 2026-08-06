package ext.MA.coversheets;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

final class StreamingSoapResponse {
    private StreamingSoapResponse() { }
    static void invoke(OutputStream output, Map<String, Path> documents) throws IOException {
        write(output, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soapenv:Body><invokeResponse xmlns=\"http://adobe.com/idp/services\"><result xsi:type=\"ns1:AssemblerResult\" xmlns:ns1=\"http://adobe.com/idp/services\"><documents xsi:type=\"ns2:Map\" xmlns:ns2=\"http://xml.apache.org/xml-soap\">");
        for (Map.Entry<String, Path> entry : documents.entrySet()) { String name = xml(entry.getKey()); write(output, "<ns2:item><ns2:key xsi:type=\"xsd:string\">" + name + "</ns2:key><ns2:value xsi:type=\"ns1:BLOB\"><contentType>application/pdf</contentType><binaryData>"); base64(output, entry.getValue()); write(output, "</binaryData><attributes><item><key xsi:type=\"xsd:string\">basename</key><value xsi:type=\"xsd:string\">" + name + ".pdf</value></item><item><key xsi:type=\"xsd:string\">file</key><value xsi:type=\"xsd:string\">" + name + ".pdf</value></item></attributes></ns2:value></ns2:item>"); }
        write(output, "</documents><failedBlockNames/><numRequestedBlocks xsi:type=\"xsd:int\">" + documents.size() + "</numRequestedBlocks></result></invokeResponse></soapenv:Body></soapenv:Envelope>");
    }
    static void oneDocument(OutputStream output, Path pdf) throws IOException {
        write(output, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><invokeOneDocumentResponse xmlns=\"http://adobe.com/idp/services\"><result><contentType>application/pdf</contentType><binaryData>"); base64(output, pdf); write(output, "</binaryData></result></invokeOneDocumentResponse></soapenv:Body></soapenv:Envelope>");
    }
    private static void base64(OutputStream output, Path source) throws IOException { try (InputStream input = Files.newInputStream(source); OutputStream encoded = Base64.getEncoder().wrap(new NonClosingOutputStream(output))) { input.transferTo(encoded); } }
    private static void write(OutputStream output, String text) throws IOException { output.write(text.getBytes(StandardCharsets.UTF_8)); }
    private static String xml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("\u0027", "&apos;"); }
    private static final class NonClosingOutputStream extends FilterOutputStream { NonClosingOutputStream(OutputStream output) { super(output); } @Override public void close() throws IOException { flush(); } }
}
