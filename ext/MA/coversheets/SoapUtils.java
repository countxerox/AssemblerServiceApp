package ext.MA.coversheets;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoapUtils {

    public static String extractAndDecodeFirstBinaryData(String soap) {
        Pattern p = Pattern.compile("<(?:\\w+:)?binaryData>(.*?)</(?:\\w+:)?binaryData>", Pattern.DOTALL);
        Matcher m = p.matcher(soap);

        if (!m.find()) {
            return "";
        }

        String base64 = m.group(1).replaceAll("\\s+", "");

        try {
            return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    
    public static byte[] extractBlobByKey(String soap, String keyName) {
        String patternText =
                "<(?:\\w+:)?key>" + Pattern.quote(keyName) + "</(?:\\w+:)?key>.*?"
              + "<(?:\\w+:)?binaryData>(.*?)</(?:\\w+:)?binaryData>";

        Pattern p = Pattern.compile(patternText, Pattern.DOTALL);
        Matcher m = p.matcher(soap);

        if (!m.find()) {
            throw new IllegalArgumentException("Could not find BLOB for key: " + keyName);
        }

        String base64 = m.group(1).replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }
}
