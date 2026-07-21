package ext.MA.coversheets;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.multipdf.PDFMergerUtility;


public class PDFMerger {

    public static byte[] merge(byte[] firstPdf, byte[] secondPdf) throws Exception {
        return merge(List.of(firstPdf, secondPdf));
    }

    public static byte[] merge(List<byte[]> pdfs) throws Exception {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        merger.setDestinationStream(output);
        for (byte[] pdf : pdfs) {
            merger.addSource(new ByteArrayInputStream(pdf));
        }
        merger.mergeDocuments(null);
        return output.toByteArray();
    }
}
