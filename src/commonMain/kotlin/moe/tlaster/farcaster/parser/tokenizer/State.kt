package moe.tlaster.farcaster.parser.tokenizer
private val asciiUppercase = 'A'..'Z'
private val asciiLowercase = 'a'..'z'
private val asciiAlpha = asciiUppercase + asciiLowercase
private val asciiDigit = '0'..'9'
private val asciiAlphanumeric = asciiAlpha + asciiDigit
private val asciiAlphanumericUnderscore = asciiAlphanumeric + '_'
private val asciiAlphanumericUnderscoreDash = asciiAlphanumericUnderscore + '-'
private val asciiAlphanumericUnderscoreDashPlus = asciiAlphanumericUnderscoreDash + '+'
private val asciiUpperHexDigit = 'A'..'F'
private val asciiLowerHexDigit = 'a'..'f'
private val asciiHexDigit = asciiUpperHexDigit + asciiLowerHexDigit
private const val NULL = '\u0000'
private const val TAB = '\u0009'
private const val LF = '\u000A'
private val emptyChar = listOf(TAB, LF, '\u000C', '\u0020')
private val asciiAlphanumericAndEmpty = asciiAlphanumeric + ' ' + TAB + LF
private val urlChar = asciiAlphanumeric + "-._~:/?#[]@!\$&'()*+,;=".toList()
private val marks = "-._~:/?#[]@!\$&'()*+,;=".toList()

private fun Int.isFullWidthCodePoint(): Boolean {
    val cp = this
    if (cp < 0) return false
    return (cp in 0x1100..0x115F) || // Hangul Jamo
        (cp == 0x2329 || cp == 0x232A) || // 〈 〉
        (cp in 0x2E80..0xA4CF && cp != 0x303F) || // CJK radicals/strokes… Yi (exclude 0x303F)
        (cp in 0xAC00..0xD7A3) || // Hangul Syllables
        (cp in 0xF900..0xFAFF) || // CJK Compatibility Ideographs
        (cp in 0xFE10..0xFE19) || // Vertical forms
        (cp in 0xFE30..0xFE6F) || // CJK Compatibility Forms… Small Form Variants
        (cp in 0xFF01..0xFF60) || // Fullwidth ASCII variants etc.
        (cp in 0xFFE0..0xFFE6) || // Fullwidth symbols
        (cp in 0x1B000..0x1B001) || // Kana Supplement
        (cp in 0x1F200..0x1F251) || // Enclosed Ideographic Supplement
        (cp in 0x20000..0x3FFFD) // CJK Unified Ideographs Ext. B–(up to T)
}

private fun Char.isFullWidthChar(): Boolean = this.code.isFullWidthCodePoint()

private fun Int.isFullWidthSymbolCodePoint(): Boolean {
    val cp = this
    if (cp < 0) return false

    // CJK Symbols & Punctuation
    if (cp in 0x3001..0x303D) return true // 「」『』、《》、。 、、
    // Vertical Forms
    if (cp in 0xFE10..0xFE19) return true
    // CJK Compatibility Forms
    if (cp in 0xFE30..0xFE4F) return true
    // Small Form Variants
    if (cp in 0xFE50..0xFE6F) return true
    // Fullwidth ASCII
    if (cp in 0xFF01..0xFF0F) return true // ！"#$%&'()*+,-./
    if (cp in 0xFF1A..0xFF20) return true // ：；＜＝＞？＠
    if (cp in 0xFF3B..0xFF40) return true // ［＼］＾＿`
    if (cp in 0xFF5B..0xFF60) return true // ｛｜｝～
    return false
}

private fun Char.isFullWidthSymbol(): Boolean = this.code.isFullWidthSymbolCodePoint()

private fun prevIsFullWidthChar(reader: Reader): Boolean {
    return reader.position >= 2 && reader.readAt(reader.position - 2).isFullWidthChar()
}
private fun prevIsSpace(reader: Reader): Boolean {
    // position == 1 means it is at the beginning of the string, since we consume the first character
    return reader.position == 1 || reader.readAt(reader.position - 2) in emptyChar
}

private fun findBackwardSpace(reader: Reader): Int {
    var position = reader.position
    while (position > 0) {
        if (reader.readAt(position - 1) in emptyChar) {
            // not -1 because we want the position right after the space
            return position
        }
        position--
    }
    // is at the beginning of the string
    return 0
}

internal sealed interface State {
    fun read(tokenizer: Tokenizer, reader: Reader)
}

internal data object DataState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            'h' -> tokenizer.switch(HState)
            '$' -> tokenizer.switch(DollarState)
            '@' -> tokenizer.switch(AtState)
            '/' -> tokenizer.switch(SlashState)
            '.' -> tokenizer.switch(DotState)
            '#' -> tokenizer.switch(HashState)
            eof -> tokenizer.emit(TokenCharacterType.Eof, reader.position)
            else -> tokenizer.emit(TokenCharacterType.Character, reader.position)
        }
    }
}

