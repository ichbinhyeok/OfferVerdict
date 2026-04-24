package com.offerverdict.service;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.RenderingHints;
import java.awt.image.ConvolveOp;
import java.awt.image.BufferedImage;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OfferDocumentOcrService {
    private static final String LANGUAGE = "eng";
    private static final int MIN_RENDER_WIDTH = 1800;
    private static final int PDF_RENDER_DPI = 220;
    private static final int MAX_PDF_OCR_PAGES = 6;
    private static final double[] IMAGE_OCR_ROTATIONS = new double[] {
            0.0, -4.0, 4.0,
            88.5, 90.0, 91.5,
            -88.5, -90.0, -91.5,
            180.0
    };
    private static final int[] IMAGE_PAGE_SEGMENTATION_MODES = new int[] {
            ITessAPI.TessPageSegMode.PSM_AUTO,
            ITessAPI.TessPageSegMode.PSM_SPARSE_TEXT,
            ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK,
            ITessAPI.TessPageSegMode.PSM_SINGLE_COLUMN
    };

    private final Object tessdataLock = new Object();
    private final Object ocrLock = new Object();
    private Path tessdataDirectory;

    public String extractImageText(byte[] bytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            return "";
        }
        return runBestOcr(buildImageCandidates(image));
    }

    public String extractPdfText(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder text = new StringBuilder();
            int pageCount = Math.min(document.getNumberOfPages(), MAX_PDF_OCR_PAGES);
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage rendered = renderer.renderImageWithDPI(pageIndex, PDF_RENDER_DPI, ImageType.RGB);
                String pageText = runOcr(prepareBinaryCandidate(rendered));
                if (!pageText.isBlank()) {
                    if (text.length() > 0) {
                        text.append("\n\n");
                    }
                    text.append(pageText.trim());
                }
            }
            return text.toString().trim();
        }
    }

    private String runBestOcr(List<BufferedImage> candidates) throws IOException {
        synchronized (ocrLock) {
            try {
                String bestText = "";
                int bestScore = -1;
                for (int pageSegMode : IMAGE_PAGE_SEGMENTATION_MODES) {
                    Tesseract tesseract = createTesseract(pageSegMode);
                    for (BufferedImage candidate : candidates) {
                        String text = normalizeOcrText(tesseract.doOCR(candidate));
                        int score = scoreOcrText(text);
                        if (score > bestScore) {
                            bestScore = score;
                            bestText = text;
                        }
                    }
                }
                return bestText;
            } catch (TesseractException ex) {
                throw new IOException("OCR failed", ex);
            }
        }
    }

    private String runOcr(BufferedImage image) throws IOException {
        synchronized (ocrLock) {
            try {
                Tesseract tesseract = createTesseract(ITessAPI.TessPageSegMode.PSM_AUTO);
                return normalizeOcrText(tesseract.doOCR(image));
            } catch (TesseractException ex) {
                throw new IOException("OCR failed", ex);
            }
        }
    }

    private Tesseract createTesseract(int pageSegMode) throws IOException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(ensureTessdataDirectory().toString());
        tesseract.setLanguage(LANGUAGE);
        tesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        tesseract.setPageSegMode(pageSegMode);
        tesseract.setVariable("user_defined_dpi", "300");
        tesseract.setVariable("preserve_interword_spaces", "1");
        return tesseract;
    }

    private List<BufferedImage> buildImageCandidates(BufferedImage source) {
        BufferedImage normalized = copyToRgb(source);
        List<BufferedImage> baseImages = new ArrayList<>();
        baseImages.add(normalized);

        BufferedImage paperCropped = cropToPaperBounds(normalized);
        if (paperCropped != normalized) {
            baseImages.add(paperCropped);
        }

        BufferedImage rectified = rectifyDocument(normalized);
        if (rectified != null) {
            baseImages.add(rectified);
        }

        if (paperCropped != normalized) {
            BufferedImage rectifiedPaper = rectifyDocument(paperCropped);
            if (rectifiedPaper != null) {
                baseImages.add(rectifiedPaper);
            }
        }

        List<BufferedImage> candidates = new ArrayList<>();
        for (BufferedImage baseImage : baseImages) {
            BufferedImage croppedBase = cropToInkBounds(baseImage);
            for (double rotation : IMAGE_OCR_ROTATIONS) {
                BufferedImage rotated = Math.abs(rotation) < 0.01 ? croppedBase : rotate(croppedBase, rotation);
                BufferedImage cropped = cropToInkBounds(rotated);
                candidates.add(prepareBinaryCandidate(cropped));
                candidates.add(prepareGrayCandidate(cropped));
                candidates.add(prepareAdaptiveBinaryCandidate(cropped));
            }
        }
        return candidates;
    }

    private BufferedImage prepareBinaryCandidate(BufferedImage source) {
        BufferedImage contrasted = prepareGrayCandidate(source);
        BufferedImage binary = new BufferedImage(contrasted.getWidth(), contrasted.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D binaryGraphics = binary.createGraphics();
        try {
            binaryGraphics.setColor(Color.WHITE);
            binaryGraphics.fillRect(0, 0, contrasted.getWidth(), contrasted.getHeight());
            binaryGraphics.drawImage(contrasted, 0, 0, null);
        } finally {
            binaryGraphics.dispose();
        }
        return binary;
    }

    private BufferedImage prepareGrayCandidate(BufferedImage source) {
        double scale = source.getWidth() >= MIN_RENDER_WIDTH ? 1.0 : (double) MIN_RENDER_WIDTH / source.getWidth();
        int targetWidth = Math.max(source.getWidth(), (int) Math.round(source.getWidth() * scale));
        int targetHeight = Math.max(source.getHeight(), (int) Math.round(source.getHeight() * scale));

        BufferedImage gray = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        BufferedImage stretched = stretchContrast(gray);
        RescaleOp contrast = new RescaleOp(1.18f, -12f, null);
        BufferedImage contrasted = contrast.filter(stretched, null);
        return sharpen(contrasted);
    }

    private BufferedImage prepareAdaptiveBinaryCandidate(BufferedImage source) {
        BufferedImage gray = prepareGrayCandidate(source);
        int width = gray.getWidth();
        int height = gray.getHeight();
        int radius = Math.max(18, Math.min(56, Math.max(width, height) / 44));
        int bias = 12;

        int[][] integral = new int[height + 1][width + 1];
        for (int y = 0; y < height; y++) {
            int rowSum = 0;
            for (int x = 0; x < width; x++) {
                rowSum += gray.getRaster().getSample(x, y, 0);
                integral[y + 1][x + 1] = integral[y][x + 1] + rowSum;
            }
        }

        BufferedImage binary = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < height; y++) {
            int y0 = Math.max(0, y - radius);
            int y1 = Math.min(height - 1, y + radius);
            for (int x = 0; x < width; x++) {
                int x0 = Math.max(0, x - radius);
                int x1 = Math.min(width - 1, x + radius);
                int area = (x1 - x0 + 1) * (y1 - y0 + 1);
                int sum = integral[y1 + 1][x1 + 1] - integral[y0][x1 + 1] - integral[y1 + 1][x0] + integral[y0][x0];
                int threshold = (int) Math.round(sum / (double) area) - bias;
                int value = gray.getRaster().getSample(x, y, 0);
                binary.setRGB(x, y, value < threshold ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return binary;
    }

    private BufferedImage rectifyDocument(BufferedImage source) {
        BufferedImage gray = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, gray.getWidth(), gray.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        int width = gray.getWidth();
        int height = gray.getHeight();
        int threshold = estimateDocumentThreshold(gray);
        int minRowSpan = Math.max(200, (int) Math.round(width * 0.38));
        int[] leftEdges = new int[height];
        int[] rightEdges = new int[height];
        boolean[] validRows = new boolean[height];
        int validCount = 0;
        long spanSum = 0;

        for (int y = 0; y < height; y++) {
            int left = -1;
            int right = -1;
            for (int x = 0; x < width; x++) {
                int value = gray.getRaster().getSample(x, y, 0);
                if (value >= threshold) {
                    left = x;
                    break;
                }
            }
            for (int x = width - 1; x >= 0; x--) {
                int value = gray.getRaster().getSample(x, y, 0);
                if (value >= threshold) {
                    right = x;
                    break;
                }
            }

            if (left >= 0 && right > left && (right - left) >= minRowSpan) {
                validRows[y] = true;
                leftEdges[y] = left;
                rightEdges[y] = right;
                validCount++;
                spanSum += (right - left);
            }
        }

        if (validCount < Math.max(40, (int) Math.round(height * 0.18))) {
            return null;
        }

        int top = -1;
        int bottom = -1;
        for (int y = 0; y < height; y++) {
            if (validRows[y]) {
                if (top < 0) {
                    top = y;
                }
                bottom = y;
            }
        }

        if (top < 0 || bottom <= top) {
            return null;
        }

        double averageSpan = spanSum / (double) validCount;
        if (averageSpan >= width * 0.97) {
            return null;
        }

        int outputWidth = Math.max(800, (int) Math.round(averageSpan));
        int outputHeight = bottom - top + 1;
        if (outputHeight < height * 0.35) {
            return null;
        }

        BufferedImage rectified = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        int currentLeft = -1;
        int currentRight = -1;

        for (int srcY = top; srcY <= bottom; srcY++) {
            if (validRows[srcY]) {
                currentLeft = leftEdges[srcY];
                currentRight = rightEdges[srcY];
            }
            if (currentLeft < 0 || currentRight <= currentLeft) {
                continue;
            }

            double span = Math.max(1.0, currentRight - currentLeft);
            int outY = srcY - top;
            for (int outX = 0; outX < outputWidth; outX++) {
                double srcX = currentLeft + (span * outX / Math.max(1, outputWidth - 1));
                rectified.setRGB(outX, outY, sampleRgb(source, srcX, srcY));
            }
        }

        return rectified;
    }

    private BufferedImage cropToInkBounds(BufferedImage source) {
        BufferedImage gray = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, gray.getWidth(), gray.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        int minX = gray.getWidth();
        int minY = gray.getHeight();
        int maxX = -1;
        int maxY = -1;
        int threshold = 240;
        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int value = gray.getRaster().getSample(x, y, 0);
                if (value < threshold) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        if (maxX <= minX || maxY <= minY) {
            return source;
        }

        int paddingX = Math.max(60, (int) Math.round(source.getWidth() * 0.04));
        int paddingY = Math.max(48, (int) Math.round(source.getHeight() * 0.06));
        minX = Math.max(0, minX - paddingX);
        minY = Math.max(0, minY - paddingY);
        maxX = Math.min(source.getWidth() - 1, maxX + paddingX);
        maxY = Math.min(source.getHeight() - 1, maxY + paddingY);

        if ((maxX - minX) < source.getWidth() * 0.35 || (maxY - minY) < source.getHeight() * 0.25) {
            return source;
        }

        BufferedImage cropped = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D croppedGraphics = cropped.createGraphics();
        try {
            croppedGraphics.setColor(Color.WHITE);
            croppedGraphics.fillRect(0, 0, cropped.getWidth(), cropped.getHeight());
            croppedGraphics.drawImage(source, 0, 0, cropped.getWidth(), cropped.getHeight(),
                    minX, minY, maxX + 1, maxY + 1, null);
        } finally {
            croppedGraphics.dispose();
        }
        return cropped;
    }

    private BufferedImage cropToPaperBounds(BufferedImage source) {
        BufferedImage gray = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, gray.getWidth(), gray.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        int threshold = Math.min(248, estimateDocumentThreshold(gray) + 6);
        int minX = gray.getWidth();
        int minY = gray.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int value = gray.getRaster().getSample(x, y, 0);
                if (value >= threshold) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }

        if (maxX <= minX || maxY <= minY) {
            return source;
        }

        if ((maxX - minX) < source.getWidth() * 0.45 || (maxY - minY) < source.getHeight() * 0.45) {
            return source;
        }

        int padding = 18;
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        maxX = Math.min(source.getWidth() - 1, maxX + padding);
        maxY = Math.min(source.getHeight() - 1, maxY + padding);

        BufferedImage cropped = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D croppedGraphics = cropped.createGraphics();
        try {
            croppedGraphics.setColor(Color.WHITE);
            croppedGraphics.fillRect(0, 0, cropped.getWidth(), cropped.getHeight());
            croppedGraphics.drawImage(source, 0, 0, cropped.getWidth(), cropped.getHeight(),
                    minX, minY, maxX + 1, maxY + 1, null);
        } finally {
            croppedGraphics.dispose();
        }
        return cropped;
    }

    private BufferedImage copyToRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage normalized = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, normalized.getWidth(), normalized.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return normalized;
    }

    private BufferedImage stretchContrast(BufferedImage gray) {
        int[] histogram = new int[256];
        int total = gray.getWidth() * gray.getHeight();
        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                histogram[gray.getRaster().getSample(x, y, 0)]++;
            }
        }

        int lowTarget = Math.max(1, (int) Math.round(total * 0.015));
        int highTarget = Math.max(1, (int) Math.round(total * 0.985));
        int running = 0;
        int low = 0;
        int high = 255;
        for (int value = 0; value < histogram.length; value++) {
            running += histogram[value];
            if (running >= lowTarget) {
                low = value;
                break;
            }
        }

        running = 0;
        for (int value = 0; value < histogram.length; value++) {
            running += histogram[value];
            if (running >= highTarget) {
                high = value;
                break;
            }
        }

        if (high - low < 20) {
            return gray;
        }

        BufferedImage stretched = new BufferedImage(gray.getWidth(), gray.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                int value = gray.getRaster().getSample(x, y, 0);
                int normalizedValue = (int) Math.round(((value - low) * 255.0) / Math.max(1, high - low));
                normalizedValue = Math.max(0, Math.min(255, normalizedValue));
                stretched.getRaster().setSample(x, y, 0, normalizedValue);
            }
        }
        return stretched;
    }

    private BufferedImage sharpen(BufferedImage source) {
        float[] blurKernel = new float[] {
                1f / 16f, 2f / 16f, 1f / 16f,
                2f / 16f, 4f / 16f, 2f / 16f,
                1f / 16f, 2f / 16f, 1f / 16f
        };
        ConvolveOp blur = new ConvolveOp(new Kernel(3, 3, blurKernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage blurred = blur.filter(source, null);
        BufferedImage sharpened = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int base = source.getRaster().getSample(x, y, 0);
                int soft = blurred.getRaster().getSample(x, y, 0);
                int value = (int) Math.round((base * 1.28) - (soft * 0.28));
                value = Math.max(0, Math.min(255, value));
                sharpened.getRaster().setSample(x, y, 0, value);
            }
        }
        return sharpened;
    }

    private int estimateDocumentThreshold(BufferedImage gray) {
        int[] histogram = new int[256];
        int total = gray.getWidth() * gray.getHeight();
        for (int y = 0; y < gray.getHeight(); y++) {
            for (int x = 0; x < gray.getWidth(); x++) {
                histogram[gray.getRaster().getSample(x, y, 0)]++;
            }
        }

        int target = Math.max(1, (int) Math.round(total * 0.16));
        int running = 0;
        for (int value = 255; value >= 0; value--) {
            running += histogram[value];
            if (running >= target) {
                return Math.max(205, Math.min(245, value - 8));
            }
        }
        return 225;
    }

    private int sampleRgb(BufferedImage image, double x, int y) {
        int clampedY = Math.max(0, Math.min(image.getHeight() - 1, y));
        int x0 = Math.max(0, Math.min(image.getWidth() - 1, (int) Math.floor(x)));
        int x1 = Math.max(0, Math.min(image.getWidth() - 1, x0 + 1));
        double fraction = Math.max(0.0, Math.min(1.0, x - x0));
        int rgb0 = image.getRGB(x0, clampedY);
        int rgb1 = image.getRGB(x1, clampedY);
        int r = (int) Math.round(((rgb0 >> 16) & 0xff) * (1.0 - fraction) + ((rgb1 >> 16) & 0xff) * fraction);
        int g = (int) Math.round(((rgb0 >> 8) & 0xff) * (1.0 - fraction) + ((rgb1 >> 8) & 0xff) * fraction);
        int b = (int) Math.round((rgb0 & 0xff) * (1.0 - fraction) + (rgb1 & 0xff) * fraction);
        return (r << 16) | (g << 8) | b;
    }

    private BufferedImage rotate(BufferedImage source, double degrees) {
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth = (int) Math.floor(source.getWidth() * cos + source.getHeight() * sin);
        int newHeight = (int) Math.floor(source.getHeight() * cos + source.getWidth() * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rotated.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, newWidth, newHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            AffineTransform transform = new AffineTransform();
            transform.translate((newWidth - source.getWidth()) / 2.0, (newHeight - source.getHeight()) / 2.0);
            transform.rotate(radians, source.getWidth() / 2.0, source.getHeight() / 2.0);
            graphics.drawImage(source, transform, null);
        } finally {
            graphics.dispose();
        }
        return rotated;
    }

    private int scoreOcrText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int alphaNumeric = text.replaceAll("[^A-Za-z0-9$]", "").length();
        String lower = text.toLowerCase(Locale.US);
        int keywordBonus = 0;
        for (String keyword : List.of("registered nurse", "rn", "hour", "bonus", "shift", "city", "seattle", "los angeles", "telemetry")) {
            if (lower.contains(keyword)) {
                keywordBonus += 12;
            }
        }
        return alphaNumeric + keywordBonus;
    }

    private Path ensureTessdataDirectory() throws IOException {
        synchronized (tessdataLock) {
            if (tessdataDirectory != null) {
                return tessdataDirectory;
            }

            Path directory = Path.of(System.getProperty("java.io.tmpdir"), "offerverdict-tessdata");
            Files.createDirectories(directory);
            Path trainedData = directory.resolve(LANGUAGE + ".traineddata");
            if (Files.notExists(trainedData) || Files.size(trainedData) == 0) {
                ClassPathResource resource = new ClassPathResource("tessdata/" + LANGUAGE + ".traineddata");
                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, trainedData, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            tessdataDirectory = directory;
            return directory;
        }
    }

    private String normalizeOcrText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ')
                .replaceAll("\\r\\n?", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll(" {2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
