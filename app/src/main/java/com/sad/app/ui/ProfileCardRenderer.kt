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
        Color.parseColor("#0A0A12"),
        Color.parseColor("#111122"),
        Color.parseColor("#00F3FF"),
        Color.parseColor("#FF00E6"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#8899AA")
    ),
    INFERNO(
        "Inferno",
        Color.parseColor("#120600"),
        Color.parseColor("#1E0C00"),
        Color.parseColor("#FF6200"),
        Color.parseColor("#FFCC00"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#AA7755")
    ),
    MATRIX(
        "Matrix",
        Color.parseColor("#000D00"),
        Color.parseColor("#001A00"),
        Color.parseColor("#00FF41"),
        Color.parseColor("#88FF00"),
        Color.parseColor("#CCFFCC"),
        Color.parseColor("#448844")
    ),
    MIDNIGHT(
        "Midnight",
        Color.parseColor("#0D1117"),
        Color.parseColor("#161B22"),
        Color.parseColor("#7952B3"),
        Color.parseColor("#F78C6C"),
        Color.parseColor("#F0F6FC"),
        Color.parseColor("#8B949E")
    ),
    LIGHT(
        "Light",
        Color.parseColor("#F0F4FF"),
        Color.parseColor("#FFFFFF"),
        Color.parseColor("#0066DD"),
        Color.parseColor("#CC0066"),
        Color.parseColor("#0D0D1A"),
        Color.parseColor("#556080")
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
        val width = 1200
        val height = 675
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Hintergründe
        val bgPaint = Paint().apply { color = theme.bgColor; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Karten-Container (Hauptkarte)
        val cardMargin = 40f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardPaint = Paint().apply { color = theme.surfaceColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)

        val borderPaint = Paint().apply {
            color = theme.primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 24f, 24f, borderPaint)

        // Subtiles Gitternetz-Hintergrundmuster
        val gridPaint = Paint().apply {
            color = theme.primaryColor
            alpha = 15
            strokeWidth = 1f
        }
        var x = cardMargin
        while (x < width - cardMargin) {
            canvas.drawLine(x, cardMargin, x, height - cardMargin, gridPaint)
            x += 40f
        }
        var y = cardMargin
        while (y < height - cardMargin) {
            canvas.drawLine(cardMargin, y, width - cardMargin, y, gridPaint)
            y += 40f
        }

        // Header-Balken oben
        val headerPaint = Paint().apply { color = theme.primaryColor; alpha = 30 }
        canvas.drawRoundRect(RectF(cardMargin + 20f, cardMargin + 20f, width - cardMargin - 20f, cardMargin + 90f), 12f, 12f, headerPaint)

        val titlePaint = Paint().apply {
            color = theme.primaryColor
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("CITY AS A DUNGEON // OPERATIVE ID", cardMargin + 40f, cardMargin + 62f, titlePaint)

        // Avatar-Kreis links
        val avatarRadius = 100f
        val avatarCenterX = cardMargin + 140f
        val avatarCenterY = cardMargin + 260f

        val avatarBgPaint = Paint().apply { color = theme.primaryColor; alpha = 40; isAntiAlias = true }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBgPaint)

        val avatarBorderPaint = Paint().apply {
            color = theme.primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBorderPaint)

        // Avatar-Initialen oder Symbol
        val initials = if (profile.playerName.isNotBlank()) {
            profile.playerName.take(2).uppercase()
        } else {
            "SAD"
        }
        val initialsPaint = Paint().apply {
            color = theme.primaryColor
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(initials, avatarCenterX, avatarCenterY + 22f, initialsPaint)

        // Spieler Name + Titel rechts vom Avatar
        val textLeft = avatarCenterX + avatarRadius + 50f
        var textY = cardMargin + 210f

        val namePaint = Paint().apply {
            color = theme.textColor
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(profile.displayName, textLeft, textY, namePaint)

        textY += 45f
        val rankPaint = Paint().apply {
            color = theme.accentColor
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("TITEL: ${profile.title.uppercase()}", textLeft, textY, rankPaint)

        // Level Badge
        val levelText = "LVL ${profile.level}"
        val levelPaint = Paint().apply {
            color = theme.primaryColor
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(levelText, textLeft, textY + 45f, levelPaint)

        // XP-Fortschrittsbalken
        val barTop = cardMargin + 370f
        val barLeft = cardMargin + 40f
        val barRight = width - cardMargin - 40f
        val barHeight = 16f

        val barBgPaint = Paint().apply { color = theme.primaryColor; alpha = 30 }
        canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barTop + barHeight), 8f, 8f, barBgPaint)

        val xpForThisLevel = (profile.level - 1) * 500
        val xpProgress = ((profile.xp - xpForThisLevel).toFloat() / 500f).coerceIn(0f, 1f)
        val barFillRight = barLeft + (barRight - barLeft) * xpProgress

        val barFillPaint = Paint().apply { color = theme.primaryColor; isAntiAlias = true }
        canvas.drawRoundRect(RectF(barLeft, barTop, barFillRight, barTop + barHeight), 8f, 8f, barFillPaint)

        val xpLabelPaint = Paint().apply {
            color = theme.subTextColor
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText("XP: ${profile.xp} / ${profile.level * 500}", barLeft, barTop - 12f, xpLabelPaint)

        // Stats Kacheln unten
        val statY = cardMargin + 440f
        val boxWidth = (width - cardMargin * 2 - 120f) / 3f
        val boxHeight = 120f

        val stats = listOf(
            "SEKTOREN" to "${profile.exploredCount}",
            "DUNGEONS" to "${profile.visitedDungeons}",
            "ACHIEVEMENTS" to "${profile.unlockedAchievements.size}"
        )

        val boxBgPaint = Paint().apply { color = theme.primaryColor; alpha = 20 }
        val boxBorderPaint = Paint().apply {
            color = theme.primaryColor
            alpha = 80
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        val statValPaint = Paint().apply {
            color = theme.primaryColor
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val statLblPaint = Paint().apply {
            color = theme.subTextColor
            textSize = 20f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        stats.forEachIndexed { index, pair ->
            val bLeft = barLeft + index * (boxWidth + 20f)
            val bRect = RectF(bLeft, statY, bLeft + boxWidth, statY + boxHeight)
            canvas.drawRoundRect(bRect, 12f, 12f, boxBgPaint)
            canvas.drawRoundRect(bRect, 12f, 12f, boxBorderPaint)

            canvas.drawText(pair.second, bLeft + boxWidth / 2f, statY + 55f, statValPaint)
            canvas.drawText(pair.first, bLeft + boxWidth / 2f, statY + 95f, statLblPaint)
        }

        // Footer / Wasserzeichen
        val footerPaint = Paint().apply {
            color = theme.subTextColor
            textSize = 18f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        canvas.drawText("SAD // CITY AS A DUNGEON OS", width - cardMargin - 320f, height - cardMargin - 15f, footerPaint)

        return bitmap
    }

    private fun drawStatsPanel(profile: PlayerProfile, theme: CardTheme): Bitmap {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Hintergründe
        val bgPaint = Paint().apply { color = theme.bgColor; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Hauptkarte (Rahmen)
        val cardMargin = 40f
        val cardRect = RectF(cardMargin, cardMargin, width - cardMargin, height - cardMargin)
        val cardPaint = Paint().apply { color = theme.surfaceColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

        val borderPaint = Paint().apply {
            color = theme.primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        // Header Title
        val headerPaint = Paint().apply {
            color = theme.primaryColor
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("CITY AS A DUNGEON // STATS", cardMargin + 40f, cardMargin + 70f, headerPaint)

        // Avatar-Kreis groß oben mittig
        val avatarRadius = 110f
        val avatarCenterX = width / 2f
        val avatarCenterY = cardMargin + 230f

        val avatarBgPaint = Paint().apply { color = theme.primaryColor; alpha = 40; isAntiAlias = true }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBgPaint)

        val avatarBorderPaint = Paint().apply {
            color = theme.primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBorderPaint)

        val initials = if (profile.playerName.isNotBlank()) profile.playerName.take(2).uppercase() else "SAD"
        val initialsPaint = Paint().apply {
            color = theme.primaryColor
            textSize = 72f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(initials, avatarCenterX, avatarCenterY + 25f, initialsPaint)

        // Spieler Name & Titel
        var y = avatarCenterY + avatarRadius + 60f
        val namePaint = Paint().apply {
            color = theme.textColor
            textSize = 54f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(profile.displayName, avatarCenterX, y, namePaint)

        y += 50f
        val rankPaint = Paint().apply {
            color = theme.accentColor
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("LEVEL ${profile.level} · ${profile.title.uppercase()}", avatarCenterX, y, rankPaint)

        // XP Balken
        y += 60f
        val barLeft = cardMargin + 60f
        val barRight = width - cardMargin - 60f
        val barHeight = 20f

        val barBgPaint = Paint().apply { color = theme.primaryColor; alpha = 30 }
        canvas.drawRoundRect(RectF(barLeft, y, barRight, y + barHeight), 10f, 10f, barBgPaint)

        val xpForThisLevel = (profile.level - 1) * 500
        val xpProgress = ((profile.xp - xpForThisLevel).toFloat() / 500f).coerceIn(0f, 1f)
        val barFillRight = barLeft + (barRight - barLeft) * xpProgress

        val barFillPaint = Paint().apply { color = theme.primaryColor; isAntiAlias = true }
        canvas.drawRoundRect(RectF(barLeft, y, barFillRight, y + barHeight), 10f, 10f, barFillPaint)

        val xpTextPaint = Paint().apply {
            color = theme.subTextColor
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("${profile.xp} / ${profile.level * 500} XP", avatarCenterX, y - 12f, xpTextPaint)

        // Stats Kacheln (Grid 2x2)
        y += 80f
        val gridMargin = cardMargin + 50f
        val cellWidth = (width - gridMargin * 2 - 30f) / 2f
        val cellHeight = 160f

        val statsGrid = listOf(
            "ERKUNDTE SEKTOREN" to "${profile.exploredCount}",
            "BEZWUNGENE DUNGEONS" to "${profile.visitedDungeons}",
            "NIGHT OWL EXPLO" to "${profile.nightExploredCount}",
            "ERRUNGENSCHAFTEN" to "${profile.unlockedAchievements.size}"
        )

        val cellBgPaint = Paint().apply { color = theme.primaryColor; alpha = 20 }
        val cellBorderPaint = Paint().apply {
            color = theme.primaryColor
            alpha = 80
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        val cellValPaint = Paint().apply {
            color = theme.primaryColor
            textSize = 52f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val cellLblPaint = Paint().apply {
            color = theme.subTextColor
            textSize = 20f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        statsGrid.forEachIndexed { index, pair ->
            val row = index / 2
            val col = index % 2
            val cLeft = gridMargin + col * (cellWidth + 30f)
            val cTop = y + row * (cellHeight + 30f)
            val cRect = RectF(cLeft, cTop, cLeft + cellWidth, cTop + cellHeight)

            canvas.drawRoundRect(cRect, 16f, 16f, cellBgPaint)
            canvas.drawRoundRect(cRect, 16f, 16f, cellBorderPaint)

            canvas.drawText(pair.second, cLeft + cellWidth / 2f, cTop + 75f, cellValPaint)
            canvas.drawText(pair.first, cLeft + cellWidth / 2f, cTop + 125f, cellLblPaint)
        }

        // Footer / Brand
        val footerPaint = Paint().apply {
            color = theme.subTextColor
            textSize = 22f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("SAD // CITY AS A DUNGEON", avatarCenterX, height - cardMargin - 30f, footerPaint)

        return bitmap
    }
}
