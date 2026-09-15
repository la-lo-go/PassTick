package org.ligi.passandroid.ui.compose

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.ligi.passandroid.R

internal enum class LicenseKind(@RawRes val textRes: Int, @StringRes val labelRes: Int) {
    APACHE_2_0(R.raw.apache_license_2_0, R.string.license_apache_2_0),
    MIT(R.raw.mit_license, R.string.license_mit),
    BSD_3_CLAUSE(R.raw.bsd_3_clause_license, R.string.license_bsd_3_clause),
}

internal data class LicensedComponent(
    val name: String,
    val copyright: String,
    val kind: LicenseKind,
)

internal val licensedComponents: List<LicensedComponent> = listOf(
    LicensedComponent(
        name = "AndroidX Activity",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "AndroidX Annotation",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "AndroidX Core",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "AndroidX DataStore",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "AndroidX Glance",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "AndroidX Lifecycle",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "AndroidX Navigation",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Jetpack Compose",
        copyright = "Copyright (c) The Android Open Source Project",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Kotlin Standard Library",
        copyright = "Copyright (c) JetBrains s.r.o. and Kotlin Programming Language contributors",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "kotlinx.coroutines",
        copyright = "Copyright (c) 2016-present JetBrains s.r.o.",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "kotlinx.serialization",
        copyright = "Copyright (c) 2016-present JetBrains s.r.o.",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Koin",
        copyright = "Copyright (c) 2015-present Arnaud Giuliani",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Material Color Utilities",
        copyright = "Copyright (c) Google LLC",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Moshi",
        copyright = "Copyright (c) 2015 Square, Inc.",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Okio",
        copyright = "Copyright (c) 2013 Square, Inc.",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "ThreeTenABP",
        copyright = "Copyright (c) 2015 Jake Wharton",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Timber",
        copyright = "Copyright (c) 2013 Jake Wharton",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "ZXing Core",
        copyright = "Copyright (c) 2007-present ZXing authors",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "zip4j",
        copyright = "Copyright (c) 2010 Srikanth Reddy Lingala",
        kind = LicenseKind.APACHE_2_0,
    ),
    LicensedComponent(
        name = "Colormath",
        copyright = "Copyright (c) 2021 AJ Alt",
        kind = LicenseKind.MIT,
    ),
    LicensedComponent(
        name = "material-kolor",
        copyright = "Copyright (c) 2025 Jordon de Hoog",
        kind = LicenseKind.MIT,
    ),
    LicensedComponent(
        name = "ThreeTen Backport",
        copyright = "Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos",
        kind = LicenseKind.BSD_3_CLAUSE,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_third_party_licenses)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.pass_detail_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.licenses_intro),
                    Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(LicenseKind.entries.toList(), key = { it.name }) { kind ->
                val components = licensedComponents.filter { it.kind == kind }
                if (components.isNotEmpty()) {
                    LicenseCard(kind, components)
                }
            }
            item {
                Text(
                    stringResource(R.string.licenses_app_license_note),
                    Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LicenseCard(kind: LicenseKind, components: List<LicensedComponent>) {
    var expanded by rememberSaveable(kind.name) { mutableStateOf(false) }
    val context = LocalContext.current
    val licenseText = remember(expanded) {
        if (!expanded) {
            null
        } else {
            context.resources.openRawResource(kind.textRes)
                .bufferedReader()
                .use { it.readText() }
        }
    }
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(vertical = 4.dp)) {
            ListItem(
                headlineContent = {
                    Text(stringResource(kind.labelRes), fontWeight = FontWeight.SemiBold)
                },
                supportingContent = {
                    Text(pluralStringResource(R.plurals.licenses_library_count, components.size, components.size))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                components.forEach { component ->
                    Column {
                        Text(component.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            component.copyright,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(
                        if (expanded) R.string.licenses_hide_text else R.string.licenses_show_text,
                    ),
                )
            }
            if (licenseText != null) {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Text(
                    licenseText,
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
