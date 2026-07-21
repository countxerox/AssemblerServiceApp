package ext.MA.coversheets;

import java.util.Base64;
import java.util.Map;

public final class InvokeResponseBuilder {
    private InvokeResponseBuilder() { }
    public static String build(Map<String, byte[]> documents) {
        StringBuilder items = new StringBuilder();
        for (Map.Entry<String, byte[]> entry : documents.entrySet()) {
            String name = xml(entry.getKey());
            items.append("<ns2:item><ns2:key xsi:type=\"xsd:string\">").append(name)
                .append("</ns2:key><ns2:value xsi:type=\"ns1:BLOB\"><contentType>application/pdf</contentType><binaryData>")
                .append(Base64.getEncoder().encodeToString(entry.getValue()))
                .append("</binaryData><attributes><item><key xsi:type=\"xsd:string\">basename</key><value xsi:type=\"xsd:string\">")
                .append(name).append(".pdf</value></item><item><key xsi:type=\"xsd:string\">file</key><value xsi:type=\"xsd:string\">")
                .append(name).append(".pdf</value></item></attributes></ns2:value></ns2:item>");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soapenv:Body><invokeResponse xmlns=\"http://adobe.com/idp/services\"><result xsi:type=\"ns1:AssemblerResult\" xmlns:ns1=\"http://adobe.com/idp/services\"><documents xsi:type=\"ns2:Map\" xmlns:ns2=\"http://xml.apache.org/xml-soap\">" + items + "</documents><failedBlockNames/><numRequestedBlocks xsi:type=\"xsd:int\">" + documents.size() + "</numRequestedBlocks></result></invokeResponse></soapenv:Body></soapenv:Envelope>";
    }
    private static String xml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("\u0027", "&apos;"); }
}
