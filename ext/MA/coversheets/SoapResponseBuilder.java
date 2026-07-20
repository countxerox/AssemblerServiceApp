package ext.MA.coversheets;

import java.util.Base64;

public class SoapResponseBuilder {

    public static String buildInvokeOneDocumentPdfResponse(byte[] pdfBytes) {
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <soapenv:Body>
                    <invokeOneDocumentResponse xmlns="http://adobe.com/idp/services">
                      <result>
                        <contentType>application/pdf</contentType>
                        <binaryData>%s</binaryData>
                        <attributes>
                          <item>
                            <key xsi:type="xsd:string">file</key>
                            <value xsi:type="xsd:string">C:\\Adobe\\Adobe_Experience_Manager_Forms\\gds\\fake\\generated-pdf</value>
                          </item>
                          <item>
                            <key xsi:type="xsd:string">basename</key>
                            <value xsi:type="xsd:string">generated-pdf</value>
                          </item>
                          <item>
                            <key xsi:type="xsd:string">ADOBE_SAVE_MODE_REQUIRED_ATTRIBUTE</key>
                            <value xsi:type="xsd:string">false</value>
                          </item>
                          <item>
                            <key xsi:type="xsd:string">wsfilename</key>
                            <value xsi:type="xsd:string">C:\\Adobe\\Adobe_Experience_Manager_Forms\\gds\\fake\\generated-pdf</value>
                          </item>
                          <item>
                            <key xsi:type="xsd:string">ADOBE_SAVE_MODE_FORCE_COMPRESSED_OBJECTS_ATTRIBUTE</key>
                            <value xsi:type="xsd:string">false</value>
                          </item>
                          <item>
                            <key xsi:type="xsd:string">ADOBE_SAVE_MODE_ATTRIBUTE</key>
                            <value xsi:type="xsd:string">INCREMENTAL</value>
                          </item>
                        </attributes>
                      </result>
                    </invokeOneDocumentResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(base64Pdf);
    }
}
