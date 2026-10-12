# Feature brief: pass editor redesign

## Goal

Each part of a pass is easy to find in the editor, each control has its own purpose, and no part of the pass is lost when it is saved.

## User problem

The editor is one long form without a preview. Fields are label and value pairs without their place on the ticket, and each field shows four icons. Only the first code is editable, and saving a pass with several codes deletes the other codes. Formats, types, and image kinds show internal names. Places need latitude and longitude as text. Images cannot be removed. Errors show in a message far from the field.

## Scope

1. Folded sections with a summary line: Basics, Date and time, Fields, Codes, Places, Images, Note. One section is open at a time.
2. Basics: name, issuer, type as a connected single choice with readable names, and color.
3. Fields: one row per field with its label, value, and slot. A row opens to edit the label and the value. One line below holds the slot choice (Header, Main, Details, Back) and the delete action, so no line holds a single control. A drag handle reorders the rows.
4. Codes: the full list of codes. Each code shows a live code image, a format menu with readable names, the data, and the text below the code. Add a code, read a code from a photo, remove a code.
5. Places: a name per place. The exact position (latitude and longitude) is in a folded part.
6. Images: one row per image kind with a readable name, a thumbnail, and replace and remove actions.
7. Validation: the section with the problem opens and shows the error below its content.

## Decisions

- The save behavior does not change: back saves, the editor menu discards, and the bottom button saves and closes.
- Create and edit use the same editor.
- `PassDraft` and `PassUpdate` replace the single code values with `codes: List<PassCodeDraft>` and `barcodes: List<PassBarcodeSnapshot>`. The repository stores the first code in `barCode` and the others in `barCodes`, as the pass import does.
- `PassDraft.artworkRemovals` and `PassUpdate.artworkRemovals` list the image kinds to delete.
- Field slots map to the existing hints: Header is `headerFields`, Main is `primaryFields`, Details keeps `secondaryFields` or `auxiliaryFields` (new fields use `secondaryFields`), Back sets `hidden` and `backFields`.
- The editor has no ticket preview. The folded summaries show the content, and the pass view shows the result.

## Out of scope

- A new save model.
- A guided flow for new passes.
- A map picker for places.
- A live ticket preview: it took a third of the screen and the summaries already show the content.

## Tests

- Repository: saving keeps every code in order with its alternative text; removing an image deletes its files.
- Draft: conversion keeps all codes; validation rejects a code without data and reports the section.
- Compose: each folded section shows its summary, one section opens at a time, a field slot change moves the field, every code survives a save, and a validation error opens its section.
