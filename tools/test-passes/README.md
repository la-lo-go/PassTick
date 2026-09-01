# Test passes

The catalog contains one native `.espass` fixture for every barcode format supported by the app. The fixtures also vary pass type, date shape, visible and hidden fields, locations, validity periods, and alternative barcode text.

Generate the importable files from the repository root:

```powershell
./tools/test-passes/Generate-TestPasses.ps1
```

The files are written to `build/test-passes`. They are intentionally generated outside the source tree so ZIP metadata does not create noisy binary diffs.

These passes contain fictional organizations, people, identifiers, and locations intended only for development and UI testing.
