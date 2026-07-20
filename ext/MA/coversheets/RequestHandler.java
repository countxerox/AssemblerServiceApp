package ext.MA.coversheets;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class RequestHandler implements HttpHandler {

    private static final Path RESPONSE_DIR = Path.of("responses");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().toString();

        if ("GET".equalsIgnoreCase(method) && uri.toLowerCase().contains("wsdl")) {
            sendFile(exchange, RESPONSE_DIR.resolve("wsdl.xml"));
            return;
        }

        String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String ddx = SoapUtils.extractAndDecodeFirstBinaryData(request);

        if (ddx.contains("coversheet1")
                && ddx.contains("coversheet2")
                && ddx.contains("coversheet3")
                && ddx.contains("target_document")) {

        	sendFile(exchange, RESPONSE_DIR.resolve("response-1.xml"));
        	


        } else {
        	// send a ready made test file
        	// sendFile(exchange, RESPONSE_DIR.resolve("response-2.xml"));
        	
        	
        	try {
                byte[] originalPdf = SoapUtils.extractBlobByKey(request, "inDoc");
                byte[] coversheetPdf = SoapUtils.extractBlobByKey(request, "outDoc");

                byte[] mergedPdf = PDFMerger.merge(coversheetPdf, originalPdf);

                String responseXml =
                        SoapResponseBuilder.buildInvokeOneDocumentPdfResponse(mergedPdf);

                sendXml(exchange, responseXml);

            } catch (Exception e) {
                e.printStackTrace();
                sendFault(exchange, "PDF merge failed: " + e.getMessage());
            }
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
    
    private static void sendXml(HttpExchange exchange, String xml) throws IOException {
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/xml;charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();

        System.out.println("Sent generated SOAP XML response");
    }

    private static void sendFault(HttpExchange exchange, String message) throws IOException {
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
                """.formatted(message);

        sendXml(exchange, fault);
    }
}
