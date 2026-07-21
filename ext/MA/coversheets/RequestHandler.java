package ext.MA.coversheets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class RequestHandler implements HttpHandler {

    private static final Path RESPONSE_DIR = Path.of("responses");
    private static final long MAX_REQUEST_SIZE_BYTES = 250L * 1024 * 1024;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String query = exchange.getRequestURI().getQuery();

        if ("GET".equalsIgnoreCase(method) && query != null
                && query.toLowerCase(Locale.ROOT).contains("wsdl")) {
            sendFile(exchange, RESPONSE_DIR.resolve("wsdl.xml"));
            return;
        }

        if (!"POST".equalsIgnoreCase(method)) {
            sendFault(exchange, 405, "Only POST SOAP requests and GET ?WSDL are supported.");
            return;
        }

        try {
            String request = new String(readRequestBody(exchange), StandardCharsets.UTF_8);
            String operation = SoapUtils.extractOperationName(request);
            switch (operation) {
                case "invoke" -> sendXml(exchange, 200,
                        InvokeResponseBuilder.build(DdxAssembler.assemble(request)));
                case "invokeOneDocument" -> {
                    byte[] inDoc = SoapUtils.extractBlobByElementName(request, "inDoc");
                    sendXml(exchange, 200, SoapResponseBuilder.buildInvokeOneDocumentPdfResponse(inDoc));
                }
                default -> sendFault(exchange, 500, "Unsupported SOAP operation: " + operation);
            }
        } catch (IllegalArgumentException e) {
            sendFault(exchange, 400, e.getMessage());
        }
    }

    private static byte[] readRequestBody(HttpExchange exchange) throws IOException {
        String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
        if (contentLength != null) {
            try {
                if (Long.parseLong(contentLength) > MAX_REQUEST_SIZE_BYTES) {
                    throw new IllegalArgumentException("SOAP request exceeds the 250 MiB limit.");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid Content-Length header.");
            }
        }

        try (InputStream input = exchange.getRequestBody();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                total += bytesRead;
                if (total > MAX_REQUEST_SIZE_BYTES) {
                    throw new IllegalArgumentException("SOAP request exceeds the 250 MiB limit.");
                }
                output.write(buffer, 0, bytesRead);
            }

            return output.toByteArray();
        }
    }

    private static void sendFile(HttpExchange exchange, Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();

        System.out.println("Sent response file: " + file.toAbsolutePath());
    }

    private static void sendXml(HttpExchange exchange, int statusCode, String xml) throws IOException {
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();

        System.out.println("Sent generated SOAP XML response");
    }

    private static void sendFault(HttpExchange exchange, int statusCode, String message) throws IOException {
        String fault = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <soapenv:Fault>
                      <faultcode>soapenv:Server</faultcode>
                      <faultstring>%s</faultstring>
                    </soapenv:Fault>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(escapeXml(message));

        sendXml(exchange, statusCode, fault);
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