internal data object HState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        if (reader.isFollowedBy("ttps://", ignoreCase = true)) {
            tokenizer.emitRange(TokenCharacterType.Url, reader.position - 1, reader.position - 1 + "https://".length)
            tokenizer.switch(UrlState)
            reader.consume("ttps://".length)
        } else if (reader.isFollowedBy("ttp://", ignoreCase = true)) {
            tokenizer.emitRange(TokenCharacterType.Url, reader.position - 1, reader.position - 1 + "http://".length)
            tokenizer.switch(UrlState)
            reader.consume("ttp://".length)
        } else {
            tokenizer.emit(TokenCharacterType.Character, reader.position)
            tokenizer.switch(DataState)
        }
    }
}

internal data object UrlState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in emptyChar + eof -> {
                tokenizer.accept()
                tokenizer.switch(DataState)
                reader.pushback()
            }

            else -> {
                tokenizer.emit(TokenCharacterType.Url, reader.position)
            }
        }
    }
}

internal data object DollarState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        val current = reader.consume()
        if (current in asciiAlpha) {
            tokenizer.emit(TokenCharacterType.Cash, reader.position - 1)
            tokenizer.switch(CashTagState)
            reader.pushback()
        } else if (current.isFullWidthChar() && !current.isFullWidthSymbol()) {
            tokenizer.emit(TokenCharacterType.Cash, reader.position - 1)
            tokenizer.switch(CJKCashTagState)
            reader.pushback()
        } else if (current in asciiDigit) {
            tokenizer.emit(TokenCharacterType.Cash, reader.position - 1)
            tokenizer.switch(DigitCashState)
            reader.pushback()
        } else {
            tokenizer.emit(TokenCharacterType.Character, reader.position - 1)
            tokenizer.switch(DataState)
            reader.pushback()
        }
    }
}

internal data object DigitCashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        val current = reader.consume()
        if (current in listOf('k', 'K', 'm', 'M', 'b', 'B', '.', ',')) {
            // reject
            var start = reader.position - 1
            while (start > 0) {
                if (tokenizer.readAt(start - 1) != TokenCharacterType.Cash) {
                    break
                }
                start--
            }
            tokenizer.emitRange(TokenCharacterType.Character, start, reader.position)
            tokenizer.accept()
            tokenizer.switch(DataState)
            reader.pushback()
        } else if (current in emptyChar + eof || current in "-._~:?#[]@!\$&'()*+,;=".toList()) {
            tokenizer.accept()
            tokenizer.switch(DataState)
            reader.pushback()
        } else {
            tokenizer.emit(TokenCharacterType.Cash, reader.position)
            // if cashtag length > 10, accept as cash and switch to data state
            var start = reader.position - 1
            var length = 0
            while (start > 0) {
                if (tokenizer.readAt(start - 1) != TokenCharacterType.Cash) {
                    break
                }
                start--
                length++
            }
            if (length >= 10) {
                tokenizer.accept()
                tokenizer.switch(DataState)
            }
        }
    }
}

internal data object CJKCashTagState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        val current = reader.consume()
        if (current.isFullWidthChar() && !current.isFullWidthSymbol() || current in asciiAlphanumericUnderscore) {
            tokenizer.emit(TokenCharacterType.Cash, reader.position)
            // if cashtag length > 10, accept as cash and switch to data state
            var start = reader.position - 1
            var length = 0
            while (start > 0) {
                if (tokenizer.readAt(start - 1) != TokenCharacterType.Cash) {
                    break
                }
                start--
                length++
            }
            if (length >= 10) {
                tokenizer.accept()
                tokenizer.switch(DataState)
            }
        } else {
            tokenizer.accept()
            tokenizer.switch(DataState)
            reader.pushback()
        }
    }
}

internal data object CashTagState : State {

    override fun read(tokenizer: Tokenizer, reader: Reader) {
        val current = reader.consume()
        if (current in asciiAlphanumeric) {
            tokenizer.emit(TokenCharacterType.Cash, reader.position)
            // if cashtag length > 20, accept as cash and switch to data state
            var start = reader.position - 1
            var length = 0
            while (start > 0) {
                if (tokenizer.readAt(start - 1) != TokenCharacterType.Cash) {
                    break
                }
                start--
                length++
            }
            if (length >= 20) {
                tokenizer.accept()
                tokenizer.switch(DataState)
            }
        } else {
            if (current in emptyChar + eof || current in marks) {
                // accept as cash tag
                tokenizer.accept()
            } else {
                // reject as character
                var start = reader.position - 1
                while (start > 0) {
                    if (tokenizer.readAt(start - 1) != TokenCharacterType.Cash) {
                        break
                    }
                    start--
                }
                tokenizer.emitRange(TokenCharacterType.Character, start, reader.position)
            }
            tokenizer.accept()
            tokenizer.switch(DataState)
            reader.pushback()
        }
    }
}

internal data object AtState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlphanumericUnderscoreDash -> {
                tokenizer.emit(TokenCharacterType.UserName, reader.position - 1)
                tokenizer.switch(UserNameState)
                reader.pushback()
            }

            else -> {
                tokenizer.emit(TokenCharacterType.Character, reader.position - 1)
                tokenizer.switch(DataState)
                reader.pushback()
            }
        }
    }
}

