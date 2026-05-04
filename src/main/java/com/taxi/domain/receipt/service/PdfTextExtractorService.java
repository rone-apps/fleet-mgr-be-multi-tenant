package com.taxi.domain.receipt.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfTextExtractorService {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextExtractorService.class);

    public static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_PAGES_FOR_TEXT = 50; // Process up to 50 pages
    private static final int PAGES_PER_BATCH = 3; // Process 3 pages at a time to manage token usage
    private static final int RENDER_DPI = 100; // Reduced from 150 to manage token usage

    /**
     * Extract all text from a digital PDF.
     * Returns empty string if document is password-protected or is a scanned image PDF.
     */
    public String extract(byte[] pdfBytes) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
        try (PDDocument document = PDDocument.load(inputStream)) {
            int pageCount = document.getNumberOfPages();
            logger.info("PDF loaded: {} pages", pageCount);

            if (document.isEncrypted()) {
                logger.warn("PDF is encrypted - text extraction not possible");
                return "";
            }

            int pagesToProcess = Math.min(pageCount, MAX_PAGES_FOR_TEXT);
            if (pageCount > MAX_PAGES_FOR_TEXT) {
                logger.warn("PDF has {} pages - extracting text from first {} only", pageCount, MAX_PAGES_FOR_TEXT);
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(pagesToProcess);
            String text = stripper.getText(document);
            logger.info("Extracted {} characters from PDF ({} pages processed)", text.length(), pagesToProcess);
            return text;
        }
    }

    /**
     * Render the first page of a PDF as a JPEG byte array.
     * Used as fallback when the PDF contains no extractable text (scanned PDF).
     */
    public byte[] renderFirstPage(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, RENDER_DPI, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            byte[] jpeg = baos.toByteArray();
            logger.info("Rendered first page as JPEG: {} bytes", jpeg.length);
            return jpeg;
        }
    }

    /**
     * Render all pages of a PDF as JPEG byte arrays for multi-page processing.
     * Returns up to MAX_PAGES_FOR_TEXT pages.
     */
    public java.util.List<byte[]> renderAllPages(byte[] pdfBytes) throws IOException {
        java.util.List<byte[]> pages = new java.util.ArrayList<>();
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            int pageCount = document.getNumberOfPages();
            int pagesToRender = Math.min(pageCount, MAX_PAGES_FOR_TEXT);
            PDFRenderer renderer = new PDFRenderer(document);

            for (int i = 0; i < pagesToRender; i++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, 150, ImageType.RGB);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpeg", baos);
                    pages.add(baos.toByteArray());
                    logger.debug("Rendered page {} as JPEG: {} bytes", i + 1, baos.size());
                } catch (Exception e) {
                    logger.warn("Failed to render page {}: {}", i + 1, e.getMessage());
                }
            }
            logger.info("Rendered {} pages as JPEG from {} total pages", pages.size(), pageCount);
        }
        return pages;
    }

    /**
     * Count pages without extracting text, for logging/diagnostics.
     */
    public int getPageCount(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            return document.getNumberOfPages();
        }
    }
}
