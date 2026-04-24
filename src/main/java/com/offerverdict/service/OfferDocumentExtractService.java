package com.offerverdict.service;

import com.offerverdict.model.OfferDocumentExtractResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class OfferDocumentExtractService {
    private final OfferDocumentOcrService offerDocumentOcrService;

    public OfferDocumentExtractService(OfferDocumentOcrService offerDocumentOcrService) {
        this.offerDocumentOcrService = offerDocumentOcrService;
    }

    public OfferDocumentExtractResult extract(String sourceText, MultipartFile sourceFile) {
        OfferDocumentExtractResult result = new OfferDocumentExtractResult();
        String pastedText = sourceText == null ? "" : sourceText.trim();

        if (sourceFile == null || sourceFile.isEmpty()) {
            result.setSourceText(pastedText);
            result.setSourceLabel(pastedText.isBlank() ? "No document uploaded yet" : "Pasted offer text");
            result.setFromFile(false);
            return result;
        }

        String filename = safeFilename(sourceFile.getOriginalFilename());
        String extension = extensionOf(filename);
        result.setFromFile(true);
        result.setSourceLabel("Uploaded file: " + filename);

        try {
            if (isPdf(extension, sourceFile.getContentType())) {
                byte[] pdfBytes = sourceFile.getBytes();
                String extractedText = extractPdfText(pdfBytes);
                if (hasUsefulText(extractedText)) {
                    result.setSourceText(extractedText);
                    result.setSourceLabel("Uploaded PDF: " + filename);
                    return result;
                }

                String ocrText = offerDocumentOcrService.extractPdfText(pdfBytes);
                if (hasUsefulText(ocrText)) {
                    result.setSourceText(ocrText);
                    result.setSourceLabel("Uploaded PDF OCR: " + filename);
                    result.setWarning("No selectable text was found in the PDF, so we ran OCR on the pages. Review the extracted terms before running the report.");
                    return result;
                }

                result.setSourceText(pastedText);
                result.setSourceLabel("Uploaded PDF: " + filename);
                result.setWarning("We could not pull enough text from that PDF. Paste the terms manually if the scan is faint.");
                return result;
            }

            if (isPlainText(extension, sourceFile.getContentType())) {
                result.setSourceText(new String(sourceFile.getBytes(), StandardCharsets.UTF_8));
                result.setSourceLabel("Uploaded text file: " + filename);
                return result;
            }

            if (isImage(extension, sourceFile.getContentType())) {
                String ocrText = offerDocumentOcrService.extractImageText(sourceFile.getBytes());
                if (hasUsefulText(ocrText)) {
                    result.setSourceText(ocrText);
                    result.setSourceLabel("Uploaded image OCR: " + filename);
                    result.setWarning("OCR can miss fine print or dense tables. Review the extracted terms before running the report.");
                    return result;
                }

                result.setSourceText(pastedText);
                result.setWarning("We could not read enough text from that image. Try a clearer PNG/JPG or paste the terms manually.");
                result.setSourceLabel("Uploaded image: " + filename);
                return result;
            }

            result.setSourceText(pastedText);
            result.setWarning("That file type is not supported yet. Upload a PDF or text file, or paste the offer text.");
            return result;
        } catch (IOException ex) {
            result.setSourceText(pastedText);
            result.setWarning("We could not read that file. Try a text-based PDF or paste the offer text.");
            return result;
        }
    }

    private String extractPdfText(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private boolean isPdf(String extension, String contentType) {
        return "pdf".equals(extension) || "application/pdf".equalsIgnoreCase(safeContentType(contentType));
    }

    private boolean isPlainText(String extension, String contentType) {
        return "txt".equals(extension)
                || "text".equals(extension)
                || "md".equals(extension)
                || safeContentType(contentType).startsWith("text/");
    }

    private boolean isImage(String extension, String contentType) {
        return "png".equals(extension)
                || "jpg".equals(extension)
                || "jpeg".equals(extension)
                || "webp".equals(extension)
                || safeContentType(contentType).startsWith("image/");
    }

    private String safeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-document";
        }
        return originalFilename.strip();
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.US);
    }

    private String safeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.US);
    }

    private boolean hasUsefulText(String text) {
        return text != null && text.replaceAll("\\s+", "").length() >= 18;
    }
}
