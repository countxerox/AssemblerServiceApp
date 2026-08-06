package ext.MA.coversheets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class RequestHandler implements HttpHandler {
    private static final Path RESPONSE_DIR = Path.of("responses");
    private static final long MAX_REQUEST_SIZE_BYTES = 250L * 1024 * 1024;
    @Override public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod(); String query = exchange.getRequestURI().getQuery();
        if ("GET".equalsIgnoreCase(method) && query != null && query.toLowerCase(Locale.ROOT).contains("wsdl")) { sendFile(exchange, RESPONSE_DIR.resolve("wsdl.xml")); return; }
        if (!"POST".equalsIgnoreCase(method)) { sendFault(exchange, 405, "Only POST SOAP requests and GET ?WSDL are supported."); return; }
        try (InputStream input = limited(exchange); StreamingSoapRequest request = StreamingSoapRequest.parse(input)) {
            if ("invoke".equals(request.operation)) { Map<String, Path> documents = DdxAssembler.assemble(request); sendStream(exchange, output -> StreamingSoapResponse.invoke(output, documents)); }
            else if ("invokeOneDocument".equals(request.operation)) sendStream(exchange, output -> StreamingSoapResponse.oneDocument(output, request.input("inDoc")));
            else sendFault(exchange, 500, "Unsupported SOAP operation: " + request.operation);
        } catch (IllegalArgumentException e) { sendFault(exchange, 400, e.getMessage()); }
    }
    private static InputStream limited(HttpExchange exchange) throws IOException { String length = exchange.getRequestHeaders().getFirst("Content-Length"); if (length != null) try { if (Long.parseLong(length) > MAX_REQUEST_SIZE_BYTES) throw new IllegalArgumentException("SOAP request exceeds the 250 MiB limit."); } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid Content-Length header."); } return new LimitedInputStream(exchange.getRequestBody()); }
    private static final class LimitedInputStream extends java.io.FilterInputStream { long total; LimitedInputStream(InputStream input) { super(input); } @Override public int read(byte[] bytes, int offset, int length) throws IOException { int count = super.read(bytes, offset, length); if (count > 0 && (total += count) > MAX_REQUEST_SIZE_BYTES) throw new IllegalArgumentException("SOAP request exceeds the 250 MiB limit."); return count; } @Override public int read() throws IOException { int value = super.read(); if (value != -1 && ++total > MAX_REQUEST_SIZE_BYTES) throw new IllegalArgumentException("SOAP request exceeds the 250 MiB limit."); return value; } }
    private static void sendFile(HttpExchange exchange, Path file) throws IOException { byte[] bytes = Files.readAllBytes(file); exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=utf-8"); exchange.sendResponseHeaders(200, bytes.length); try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); } }
    private static void sendStream(HttpExchange exchange, StreamWriter writer) throws IOException { exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=utf-8"); exchange.sendResponseHeaders(200, 0); try (OutputStream output = exchange.getResponseBody()) { writer.write(output); } }
    private static void sendFault(HttpExchange exchange, int status, String message) throws IOException { String fault = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><soapenv:Fault><faultcode>soapenv:Server</faultcode><faultstring>" + escape(message) + "</faultstring></soapenv:Fault></soapenv:Body></soapenv:Envelope>"; byte[] bytes = fault.getBytes(StandardCharsets.UTF_8); exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=utf-8"); exchange.sendResponseHeaders(status, bytes.length); try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); } }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("\u0027", "&apos;"); }
    @FunctionalInterface private interface StreamWriter { void write(OutputStream output) throws IOException; }
}
