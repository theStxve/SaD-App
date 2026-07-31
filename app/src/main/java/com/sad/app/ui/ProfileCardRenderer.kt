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
        val gridP = Paint().apply { color = theme.primaryColor; alpha = 8; strokeWidth = 1f }
        var gx = 0f
        while (gx < W) { c.drawLine(gx, 0f, gx - H, H.toFloat(), gridP); gx += 60f }

        // Card surface
        val cardRect = RectF(M, M, W - M, H - M)
        val cardP = Paint().apply { color = theme.surfaceColor; style = Paint.Style.FILL; isAntiAlias = true }
        c.drawRoundRect(cardRect, 20f, 20f, cardP)

        // Card border with glow
        val borderP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 2.5f; isAntiAlias = true
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawRoundRect(cardRect, 20f, 20f, borderP)
        borderP.maskFilter = null; borderP.alpha = 120
        c.drawRoundRect(cardRect, 20f, 20f, borderP)

        // Left accent bar
        val accentBarP = Paint().apply { color = theme.primaryColor; alpha = 60 }
        c.drawRoundRect(RectF(M, M, M + 5f, H - M), 4f, 4f, accentBarP)

        // ── Header strip ────────────────────────────────────────────────────
        val headerH = 70f
        val headerP = Paint().apply { color = theme.primaryColor; alpha = 18 }
        c.drawRoundRect(RectF(M, M, W - M, M + headerH), 20f, 4f, headerP)

        val headerBorderP = Paint().apply {
            color = theme.primaryColor; alpha = 50
            style = Paint.Style.STROKE; strokeWidth = 1f
        }
        c.drawLine(M + 20f, M + headerH, W - M - 20f, M + headerH, headerBorderP)

        val headerTextP = Paint().apply {
            color = theme.primaryColor; textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true; letterSpacing = 0.15f
        }
        c.drawText("CITY AS A DUNGEON  //  OPERATIVE IDENTITY CARD", M + 30f, M + 44f, headerTextP)

        // Version tag right side
        headerTextP.textSize = 18f; headerTextP.alpha = 100
        headerTextP.textAlign = Paint.Align.RIGHT
        c.drawText("v1.0", W - M - 30f, M + 44f, headerTextP)
        headerTextP.textAlign = Paint.Align.LEFT

        // ── Avatar area (left column) ────────────────────────────────────────
        val avatarCX = M + 160f
        val avatarCY = M + headerH + ((H - M - headerH - M) / 2f)
        val avatarR = 105f

        // Outer glow ring
        val glowP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 3f; isAntiAlias = true
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 10f, glowP)

        // Dashed orbit ring
        val orbitP = Paint().apply {
            color = theme.primaryColor; alpha = 60
            style = Paint.Style.STROKE; strokeWidth = 1.5f
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 20f, orbitP)

        // Avatar circle fill
        val avatarBgP = Paint().apply {
            color = theme.primaryColor; alpha = 25; isAntiAlias = true
        }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBgP)

        // Avatar border
        val avatarBorderP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 3f; isAntiAlias = true
        }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBorderP)

        // Initials
        val initials = if (profile.playerName.isNotBlank()) profile.playerName.take(2).uppercase() else "SD"
        val initialsP = Paint().apply {
            color = theme.primaryColor; textSize = 72f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawText(initials, avatarCX, avatarCY + 25f, initialsP)
        initialsP.maskFilter = null
        c.drawText(initials, avatarCX, avatarCY + 25f, initialsP)

        // ── Info column (right of avatar) ────────────────────────────────────
        val infoLeft = avatarCX + avatarR + 60f
        val infoRight = W - M - 40f
        var infoY = M + headerH + 55f

        // Player name
        val nameP = Paint().apply {
            color = theme.textColor; textSize = 64f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        c.drawText(profile.displayName, infoLeft, infoY, nameP)
        infoY += 52f

        // Divider line under name
        val divP = Paint().apply { color = theme.primaryColor; alpha = 40; strokeWidth = 1f }
        c.drawLine(infoLeft, infoY, infoLeft + 380f, infoY, divP)
        infoY += 22f

        // Title row
        val titleLblP = Paint().apply {
            color = theme.subTextColor; textSize = 18f
            typeface = Typeface.MONOSPACE; isAntiAlias = true; letterSpacing = 0.1f
        }
        c.drawText("TITEL", infoLeft, infoY, titleLblP)
        val titleValP = Paint().apply {
            color = theme.accentColor; textSize = 22f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true; letterSpacing = 0.05f
        }
        c.drawText(profile.title.uppercase(), infoLeft + 80f, infoY, titleValP)
        infoY += 36f

        // Level row
        c.drawText("LEVEL", infoLeft, infoY, titleLblP)
        val levelValP = Paint().apply {
            color = theme.primaryColor; textSize = 22f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        c.drawText("${profile.level}", infoLeft + 80f, infoY, levelValP)
        infoY += 48f

        // XP bar label
        val xpForLevel = (profile.level - 1) * 500
        val xpProgress = ((profile.xp - xpForLevel).toFloat() / 500f).coerceIn(0f, 1f)
        val xpLblP = Paint().apply {
            color = theme.subTextColor; textSize = 18f; typeface = Typeface.MONOSPACE; isAntiAlias = true
        }
        c.drawText("XP  ${profile.xp} / ${profile.level * 500}", infoLeft, infoY, xpLblP)
        infoY += 16f

        // XP bar track
        val barW = (infoRight - infoLeft).coerceAtLeast(100f)
        val barH2 = 10f
        val barTrackP = Paint().apply { color = theme.primaryColor; alpha = 25; isAntiAlias = true }
        c.drawRoundRect(RectF(infoLeft, infoY, infoLeft + barW, infoY + barH2), 6f, 6f, barTrackP)

        // XP bar fill
        val barFillP = Paint().apply { color = theme.primaryColor; isAntiAlias = true }
        if (xpProgress > 0f) {
            c.drawRoundRect(RectF(infoLeft, infoY, infoLeft + barW * xpProgress, infoY + barH2), 6f, 6f, barFillP)
        }

        // XP bar glow
        barFillP.maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.OUTER)
        if (xpProgress > 0f) {
            c.drawRoundRect(RectF(infoLeft, infoY, infoLeft + barW * xpProgress, infoY + barH2), 6f, 6f, barFillP)
        }

        // ── Stats row at bottom ─────────────────────────────────────────────
        val statsTop = H - M - 140f
        val statsLeft = M + 30f
        val statsRight = W - M - 30f
        val statsW = (statsRight - statsLeft - 40f) / 3f

        // Stats top separator
        val sepP = Paint().apply { color = theme.primaryColor; alpha = 30; strokeWidth = 1f }
        c.drawLine(statsLeft, statsTop - 10f, statsRight, statsTop - 10f, sepP)

        val stats = listOf(
            Triple("SEKTOREN", "${profile.exploredCount}", theme.primaryColor),
            Triple("DUNGEONS", "${profile.visitedDungeons}", theme.accentColor),
            Triple("ACHIEVEMENTS", "${profile.unlockedAchievements.size}", theme.primaryColor)
        )

        val statValP2 = Paint().apply { textSize = 52f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val statLblP2 = Paint().apply { color = theme.subTextColor; textSize = 18f; typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER; isAntiAlias = true; letterSpacing = 0.08f }

        stats.forEachIndexed { i, (lbl, value, col) ->
            val cx = statsLeft + i * (statsW + 20f) + statsW / 2f

            // Stat box bg
            val sbP = Paint().apply { color = theme.primaryColor; alpha = 12 }
            c.drawRoundRect(RectF(cx - statsW / 2f, statsTop, cx + statsW / 2f, statsTop + 105f), 10f, 10f, sbP)

            // Stat value
            statValP2.color = col
            c.drawText(value, cx, statsTop + 60f, statValP2)

            // Stat label
            c.drawText(lbl, cx, statsTop + 88f, statLblP2)
        }

        // ── Footer watermark ────────────────────────────────────────────────
        val footP = Paint().apply {
            color = theme.subTextColor; textSize = 18f; typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT; isAntiAlias = true; alpha = 80
        }
        c.drawText("SAD // CITY AS A DUNGEON OS", W - M - 20f, H - M - 12f, footP)

        return bmp
    }

    private fun drawStatsPanel(profile: PlayerProfile, theme: CardTheme): Bitmap {
        val W = 1080
        val H = 1440
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val M = 40f

        // ── Background ──────────────────────────────────────────────────────
        c.drawColor(theme.bgColor)

        // Diagonal grid
        val gridP = Paint().apply { color = theme.primaryColor; alpha = 8; strokeWidth = 1f }
        var gx = 0f
        while (gx < W + H) { c.drawLine(gx, 0f, gx - H, H.toFloat(), gridP); gx += 60f }

        // Card surface
        val cardRect = RectF(M, M, W - M, H - M)
        val cardP = Paint().apply { color = theme.surfaceColor; style = Paint.Style.FILL; isAntiAlias = true }
        c.drawRoundRect(cardRect, 24f, 24f, cardP)

        // Card border with glow
        val borderGlowP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 2f; isAntiAlias = true
            maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawRoundRect(cardRect, 24f, 24f, borderGlowP)
        borderGlowP.maskFilter = null; borderGlowP.alpha = 100
        c.drawRoundRect(cardRect, 24f, 24f, borderGlowP)

        // Top accent bar
        val topAccentP = Paint().apply { color = theme.primaryColor; alpha = 50 }
        c.drawRoundRect(RectF(M, M, W - M, M + 5f), 24f, 4f, topAccentP)

        // ── Header ──────────────────────────────────────────────────────────
        val headerH = 75f
        val headerBgP = Paint().apply { color = theme.primaryColor; alpha = 15 }
        c.drawRoundRect(RectF(M, M, W - M, M + headerH), 24f, 4f, headerBgP)

        val headerDivP = Paint().apply { color = theme.primaryColor; alpha = 45; strokeWidth = 1f }
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
            strokeWidth = 2f; isAntiAlias = true
            maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 15f, outerGlowP)

        val orbitP = Paint().apply {
            color = theme.primaryColor; alpha = 50
            style = Paint.Style.STROKE; strokeWidth = 1.5f
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        }
        c.drawCircle(avatarCX, avatarCY, avatarR + 28f, orbitP)

        // Avatar fill
        val avatarBgP = Paint().apply { color = theme.primaryColor; alpha = 22; isAntiAlias = true }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBgP)

        val avatarBorderP = Paint().apply {
            color = theme.primaryColor; style = Paint.Style.STROKE
            strokeWidth = 3.5f; isAntiAlias = true
        }
        c.drawCircle(avatarCX, avatarCY, avatarR, avatarBorderP)

        // Initials
        val initials = if (profile.playerName.isNotBlank()) profile.playerName.take(2).uppercase() else "SD"
        val initialsGlowP = Paint().apply {
            color = theme.primaryColor; textSize = 84f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true
            maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
        }
        c.drawText(initials, avatarCX, avatarCY + 29f, initialsGlowP)
        initialsGlowP.maskFilter = null
        c.drawText(initials, avatarCX, avatarCY + 29f, initialsGlowP)

        // ── Name & Title ─────────────────────────────────────────────────────
        var y = avatarCY + avatarR + 55f

        val nameP = Paint().apply {
            color = theme.textColor; textSize = 62f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        c.drawText(profile.displayName, avatarCX, y, nameP)
        y += 50f

        // Horizontal rule under name
        val hrP = Paint().apply { color = theme.primaryColor; alpha = 35; strokeWidth = 1f }
        c.drawLine(M + 80f, y - 8f, W - M - 80f, y - 8f, hrP)

        val titleP = Paint().apply {
            color = theme.accentColor; textSize = 28f
            typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; isAntiAlias = true; letterSpacing = 0.08f
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
            typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        c.drawText("XP  ${profile.xp}  /  ${profile.level * 500}", avatarCX, y, xpLblP)
        y += 18f

        val barTrackP = Paint().apply { color = theme.primaryColor; alpha = 22; isAntiAlias = true }
        c.drawRoundRect(RectF(barLeft, y, barRight, y + 12f), 6f, 6f, barTrackP)

        if (xpProgress > 0f) {
            val barFill = barLeft + (barRight - barLeft) * xpProgress
            val barFillP = Paint().apply { color = theme.primaryColor; isAntiAlias = true }
            c.drawRoundRect(RectF(barLeft, y, barFill, y + 12f), 6f, 6f, barFillP)
            barFillP.maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.OUTER)
            c.drawRoundRect(RectF(barLeft, y, barFill, y + 12f), 6f, 6f, barFillP)
        }
        y += 60f

        // ── Stats 2x2 Grid ───────────────────────────────────────────────────
        val divP2 = Paint().apply { color = theme.primaryColor; alpha = 28; strokeWidth = 1f }
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

        val cellBgP = Paint().apply { color = theme.primaryColor; alpha = 15 }
        val cellBorderP = Paint().apply {
            color = theme.primaryColor; alpha = 60
            style = Paint.Style.STROKE; strokeWidth = 1.5f; isAntiAlias = true
        }
        val cellValP = Paint().apply {
            textSize = 60f; typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER; isAntiAlias = true
        }
        val cellLblP = Paint().apply {
            color = theme.subTextColor; textSize = 18f
            typeface = Typeface.MONOSPACE; textAlign = Paint.Align.CENTER
            isAntiAlias = true; letterSpacing = 0.08f
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
            isAntiAlias = true; alpha = 80
        }
        c.drawText("SAD // CITY AS A DUNGEON", W / 2f, H - M - 20f, footP)

        return bmp
    }
}
