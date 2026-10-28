package com.example.engine

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

object DocParser {

    fun parseUri(context: Context, uri: Uri, fileName: String): String {
        val lower = fileName.lowercase()
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            when {
                lower.endsWith(".docx") -> parseDocx(stream)
                lower.endsWith(".pdf") -> parsePdf(stream)
                else -> parsePlainText(stream)
            }
        } ?: throw IllegalStateException("Could not read file $fileName")
    }

    private fun parsePlainText(inputStream: InputStream): String {
        val raw = inputStream.bufferedReader().use { it.readText() }
        return raw.replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .trim()
    }

    fun parseDocx(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        val textBuilder = StringBuilder()

        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(zip, "UTF-8")

                var eventType = parser.eventType
                var inTextTag = false
                var currentParagraph = StringBuilder()
                var currentCell = StringBuilder()
                var currentRow = mutableListOf<String>()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name) {
                                "t" -> inTextTag = true
                                "tab" -> currentParagraph.append("\t")
                                "br" -> currentParagraph.append("\n")
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inTextTag) {
                                val t = parser.text
                                currentParagraph.append(t)
                                currentCell.append(t)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (parser.name) {
                                "t" -> inTextTag = false
                                "tc" -> {
                                    if (currentCell.isNotBlank()) {
                                        currentRow.add(currentCell.toString().trim())
                                        currentCell = StringBuilder()
                                    }
                                }
                                "tr" -> {
                                    if (currentRow.isNotEmpty()) {
                                        textBuilder.append(currentRow.joinToString(" | ")).append("\n")
                                        currentRow = mutableListOf()
                                    }
                                }
                                "p" -> {
                                    val pText = currentParagraph.toString().trim()
                                    if (pText.isNotEmpty()) {
                                        textBuilder.append(pText).append("\n\n")
                                        currentParagraph = StringBuilder()
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                break
            }
            entry = zip.nextEntry
        }

        val result = textBuilder.toString().trim()
        if (result.isEmpty()) {
            throw IllegalStateException("Empty or unreadable Word document")
        }
        return result
    }

    fun parsePdf(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        val text = String(bytes, Charsets.ISO_8859_1)

        val sb = StringBuilder()

        // 1. Extract Tj parenthesized strings
        val tjMatcher = java.util.regex.Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)\\s*(?:Tj|'|\")").matcher(text)
        var count = 0
        while (tjMatcher.find()) {
            val raw = tjMatcher.group(1) ?: continue
            val unescaped = unescapePdfString(raw)
            if (unescaped.isNotBlank()) {
                sb.append(unescaped).append(" ")
                count++
            }
        }

        // 2. Extract array TJ text strings
        if (count < 5) {
            val arrayTjMatcher = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]\\s*TJ").matcher(text)
            while (arrayTjMatcher.find()) {
                val arrayContent = arrayTjMatcher.group(1) ?: continue
                val innerStrMatcher = java.util.regex.Pattern.compile("\\(([^()\\\\]*(?:\\\\.[^()\\\\]*)*)\\)").matcher(arrayContent)
                while (innerStrMatcher.find()) {
                    val raw = innerStrMatcher.group(1) ?: continue
                    val unescaped = unescapePdfString(raw)
                    if (unescaped.isNotBlank()) {
                        sb.append(unescaped).append(" ")
                        count++
                    }
                }
            }
        }

        val resultStr = sb.toString().trim()

        if (count > 5 && resultStr.length > 40) {
            val cleaned = resultStr
                .replace(Regex("\\s+"), " ")
                .replace(" .", ".")
                .replace(" ,", ",")
                .replace(" :", ":")
                .trim()
            return cleaned
        }

        // Fallback: extract continuous text blocks from streams
        val asciiMatcher = java.util.regex.Pattern.compile("[A-Za-z0-9 ,.;:!?'\"()\\-\\n]{30,}").matcher(text)
        val fallbackSb = StringBuilder()
        while (asciiMatcher.find()) {
            val block = asciiMatcher.group()
            if (!block.contains("/Filter") && !block.contains("/Length") && !block.contains("stream") && !block.contains("endobj")) {
                fallbackSb.append(block.trim()).append("\n\n")
            }
        }

        if (fallbackSb.length > 50) {
            return fallbackSb.toString().trim()
        }

        throw IllegalStateException("No extractable text in PDF. The document might be scanned or image-based.")
    }

    private fun unescapePdfString(raw: String): String {
        var s = raw.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")

        val octalMatcher = java.util.regex.Pattern.compile("\\\\([0-7]{1,3})").matcher(s)
        val sb = StringBuffer()
        while (octalMatcher.find()) {
            val octalStr = octalMatcher.group(1)
            val code = octalStr.toInt(8)
            octalMatcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(code.toChar().toString()))
        }
        octalMatcher.appendTail(sb)
        return sb.toString()
    }
}
