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
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                float y = 750;

                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(50, y);
                content.showText("VaultCore Monthly Statement");
                content.endText();

                y -= 30;

                y = writeLine(content, y, "Account: " + statement.accountNumber());
                y = writeLine(content, y, "Currency: " + statement.currency());
                y = writeLine(content, y, "Month: " + statement.month());
                y = writeLine(content, y, "Opening Balance: " + statement.openingBalance());
                y = writeLine(content, y, "Closing Balance: " + statement.closingBalance());

                y -= 10;
                y = writeLine(content, y, "Total Debits: " + statement.totalDebits());
                y = writeLine(content, y, "Total Credits: " + statement.totalCredits());

                y -= 20;
                y = writeLine(content, y, "Transactions:");

                DateTimeFormatter dt = DateTimeFormatter.ISO_INSTANT;
                for (LedgerEntry e : statement.entries()) {
                    String line = dt.format(e.getCreatedAt()) + " | "
                        + e.getEntryType() + " | "
                        + e.getAmount() + " | "
                        + safe(e.getDescription());
                    y = writeLine(content, y, line);
                    if (y < 100) break;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate PDF", ex);
        }
    }

    private float writeLine(PDPageContentStream content, float y, String text) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 10);
        content.newLineAtOffset(50, y);
        content.showText(text);
        content.endText();
        return y - 14;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}