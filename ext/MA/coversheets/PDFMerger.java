package ext.MA.coversheets;


import org.apache.pdfbox.multipdf.PDFMergerUtility;


import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.io.RandomAccessReadBuffer;


public class PDFMerger {

    public static byte[] merge(byte[] firstPdf, byte[] secondPdf) throws Exception {
    	PDFMergerUtility merger = new PDFMergerUtility();

    	ByteArrayOutputStream output = new ByteArrayOutputStream();
    	merger.setDestinationStream(output);

    	merger.addSource(new RandomAccessReadBuffer(firstPdf));
    	merger.addSource(new RandomAccessReadBuffer(secondPdf));

    	merger.mergeDocuments(null);

    	return output.toByteArray();
    }
}
