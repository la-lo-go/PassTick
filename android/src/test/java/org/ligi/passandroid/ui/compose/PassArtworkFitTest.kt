package org.ligi.passandroid.ui.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.ligi.passandroid.repository.PassArtworkKind

class PassArtworkFitTest {
    @Test
    fun `detail and picker keep brand artwork fully visible`() {
        assertThat(PassArtworkKind.ICON.artworkFit()).isEqualTo(PassArtworkFit.CONTAIN)
        assertThat(PassArtworkKind.LOGO.artworkFit()).isEqualTo(PassArtworkFit.CONTAIN)
        assertThat(PassArtworkFit.CONTAIN.fillFraction()).isLessThan(1f)
    }

    @Test
    fun `home square brand artwork fills its thumbnail`() {
        assertThat(PassArtworkKind.ICON.artworkFit(PassArtworkContext.HOME_THUMBNAIL, 1f)).isEqualTo(PassArtworkFit.COVER)
        assertThat(PassArtworkKind.LOGO.artworkFit(PassArtworkContext.HOME_THUMBNAIL, 100f / 90f)).isEqualTo(PassArtworkFit.COVER)
        assertThat(PassArtworkFit.COVER.fillFraction()).isEqualTo(1f)
    }

    @Test
    fun `home wide brand artwork remains fully visible`() {
        assertThat(PassArtworkKind.ICON.artworkFit(PassArtworkContext.HOME_THUMBNAIL, 100f / 89f)).isEqualTo(PassArtworkFit.CONTAIN)
    }

    @Test
    fun `photographic artwork fills its container`() {
        assertThat(PassArtworkKind.STRIP.artworkFit()).isEqualTo(PassArtworkFit.COVER)
        assertThat(PassArtworkKind.THUMBNAIL.artworkFit()).isEqualTo(PassArtworkFit.COVER)
        assertThat(PassArtworkKind.FOOTER.artworkFit()).isEqualTo(PassArtworkFit.COVER)
    }
}
