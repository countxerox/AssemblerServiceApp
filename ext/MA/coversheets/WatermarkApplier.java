package ext.MA.coversheets;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.w3c.dom.Element;

final class WatermarkApplier {
    private WatermarkApplier() { }
    static Path apply(Path source, Element watermark, Path directory) throws IOException {
        String text = watermarkText(watermark).trim();
        if (text.isEmpty()) return source;
        Element styled = child(watermark, "StyledText");
        float size = points(styled == null ? null : styled.getAttribute("font-size"), 8f);
        boolean bold = styled != null && "bold".equalsIgnoreCase(styled.getAttribute("font-weight"));
        Color color = color(styled == null ? null : styled.getAttribute("color"));
        float horizontalOffset = points(watermark.getAttribute("horizontalOffset"), 0f);
        float verticalOffset = points(watermark.getAttribute("verticalOffset"), 0f);
        float rotation = number(watermark.getAttribute("rotation"), 0f);
        String horizontal = defaultValue(watermark.getAttribute("horizontalAnchor"), "Left");
        String vertical = defaultValue(watermark.getAttribute("verticalAnchor"), "Bottom");
        PDType1Font font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        Path target = Files.createTempFile(directory, "watermarked-", ".pdf");
        try (PDDocument document = PDDocument.load(source.toFile())) {
            for (PDPage page : document.getPages()) draw(document, page, text, font, size, color, horizontal, vertical, horizontalOffset, verticalOffset, rotation);
            document.save(target.toFile());
        }
        return target;
    }
    private static void draw(PDDocument document, PDPage page, String text, PDType1Font font, float size, Color color, String horizontal, String vertical, float xOffset, float yOffset, float rotation) throws IOException {
        PDRectangle box = page.getCropBox(); float width = font.getStringWidth(text) / 1000f * size;
        float x = box.getLowerLeftX() + xOffset;
        if ("Center".equalsIgnoreCase(horizontal)) x = box.getLowerLeftX() + (box.getWidth() - width) / 2f + xOffset;
        else if ("Right".equalsIgnoreCase(horizontal)) x = box.getUpperRightX() - width + xOffset;
        float y = box.getLowerLeftY() + yOffset;
        if ("Center".equalsIgnoreCase(vertical)) y = box.getLowerLeftY() + (box.getHeight() - size) / 2f + yOffset;
        else if ("Top".equalsIgnoreCase(vertical)) y = box.getUpperRightY() - size + yOffset;
        try (PDPageContentStream stream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.saveGraphicsState(); stream.setNonStrokingColor(color); stream.beginText(); stream.setFont(font, size); stream.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(rotation), x, y)); stream.showText(text); stream.endText(); stream.restoreGraphicsState();
        }
    }
    private static Element child(Element parent, String name) { for (org.w3c.dom.Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && name.equals(n.getLocalName())) return (Element) n; return null; }
    private static String watermarkText(Element watermark) { Element styled = child(watermark, "StyledText"); return styled == null ? "" : styled.getTextContent(); }
    private static float points(String value, float fallback) { if (value == null || value.isBlank()) return fallback; String v = value.trim().toLowerCase(); try { return v.endsWith("pt") ? Float.parseFloat(v.substring(0, v.length() - 2)) : Float.parseFloat(v); } catch (NumberFormatException e) { throw new IllegalArgumentException("Unsupported watermark length: " + value); } }
    private static float number(String value, float fallback) { if (value == null || value.isBlank()) return fallback; try { return Float.parseFloat(value.trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Unsupported watermark rotation: " + value); } }
    private static String defaultValue(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static Color color(String value) { if (value == null || value.isBlank() || "black".equalsIgnoreCase(value)) return Color.BLACK; if ("white".equalsIgnoreCase(value)) return Color.WHITE; if ("red".equalsIgnoreCase(value)) return Color.RED; if (value.matches("#[0-9a-fA-F]{6}")) return new Color(Integer.parseInt(value.substring(1), 16)); throw new IllegalArgumentException("Unsupported watermark color: " + value); }
}
