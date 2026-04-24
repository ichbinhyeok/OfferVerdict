package com.offerverdict.service;

import com.offerverdict.model.OfferDocumentExtractResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OfferDocumentExtractServiceTest {

    private final OfferDocumentExtractService service =
            new OfferDocumentExtractService(new OfferDocumentOcrService());

    @Test
    void extract_readsTextFromUploadedImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "sourceFile",
                "offer-shot.png",
                "image/png",
                pngBytes(
                        "CURRENT RN AUSTIN TX $42/HR",
                        "NEW ICU RN OFFER SEATTLE WA $60/HR",
                        "SIGN ON BONUS $15000",
                        "HOSPITAL WIDE FLOAT"));

        OfferDocumentExtractResult result = service.extract("", file);

        assertTrue(result.getSourceLabel().contains("Uploaded image OCR"));
        assertTrue(result.getSourceText().toLowerCase().contains("seattle"));
        assertTrue(result.getSourceText().toLowerCase().contains("icu"));
        assertTrue(result.getSourceText().contains("15000"));
    }

    @Test
    void extract_fallsBackToOcrForScannedPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "sourceFile",
                "offer-scan.pdf",
                "application/pdf",
                imagePdfBytes(
                        "CURRENT RN AUSTIN TX $42/HR",
                        "NEW ICU RN OFFER SEATTLE WA $60/HR",
                        "24 MONTH COMMITMENT",
                        "CAN CANCEL WITHOUT PAY"));

        OfferDocumentExtractResult result = service.extract("", file);

        assertTrue(result.getSourceLabel().contains("Uploaded PDF OCR"));
        assertTrue(result.getSourceText().toLowerCase().contains("seattle"));
        assertTrue(result.getSourceText().contains("60"));
    }

    @Test
    void extract_recoversTextFromTiltedImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "sourceFile",
                "offer-tilted.png",
                "image/png",
                tiltedPngBytes(
                        "REGISTERED NURSE",
                        "SEATTLE WA TELEMETRY",
                        "PAY RANGE MINIMUM $45.59 HOURLY",
                        "PAY RANGE MAXIMUM $84.47 HOURLY",
                        "SIGN ON BONUS $7500"));

        OfferDocumentExtractResult result = service.extract("", file);

        assertTrue(result.getSourceLabel().contains("Uploaded image OCR"));
        assertTrue(result.getSourceText().toLowerCase().contains("seattle"));
        assertTrue(result.getSourceText().toLowerCase().contains("telemetry"));
        assertTrue(result.getSourceText().contains("7500"));
    }

    @Test
    void extract_recoversTextFromSidewaysPhonePhotoLikeImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "sourceFile",
                "offer-phone-photo.png",
                "image/png",
                phonePhotoPngBytes(
                        "REGISTERED NURSE",
                        "SEATTLE WA TELEMETRY",
                        "PAY RANGE MINIMUM $45.59 HOURLY",
                        "PAY RANGE MAXIMUM $84.47 HOURLY",
                        "SIGN ON BONUS $7500"));

        OfferDocumentExtractResult result = service.extract("", file);

        assertTrue(result.getSourceLabel().contains("Uploaded image OCR"));
        assertTrue(result.getSourceText().toLowerCase().contains("telemetry"));
        assertTrue(result.getSourceText().contains("45.59"));
        assertTrue(result.getSourceText().contains("84.47"));
        assertTrue(result.getSourceText().contains("7500"));
    }

    private byte[] pngBytes(String... lines) throws IOException {
        int width = 1900;
        int height = Math.max(1100, 120 + (lines.length * 110));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 44));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int y = 110;
            for (String line : lines) {
                graphics.drawString(line, 80, y);
                y += 100;
            }
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private byte[] imagePdfBytes(String... lines) throws IOException {
        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(pngBytes(lines)));
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(LosslessFactory.createFromImage(document, image), 24, 180, 560, 480);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] tiltedPngBytes(String... lines) throws IOException {
        BufferedImage base = ImageIO.read(new java.io.ByteArrayInputStream(pngBytes(lines)));
        int width = base.getWidth() + 280;
        int height = base.getHeight() + 240;
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(new Color(232, 232, 232));
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            AffineTransform transform = new AffineTransform();
            transform.translate(120, 80);
            transform.rotate(Math.toRadians(-4), base.getWidth() / 2.0, base.getHeight() / 2.0);
            graphics.drawImage(base, transform, null);
        } finally {
            graphics.dispose();
            base.flush();
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(canvas, "png", output);
            canvas.flush();
            return output.toByteArray();
        }
    }

    private byte[] phonePhotoPngBytes(String... lines) throws IOException {
        BufferedImage base = ImageIO.read(new java.io.ByteArrayInputStream(pngBytes(lines)));
        int width = base.getHeight() + 900;
        int height = base.getWidth() + 360;
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(new Color(229, 229, 229));
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            AffineTransform transform = new AffineTransform();
            transform.translate(220, 150);
            transform.rotate(Math.toRadians(91.5), base.getWidth() / 2.0, base.getHeight() / 2.0);
            graphics.drawImage(base, transform, null);
        } finally {
            graphics.dispose();
            base.flush();
        }

        BufferedImage blurred = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
        float[] blurKernel = new float[] {
                1f / 16f, 2f / 16f, 1f / 16f,
                2f / 16f, 4f / 16f, 2f / 16f,
                1f / 16f, 2f / 16f, 1f / 16f
        };
        ConvolveOp blur = new ConvolveOp(new Kernel(3, 3, blurKernel), ConvolveOp.EDGE_NO_OP, null);
        blur.filter(canvas, blurred);
        canvas.flush();

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(blurred, "png", output);
            blurred.flush();
            return output.toByteArray();
        }
    }
}
