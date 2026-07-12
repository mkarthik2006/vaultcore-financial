package com.vaultcore.core.statement;

import com.vaultcore.core.ledger.LedgerEntry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Component
public class StatementPdfRenderer {

    public byte[] render(MonthlyStatement statement) {
        try (PDDocument doc = new PDDocument();
             PageWriter writer = new PageWriter(doc)) {

            writer.heading("VaultCore Monthly Statement");
            writer.gap(14);

            writer.line("Account: " + statement.accountNumber());
            writer.line("Currency: " + statement.currency());
            writer.line("Month: " + statement.month());
            writer.line("Opening Balance: " + statement.openingBalance());
            writer.line("Closing Balance: " + statement.closingBalance());

            writer.gap(10);
            writer.line("Total Debits: " + statement.totalDebits());
            writer.line("Total Credits: " + statement.totalCredits());

            writer.gap(20);
            writer.line("Transactions:");

            DateTimeFormatter dt = DateTimeFormatter.ISO_INSTANT;
            for (LedgerEntry e : statement.entries()) {
                String line = dt.format(e.getCreatedAt()) + " | "
                    + e.getEntryType() + " | "
                    + e.getAmount() + " | "
                    + safe(e.getDescription());
                // Overflow now flows onto a new page instead of being silently dropped.
                writer.line(line);
            }

            writer.finish();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }

    private String safe(String text) {
        if (text == null) {
            return "";
        }
        // PDFBox standard-14 fonts only encode WinAnsi; replace control/non-encodable characters so
        // an odd description can never crash statement generation.
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            sb.append((c >= 32 && c < 127) ? c : '?');
        }
        return sb.toString();
    }

    /**
     * Cursor over a growing PDF: writes lines top-to-bottom and starts a fresh page automatically
     * when the bottom margin is reached, so statements of any length render in full.
     */
    private static final class PageWriter implements AutoCloseable {

        private static final float TOP = 750f;
        private static final float BOTTOM = 60f;
        private static final float LEFT = 50f;
        private static final float LEADING = 14f;

        private final PDDocument doc;
        private PDPageContentStream content;
        private float y;

        PageWriter(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        void heading(String text) throws IOException {
            write(text, PDType1Font.HELVETICA_BOLD, 16);
            y -= 16;
        }

        void line(String text) throws IOException {
            if (y <= BOTTOM) {
                newPage();
            }
            write(text, PDType1Font.HELVETICA, 10);
            y -= LEADING;
        }

        void gap(float pixels) {
            y -= pixels;
        }

        /** Closes the active content stream so the document can be saved. */
        void finish() throws IOException {
            close();
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage();
            doc.addPage(page);
            content = new PDPageContentStream(doc, page);
            y = TOP;
        }

        private void write(String text, PDType1Font font, int size) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(LEFT, y);
            content.showText(text);
            content.endText();
        }

        @Override
        public void close() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }
    }
}
