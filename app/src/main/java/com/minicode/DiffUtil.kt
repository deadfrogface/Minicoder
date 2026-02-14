package com.minicode

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan

object DiffUtil {

    fun computeLineDiff(original: String, generated: String): Pair<SpannableStringBuilder, SpannableStringBuilder> {
        val origLines = original.lines()
        val genLines = generated.lines()
        val origOut = SpannableStringBuilder(original)
        val genOut = SpannableStringBuilder(generated)

        val maxLines = maxOf(origLines.size, genLines.size)
        var origOffset = 0
        var genOffset = 0

        for (i in 0 until maxLines) {
            val origLine = origLines.getOrNull(i) ?: ""
            val genLine = genLines.getOrNull(i) ?: ""
            val origLineLen = origLine.length + if (i < origLines.size - 1) 1 else 0
            val genLineLen = genLine.length + if (i < genLines.size - 1) 1 else 0

            if (origLine != genLine) {
                val highlight = 0x1AFF9800.toInt()
                if (origLineLen > 0 && origOffset < origOut.length) {
                    origOut.setSpan(
                        BackgroundColorSpan(highlight),
                        origOffset,
                        (origOffset + origLineLen).coerceAtMost(origOut.length),
                        0
                    )
                }
                if (genLineLen > 0 && genOffset < genOut.length) {
                    genOut.setSpan(
                        BackgroundColorSpan(highlight),
                        genOffset,
                        (genOffset + genLineLen).coerceAtMost(genOut.length),
                        0
                    )
                }
            }
            origOffset += origLineLen
            genOffset += genLineLen
        }

        return Pair(origOut, genOut)
    }
}
