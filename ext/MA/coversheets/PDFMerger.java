package ext.MA.coversheets;

import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

public final class PDFMerger {
    private PDFMerger() { }
    public static void merge(List<Path> sources, Path destination) throws Exception {
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(destination.toString());
        for (Path source : sources) merger.addSource(source.toFile());
        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
    }
}
