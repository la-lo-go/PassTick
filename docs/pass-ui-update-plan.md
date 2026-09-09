# Pass UI update

## Scope

Preserve the current working changes and all stored passes. Do not publish the repository.

## Acceptance checks

- Align the top of pass artwork with the top of the text.
- Use a softer blur with smooth edges.
- Export a full pass, its code, or selected content as a PNG image.
- Let a pass have multiple tags. Let users edit tag names, colors, and icons.
- Convert existing favorites to pinned passes. Keep pinned passes at the top of the home page.
- Store archive state separately from tags. Derive past state from event dates.
- Add dynamic tag filters and All, Pinned, and Archived filters to the drawer.
- Put pass-view and home-card layout controls in Settings. Use a suitable pass-view icon.
- Update card blur, lock, and pin indicators as soon as state changes.
- Close the previous card action group when another group opens.
- Hide pass type in the default home-card layout.
- Add a primary SVG ticket mark with square corners and a transparent center cut. Use it for the themed launch screen.
- Use connected, full-height side action groups with the existing action colors.
- Use a single-select segmented sort group with one-word labels.
- Prevent a crash when Back and the drawer button are tapped in quick succession.
- Keep the calendar action icon. Put the confirmation tick next to the calendar status text.
- Reverse timeline order. Hide actions at the sides and remove tag badges.
- Select the highest-resolution pass icon available in the archive.
- Put pin and lock indicators together at the top right of each card.
- Do not use an event end date to include a pass in Today.

## Validation

Add characterization tests before changing persistence or temporal behavior. Run focused tests during development. Run all unit tests, lint, a release build, and a complexity check after integration. Use the connected physical Android device for device tests. Do not erase its application data.

## Work allocation

- Domain agent: storage, migration, state, image selection, and temporal rules.
- Home agent: cards, drawer, action groups, sorting, and timeline.
- Detail agent: detail view, image export, settings, navigation, and launch artwork.
- Main agent: integration checks, review, and physical-device validation.

All code changes are assigned to GPT-5.6 Luna.