internal data object UserNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        val userNameTokens = if (tokenizer.enableDotInUserName) {
            asciiAlphanumericUnderscore + '.'
        } else {
            asciiAlphanumericUnderscore
        } + '-'
        when (val current = reader.consume()) {
            in userNameTokens -> {
                if (current == '.') {
                    // require next char to be in asciiAlphanumeric
                    if (!reader.hasNext() || reader.next() !in asciiAlphanumeric) {
                        tokenizer.accept()
                        tokenizer.switch(DataState)
                        reader.pushback()
                    } else {
                        tokenizer.emit(TokenCharacterType.UserName, reader.position)
                    }
                } else {
                    tokenizer.emit(TokenCharacterType.UserName, reader.position)
                }
            }

            else -> {
                tokenizer.accept()
                tokenizer.switch(DataState)
                reader.pushback()
            }
        }
    }
}

internal data object SlashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        if (prevIsSpace(reader) && !reader.isFollowedBy("-")) {
            when (val current = reader.consume()) {
                in asciiAlphanumericUnderscoreDash -> {
                    tokenizer.emit(TokenCharacterType.Channel, reader.position - 1)
                    tokenizer.switch(ChannelNameState)
                    reader.pushback()
                }

                else -> {
                    tokenizer.emit(TokenCharacterType.Character, reader.position - 1)
                    tokenizer.switch(DataState)
                    reader.pushback()
                }
            }
        } else {
            tokenizer.emit(TokenCharacterType.Character, reader.position)
            tokenizer.switch(DataState)
        }
    }
}

internal data object ChannelNameState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlphanumericUnderscoreDash -> {
                tokenizer.emit(TokenCharacterType.Channel, reader.position)
            }

            '/' -> {
                // reject all the channel name
                reader.pushback()
                val start = findBackwardSlash(reader)
                tokenizer.emitRange(TokenCharacterType.Character, start, reader.position)
                tokenizer.switch(DataState)
            }

            else -> {
                tokenizer.accept()
                tokenizer.switch(DataState)
                reader.pushback()
            }
        }
    }
    private fun findBackwardSlash(reader: Reader): Int {
        var position = reader.position
        while (position > 0) {
            if (reader.readAt(position - 1) == '/') {
                return position - 1
            }
            position--
        }
        // is at the beginning of the string
        return 0
    }
}

private fun findBackwardValidUrl(reader: Reader): Int {
    var position = reader.position
    while (position > 0) {
        if (reader.readAt(position - 1) !in urlChar) {
            // not -1 because we want the position right after the text
            return position
        }
        position--
    }
    // is at the beginning of the string
    return 0
}

internal data object DotState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        if (reader.position > 1 && reader.readAt(reader.position - 2) !in emptyChar) {
            tokenizer.customUserSuffix.forEach {
                if (reader.isFollowedBy(it, ignoreCase = true)) {
                    val start = findBackwardSpace(reader)
                    tokenizer.emitRange(TokenCharacterType.CustomUser, start, reader.position + it.length)
                    reader.consume(it.length)
                    tokenizer.switch(DataState)
                    return
                }
            }
        }
        if (reader.position > 1 && reader.readAt(reader.position - 2) !in emptyChar && reader.readAt(reader.position) in asciiAlphanumeric) {
            val start = findBackwardValidUrl(reader)
            tokenizer.emitRange(TokenCharacterType.Url, start, reader.position)
            tokenizer.switch(HeadlessUrlState)
        } else {
            tokenizer.emit(TokenCharacterType.Character, reader.position)
            tokenizer.switch(DataState)
        }
    }
}

internal data object HeadlessUrlState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            !in urlChar -> {
                reader.pushback()
                val start = findBackwardValidUrl(reader)
                val value = reader.readAt(start, reader.position - start).split('.').lastOrNull()?.takeWhile {
                    it in asciiAlpha
                }
                if (value != null && DomainList.any { it.equals(value, ignoreCase = true) }) {
                    tokenizer.emitRange(TokenCharacterType.Url, start, reader.position)
                } else {
                    tokenizer.emitRange(TokenCharacterType.Character, start, reader.position)
                }
                tokenizer.accept()
                tokenizer.switch(DataState)
            }

            else -> {
                tokenizer.emit(TokenCharacterType.Url, reader.position)
            }
        }
    }
}

internal data object HashState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlphanumeric -> {
                tokenizer.emit(TokenCharacterType.HashTag, reader.position - 1)
                tokenizer.switch(HashTagState)
                reader.pushback()
            }

            else -> {
                tokenizer.emit(TokenCharacterType.Character, reader.position - 1)
                tokenizer.switch(DataState)
                reader.pushback()
            }
        }
    }
}

internal data object HashTagState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlphanumeric -> {
                tokenizer.emit(TokenCharacterType.HashTag, reader.position)
            }

            else -> {
                tokenizer.accept()
                tokenizer.switch(DataState)
                reader.pushback()
            }
        }
    }
}
