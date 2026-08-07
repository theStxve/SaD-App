package com.sad.app.ui

import android.graphics.*
import com.sad.app.data.PlayerProfile

enum class CardLayoutMode(val displayName: String) {
    CARD_ID("ID-Karte (Quer)"),
    STATS_PANEL("Stats-Panel (Hoch)")
}

enum class CardTheme(
    val displayName: String,
    val bgColor: Int,
    val surfaceColor: Int,
    val primaryColor: Int,
    val accentColor: Int,
    val textColor: Int,
    val subTextColor: Int
) {
    CYBER(
        "Cyberpunk",
        Color.parseColor("#070A12"),
        Color.parseColor("#0D1020"),
        Color.parseColor("#00F3FF"),
        Color.parseColor("#FF00E6"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#7A8FA8")
    ),
    INFERNO(
        "Inferno",
        Color.parseColor("#0E0600"),
        Color.parseColor("#1A0C00"),
        Color.parseColor("#FF6200"),
        Color.parseColor("#FFB300"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#A07050")
    ),
    MATRIX(
        "Matrix",
        Color.parseColor("#000D00"),
        Color.parseColor("#001500"),
        Color.parseColor("#00FF41"),
        Color.parseColor("#88FF00"),
        Color.parseColor("#CCFFCC"),
        Color.parseColor("#3A7A3A")
    ),
    MIDNIGHT(
        "Midnight",
        Color.parseColor("#0A0D18"),
        Color.parseColor("#131828"),
        Color.parseColor("#8A6FDF"),
        Color.parseColor("#F78C6C"),
        Color.parseColor("#F0F6FC"),
        Color.parseColor("#6E7C9A")
    ),
    LIGHT(
        "Light",
        Color.parseColor("#E8EEFF"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#1A5FCC"),
        Color.parseColor("#CC0066"),
        Color.parseColor("#0A0A1A"),
        Color.parseColor("#445580")
    )
}

object ProfileCardRenderer {

    fun generateCard(
        profile: PlayerProfile,
        layoutMode: CardLayoutMode,
        cardTheme: CardTheme
    ): Bitmap {
        return when (layoutMode) {
            CardLayoutMode.CARD_ID -> drawIdCard(profile, cardTheme)
            CardLayoutMode.STATS_PANEL -> drawStatsPanel(profile, cardTheme)
        }
    }

    private fun drawIdCard(profile: PlayerProfile, theme: CardTheme): Bitmap {
        val W = 1400
        val H = 700
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val M = 36f  // card margin

        // ── Background ──────────────────────────────────────────────────────
        c.drawColor(theme.bgColor)

        // Subtle diagonal grid texture
        val gridP = Paint().apply { color = theme.primaryColor; alpha = 12; strokeWidth = 1.2f }
        var gx = 0f
        while (gx < W) { c.drawLine(gx, 0f, gx - H, H.toFloat(), gridP); gx += 50f }

        // Card surface
        val cardRect = RectF(M, M, W - M, H - M)
        val cardP = Paint().apply { color = theme.surfaceColor; style = Paint.Style.FILL; isAntiAlias = true }
        c.drawRoundRect(cardRect, 20f, 20f, cardP)

        // Card border with glow
        val borderP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 3f; isAntiAlias = true
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawRoundRect(cardRect, 20f, 20f, borderP)
        borderP.maskFilter = null; borderP.alpha = 160
        c.drawRoundRect(cardRect, 20f, 20f, borderP)

        // Tech Corner Accents (Tough Cyber-Look)
        drawTechCorners(c, cardRect, theme.primaryColor, 25f, 4f)

        // Left accent bar
        val accentBarP = Paint().apply { color = theme.accentColor; alpha = 140; isAntiAlias = true }
        c.drawRoundRect(RectF(M + 2f, M + 20f, M + 8f, H - M - 20f), 4f, 4f, accentBarP)

        // ── Header strip ────────────────────────────────────────────────────
        val headerH = 70f
        val headerP = Paint().apply { color = theme.primaryColor; alpha = 24 }
        c.drawRoundRect(RectF(M + 12f, M + 10f, W - M - 12f, M + headerH), 12f, 12f, headerP)

        val headerBorderP = Paint().apply {
            color = theme.primaryColor; alpha = 80
            style = Paint.Style.STROKE; strokeWidth = 1.5f
        }
        c.drawLine(M + 20f, M + headerH, W - M - 20f, M + headerH, headerBorderP)

        val headerTextP = Paint().apply {
            color = theme.primaryColor; textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true; letterSpacing = 0.18f
        }
        c.drawText("CITY AS A DUNGEON  //  OPERATIVE IDENTITY CARD", M + 30f, M + 44f, headerTextP)

        // Version tag & Online Dot right side
        headerTextP.textSize = 17f; headerTextP.alpha = 180
        headerTextP.textAlign = Paint.Align.RIGHT
        c.drawText("STATUS: ONLINE  ●  v1.0.4", W - M - 30f, M + 44f, headerTextP)
        headerTextP.textAlign = Paint.Align.LEFT

        // ── Avatar area (left column) ────────────────────────────────────────
        val avatarCX = M + 160f
        val avatarCY = M + headerH + ((H - M - headerH - M) / 2f)
        val avatarR = 105f

        // Outer glow ring
        val glowP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 4f; isAntiAlias = true
            maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 10f, glowP)

        // Dashed orbit ring
        val orbitP = Paint().apply {
            color = theme.accentColor; alpha = 140
            style = Paint.Style.STROKE; strokeWidth = 2f
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 22f, orbitP)

        // Avatar circle fill
        val avatarBgP = Paint().apply { color = theme.primaryColor; alpha = 30; isAntiAlias = true }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBgP)

        val avatarBorderP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 3.5f; isAntiAlias = true
        }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBorderP)

        // Initials inside avatar
        val initials = if (profile.playerName.isNotBlank()) profile.playerName.take(2).uppercase() else "SD"
        val initialsP = Paint().apply {
            color = theme.textColor; textSize = 78f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        c.drawText(initials, avatarCX, avatarCY + 27f, initialsP)

        // ── Main Info area (right of avatar) ────────────────────────────────
        val leftX = avatarCX + avatarR + 55f

        // Operative Name
        val nameP = Paint().apply {
            color = theme.textColor; textSize = 54f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        c.drawText(profile.displayName, leftX, M + headerH + 70f, nameP)

        // Title Badge
        val titleText = "${profile.title.uppercase()}  ·  LEVEL ${profile.level}"
        val titleP = Paint().apply {
            color = theme.accentColor; textSize = 22f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true; letterSpacing = 0.1f
        }
        c.drawText(titleText, leftX, M + headerH + 112f, titleP)

        // Divider
        val divP = Paint().apply { color = theme.primaryColor; alpha = 45; strokeWidth = 1.2f }
        c.drawLine(leftX, M + headerH + 130f, W - M - 40f, M + headerH + 130f, divP)

        // ── XP Progress Bar ─────────────────────────────────────────────────
        val xpY = M + headerH + 155f
        val xpForLevel = (profile.level - 1) * 500
        val xpProgress = ((profile.xp - xpForLevel).toFloat() / 500f).coerceIn(0f, 1f)

        val xpTextP = Paint().apply {
            color = theme.subTextColor; textSize = 17f
            typeface = Typeface.MONOSPACE; isAntiAlias = true; letterSpacing = 0.08f
        }
        c.drawText("XP PROGRESS: ${profile.xp} / ${profile.level * 500}", leftX, xpY, xpTextP)

        val barLeft = leftX
        val barRight = W - M - 50f
        val barTop = xpY + 14f
        val barBottom = barTop + 14f

        val barTrackP = Paint().apply { color = theme.primaryColor; alpha = 30; isAntiAlias = true }
        c.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 6f, 6f, barTrackP)

        if (xpProgress > 0f) {
            val barFillRight = barLeft + (barRight - barLeft) * xpProgress
            val barFillP = Paint().apply { color = theme.primaryColor; isAntiAlias = true }
            c.drawRoundRect(RectF(barLeft, barTop, barFillRight, barBottom), 6f, 6f, barFillP)

            val barGlowP = Paint().apply {
                color = theme.primaryColor; style = Paint.Style.STROKE
                strokeWidth = 3f; isAntiAlias = true
                maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
            }
            c.drawRoundRect(RectF(barLeft, barTop, barFillRight, barBottom), 6f, 6f, barGlowP)
        }

        // ── Stats 2x2 Grid ───────────────────────────────────────────────────
        val gridTop = barBottom + 35f
        val cellW = (W - M - 50f - leftX - 24f) / 2f
        val cellH = 110f

        val statsList = listOf(
            Triple("SEKTOREN", "${profile.exploredCount}", theme.primaryColor),
            Triple("DUNGEONS", "${profile.visitedDungeons}", theme.accentColor),
            Triple("NIGHT OWL", "${profile.nightExploredCount}", theme.primaryColor),
            Triple("ERFOLGE", "${profile.unlockedAchievements.size}", theme.accentColor)
        )

        val cellBgP = Paint().apply { color = theme.primaryColor; alpha = 20; isAntiAlias = true }
        val cellBorderP = Paint().apply {
            color = theme.primaryColor; alpha = 75
            style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true
        }
        val valP = Paint().apply {
            textSize = 44f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        val lblP = Paint().apply {
            color = theme.subTextColor; textSize = 15f
            typeface = Typeface.MONOSPACE; isAntiAlias = true; letterSpacing = 0.1f
        }

        statsList.forEachIndexed { idx, (label, value, col) ->
            val row = idx / 2
            val colIdx = idx % 2
            val cL = leftX + colIdx * (cellW + 24f)
            val cT = gridTop + row * (cellH + 18f)
            val rect = RectF(cL, cT, cL + cellW, cT + cellH)

            c.drawRoundRect(rect, 10f, 10f, cellBgP)
            c.drawRoundRect(rect, 10f, 10f, cellBorderP)

            valP.color = col
            c.drawText(value, cL + 20f, cT + 50f, valP)
            c.drawText(label, cL + 20f, cT + 85f, lblP)
        }

        // Footer water mark
        val footerP = Paint().apply {
            color = theme.subTextColor; textSize = 14f
            typeface = Typeface.MONOSPACE; textAlign = Paint.Align.RIGHT
            isAntiAlias = true; alpha = 90
        }
        c.drawText("SAD // SYSTEM ID: ${profile.playerName.hashCode()}", W - M - 20f, H - M - 15f, footerP)

        return bmp
    }

    private fun drawStatsPanel(profile: PlayerProfile, theme: CardTheme): Bitmap {
        val W = 1000
        val H = 1400
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val M = 40f

        // ── Background ──────────────────────────────────────────────────────
        c.drawColor(theme.bgColor)

        // Diagonal grid
        val gridP = Paint().apply { color = theme.primaryColor; alpha = 12; strokeWidth = 1.2f }
        var gx = 0f
        while (gx < W + H) { c.drawLine(gx, 0f, gx - H, H.toFloat(), gridP); gx += 50f }

        // Card surface
        val cardRect = RectF(M, M, W - M, H - M)
        val cardP = Paint().apply { color = theme.surfaceColor; style = Paint.Style.FILL; isAntiAlias = true }
        c.drawRoundRect(cardRect, 24f, 24f, cardP)

        // Card border with glow
        val borderGlowP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 3f; isAntiAlias = true
            maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawRoundRect(cardRect, 24f, 24f, borderGlowP)
        borderGlowP.maskFilter = null; borderGlowP.alpha = 160
        c.drawRoundRect(cardRect, 24f, 24f, borderGlowP)

        // Tech Corner Accents
        drawTechCorners(c, cardRect, theme.primaryColor, 30f, 4.5f)

        // Top accent bar
        val topAccentP = Paint().apply { color = theme.accentColor; alpha = 120 }
        c.drawRoundRect(RectF(M + 20f, M + 4f, W - M - 20f, M + 10f), 4f, 4f, topAccentP)

        // ── Header ──────────────────────────────────────────────────────────
        val headerH = 75f
        val headerBgP = Paint().apply { color = theme.primaryColor; alpha = 24 }
        c.drawRoundRect(RectF(M + 12f, M + 12f, W - M - 12f, M + headerH), 12f, 12f, headerBgP)

        val headerDivP = Paint().apply { color = theme.primaryColor; alpha = 70; strokeWidth = 1.2f }
        c.drawLine(M + 30f, M + headerH, W - M - 30f, M + headerH, headerDivP)

        val headerTextP = Paint().apply {
            color = theme.primaryColor; textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER; isAntiAlias = true; letterSpacing = 0.2f
        }
        c.drawText("CITY AS A DUNGEON  //  OPERATIVE PROFILE", W / 2f, M + 47f, headerTextP)

        // ── Avatar ──────────────────────────────────────────────────────────
        val avatarCX = W / 2f
        val avatarCY = M + headerH + 145f
        val avatarR = 115f

        // Glow rings
        val outerGlowP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 3f; isAntiAlias = true
            maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 15f, outerGlowP)

        val orbitP = Paint().apply {
            color = theme.accentColor; alpha = 140
            style = Paint.Style.STROKE; strokeWidth = 2f
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 28f, orbitP)

        // Avatar fill
        val avatarBgP = Paint().apply { color = theme.primaryColor; alpha = 30; isAntiAlias = true }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBgP)

        val avatarBorderP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 4f; isAntiAlias = true
        }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBorderP)

        // Initials
        val initials = if (profile.playerName.isNotBlank()) profile.playerName.take(2).uppercase() else "SD"
        val initialsGlowP = Paint().apply {
            color = theme.textColor; textSize = 88f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        c.drawText(initials, avatarCX, avatarCY + 30f, initialsGlowP)

        // ── Name & Title ─────────────────────────────────────────────────────
        var y = avatarCY + avatarR + 55f

        val nameP = Paint().apply {
            color = theme.textColor; textSize = 62f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        c.drawText(profile.displayName, avatarCX, y, nameP)
        y += 50f

        // Horizontal rule under name
        val hrP = Paint().apply { color = theme.primaryColor; alpha = 50; strokeWidth = 1.2f }
        c.drawLine(M + 80f, y - 8f, W - M - 80f, y - 8f, hrP)

        val titleP = Paint().apply {
            color = theme.accentColor; textSize = 28f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true; letterSpacing = 0.1f
        }
        c.drawText("${profile.title.uppercase()}  ·  LEVEL ${profile.level}", avatarCX, y + 20f, titleP)
        y += 80f

        // ── XP Bar ───────────────────────────────────────────────────────────
        val xpForLevel = (profile.level - 1) * 500
        val xpProgress = ((profile.xp - xpForLevel).toFloat() / 500f).coerceIn(0f, 1f)
        val barLeft = M + 80f
        val barRight = W - M - 80f

        val xpLblP = Paint().apply {
            color = theme.subTextColor; textSize = 20f
            typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER; isAntiAlias = true; letterSpacing = 0.08f
        }
        c.drawText("XP PROGRESS: ${profile.xp} / ${profile.level * 500}", avatarCX, y, xpLblP)
        y += 18f

        val barTrackP = Paint().apply { color = theme.primaryColor; alpha = 30; isAntiAlias = true }
        c.drawRoundRect(RectF(barLeft, y, barRight, y + 14f), 7f, 7f, barTrackP)

        if (xpProgress > 0f) {
            val barFill = barLeft + (barRight - barLeft) * xpProgress
            val barFillP = Paint().apply { color = theme.primaryColor; isAntiAlias = true }
            c.drawRoundRect(RectF(barLeft, y, barFill, y + 14f), 7f, 7f, barFillP)
            barFillP.maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
            c.drawRoundRect(RectF(barLeft, y, barFill, y + 14f), 7f, 7f, barFillP)
        }
        y += 60f

        // ── Stats 2x2 Grid ───────────────────────────────────────────────────
        val divP2 = Paint().apply { color = theme.primaryColor; alpha = 45; strokeWidth = 1.2f }
        c.drawLine(M + 60f, y - 10f, W - M - 60f, y - 10f, divP2)

        val gridMargin = M + 55f
        val cellW = (W - gridMargin * 2 - 30f) / 2f
        val cellH = 160f

        val statsGrid = listOf(
            Triple("ERKUNDETE\nSEKTOREN", "${profile.exploredCount}", theme.primaryColor),
            Triple("BEZWUNGENE\nDUNGEONS", "${profile.visitedDungeons}", theme.accentColor),
            Triple("NIGHT OWL\nEXPLO", "${profile.nightExploredCount}", theme.primaryColor),
            Triple("FREI-\nGESCHALTET", "${profile.unlockedAchievements.size}", theme.accentColor)
        )

        val cellBgP = Paint().apply { color = theme.primaryColor; alpha = 20; isAntiAlias = true }
        val cellBorderP = Paint().apply {
            color = theme.primaryColor; alpha = 75
            style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true
        }
        val cellValP = Paint().apply {
            textSize = 62f; typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        val cellLblP = Paint().apply {
            color = theme.subTextColor; textSize = 18f
            typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER
            isAntiAlias = true; letterSpacing = 0.1f
        }

        statsGrid.forEachIndexed { idx, (lbl, value, col) ->
            val row = idx / 2; val col2 = idx % 2
            val cLeft = gridMargin + col2 * (cellW + 30f)
            val cTop = y + row * (cellH + 24f)
            val cRect = RectF(cLeft, cTop, cLeft + cellW, cTop + cellH)

            c.drawRoundRect(cRect, 14f, 14f, cellBgP)
            c.drawRoundRect(cRect, 14f, 14f, cellBorderP)

            cellValP.color = col
            c.drawText(value, cLeft + cellW / 2f, cTop + 75f, cellValP)

            // Multi-line label
            val lines = lbl.split("\n")
            lines.forEachIndexed { li, line ->
                c.drawText(line, cLeft + cellW / 2f, cTop + 108f + li * 22f, cellLblP)
            }
        }

        // ── Footer ───────────────────────────────────────────────────────────
        val footP = Paint().apply {
            color = theme.subTextColor; textSize = 20f
            typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER
            isAntiAlias = true; alpha = 100
        }
        c.drawText("SAD // CITY AS A DUNGEON  ·  OPERATIVE PROFILE", W / 2f, H - M - 20f, footP)

        return bmp
    }

    private fun drawTechCorners(c: Canvas, rect: RectF, color: Int, length: Float, stroke: Float) {
        val p = Paint().apply {
            this.color = color; style = Paint.Style.STROKE
            strokeWidth = stroke; isAntiAlias = true; alpha = 230
        }

        // Top-Left Corner
        c.drawLine(rect.left, rect.top, rect.left + length, rect.top, p)
        c.drawLine(rect.left, rect.top, rect.left, rect.top + length, p)

        // Top-Right Corner
        c.drawLine(rect.right, rect.top, rect.right - length, rect.top, p)
        c.drawLine(rect.right, rect.top, rect.right, rect.top + length, p)

        // Bottom-Left Corner
        c.drawLine(rect.left, rect.bottom, rect.left + length, rect.bottom, p)
        c.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - length, p)

        // Bottom-Right Corner
        c.drawLine(rect.right, rect.bottom, rect.right - length, rect.bottom, p)
        c.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - length, p)
    }
}
