package org.ligi.passandroid.ui.state

import java.text.Normalizer
import java.util.Locale

internal fun PassUiModel.searchDocument(categoryName: String? = null): String = buildString {
    append(description).append(' ')
    append(creator.orEmpty()).append(' ')
    append(type.name).append(' ')
    append(barcodeAlternativeText.orEmpty()).append(' ')
    append(barcodeMessage.orEmpty()).append(' ')
    append(notes).append(' ')
    append(categoryName.orEmpty()).append(' ')
    append(calendarTimeSpan?.from?.toString().orEmpty()).append(' ')
    append(calendarTimeSpan?.to?.toString().orEmpty()).append(' ')
    fields.forEach { field ->
        append(field.label).append(' ')
        append(field.value).append(' ')
        append(field.hint.orEmpty()).append(' ')
    }
    locations.forEach { append(it.name.orEmpty()).append(' ') }
}.normalizedForSearch()

internal fun String.searchTerms(): List<String> = normalizedForSearch()
    .split(' ')
    .filter(String::isNotBlank)
    .distinct()

private fun String.normalizedForSearch(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(DIACRITICS_PATTERN, "")
    .lowercase(Locale.ROOT)

private val DIACRITICS_PATTERN = Regex("\\p{M}+")
