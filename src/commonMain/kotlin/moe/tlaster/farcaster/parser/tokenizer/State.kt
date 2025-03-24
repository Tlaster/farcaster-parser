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
        when (val current = reader.consume()) {
            in asciiAlpha -> {
                tokenizer.emit(TokenCharacterType.Cash, reader.position - 1)
                tokenizer.switch(CashTagState)
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

internal data object CashTagState : State {
    private fun cashCheck(tokenizer: Tokenizer, reader: Reader) {
        var index = reader.position - 2
        while (index > 0) {
            if (tokenizer.readAt(index) != TokenCharacterType.Cash) {
                break
            }
            index--
        }
        val cash = reader.readAt(index + 1, reader.position - index - 2).trimStart('$').trimStart('＄')
        if (cash.all { it in asciiDigit }) {
            tokenizer.emitRange(TokenCharacterType.Character, index, reader.position)
            tokenizer.switch(DataState)
            reader.pushback()
        } else {
            tokenizer.accept()
            tokenizer.switch(DataState)
            reader.pushback()
        }
    }
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlphanumeric -> {
                tokenizer.emit(TokenCharacterType.Cash, reader.position)
            }

            else -> {
                cashCheck(tokenizer, reader)
            }
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
