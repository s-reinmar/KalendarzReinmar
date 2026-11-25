package com.example.data.transfer

object CsvRowParser {

    fun detectDelimiter(text: String): Char {
        var inQuotes = false
        var commas = 0
        var semicolons = 0
        for (ch in text) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) commas++
                ';' -> if (!inQuotes) semicolons++
                '\n', '\r' -> if (!inQuotes) break
            }
        }
        return if (semicolons > commas) ';' else ','
    }

    fun parseRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < text.length) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < text.length && text[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow.toList())
        }

        return rows
    }

    fun escapeField(value: String): String {
        val needsQuotes = value.any { it == ',' || it == ';' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
