# Feature brief: pass export templates

## Goal

An exported pass looks like the pass in the app, and the user selects the format that fits the use: a ticket, a lock-screen image, a printable sheet, or the code only.

## User problem

The export draws a white card with the fields as a plain list. The labels are fixed English text. Only the first code is exported. Print sends a raster image, so printed text is not sharp and a PDF has no selectable text.

## Scope

1. Four templates:
   - **Ticket**: the pass view ticket. Header, main block for the pass type, date cells, field grid, perforation, and the code area on white.
   - **Story**: a full-bleed 9:16 image in the pass color with a large code. For the lock screen or the gate.
   - **Sheet**: an A4 or Letter page. The ticket, a cut line, then back fields, notes, locations, and validity. Many pages when the content does not fit.
   - **Code**: the code only, with its alternative text and an optional caption. "Save code image" uses this template.
2. Output format per template: PNG or PDF. The PDF is vector: text and codes stay sharp.
3. Actions: save (PNG to the gallery, PDF through the Storage Access Framework), share, and print. Print uses the same PDF.
4. Options per template. Each template remembers its own options for all passes.

## Decisions

- `PassExportTemplate { TICKET, STORY, SHEET, CODE }` and `PassExportFormat { PNG, PDF }`.
- `PassImageExportOptions` keeps `aspectRatio`, `orientation`, and `content`. It gains `format`, `monochrome`, and `opaqueBackground`. `PassImageAspectRatio` gains `LETTER`.
- `PassExportSettings(template, options: Map<PassExportTemplate, PassImageExportOptions>)` replaces `AppSettings.imageExportOptions`.
- The legacy keys `image_export_aspect_ratio`, `image_export_orientation`, and `image_export_content` stay the Ticket keys, so stored options keep their values. Other templates use `image_export_<template>_*` keys.
- `PassExportDocument` is a pure model built from `PassUiModel`, the options, and resolved labels. It reuses `ticketLayout()`, `headerFields`, `ticketGridFields()`, and `repeatsDate()`. It has no Android dependency.
- One Canvas painter per template draws the document in design units (the ticket is 360 units wide, like the pass view in dp). The same painter draws a `Bitmap` for PNG and a `PdfDocument` page for PDF.
- Codes are drawn as module rectangles from `CrispBarcodeRenderer.renderMatrix`, so they stay sharp in a PDF at any scale.
- The ticket order follows the pass view order: the code is on top when the Code section comes before the Details section.
- A fixed ratio scales the content down to fit, as before. A PNG of a sheet fits all content on one page. A PDF of a sheet stays on one page when the content fits after a shrink to 80 % or more. Longer content paginates at block boundaries.
- The upright ticket on a sheet puts up to four codes in one row, so a pass with many codes fits one page. The narrow stub of the landscape ticket keeps two codes per row.
- Field selection is transient: the export screen keeps excluded field keys only for the current export. The fields show as toggle chips in a folded "Fields" section. The folded section shows the count of included fields.
- A wide frame (a landscape ratio) gets a landscape ticket: the details on one side, the code stub on the other, and an upright perforation. A landscape sheet uses the same ticket.
- In a fixed frame the ticket fills the frame. The layout width is the narrowest that lets the content fit the frame height, so the text stays as large as possible. Then the ticket stretches to the frame height, and the code part takes the extra space with larger codes. An upright frame tries two codes per row and all codes in one row, and keeps the layout with the best balance of text size and code size.
- The preview shows the export canvas on a tonal surface with a hairline edge, so a change of ratio or orientation is visible with a transparent background.
- Several square codes share a grid. The Code template selects the column count that gives the largest codes in the frame.
- The format is a segmented single choice. Styles and content are toggle chips with a check mark.
- The options panel uses at most half of the screen and scrolls, so the preview always stays visible.
- All labels come from string resources.

## Preserved behavior

- Ratios Auto, 1:1, 4:5, 3:4, 9:16, 16:9, and A4, with portrait and landscape.
- Content toggles: artwork, details, notes, code, date, location, and hidden fields. A toggle shows only when the pass has the data.
- PNG to `Pictures/PassTick`, with clean-up when the write fails. Share through FileProvider. Print through the system dialog.
- Zoomable preview with a loading indicator.
- "Save code image" from the pass detail: a tight code-only image.
- A transparent background around Ticket and Code templates by default.

## Out of scope

- Export of more than one pass into one file.
- Custom fonts and custom colors.

## Tests

- Settings: per-template round trip, legacy keys decode to the Ticket options, unknown values fall back to defaults.
- Document model: layout per pass type, label resolution, hidden fields, codes with alternative text, notes, locations, excluded fields, and the content toggles.
- Instrumented: the shape of the PNG and of the PDF page for every template, ratio, and orientation; PNG and PDF output per template, code round trip through the decoder, PDF page count for a long sheet, gallery write, and print adapter output.
