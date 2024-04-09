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
            in asciiAlphanumeric -> {
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
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            in asciiAlphanumeric -> {
                tokenizer.emit(TokenCharacterType.Cash, reader.position)
            }

            else -> {
                tokenizer.accept()
                tokenizer.switch(DataState)
                reader.pushback()
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
        }
        when (val current = reader.consume()) {
            in userNameTokens -> {
                tokenizer.emit(TokenCharacterType.UserName, reader.position)
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
        if (prevIsSpace(reader)) {
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

            else -> {
                tokenizer.accept()
                tokenizer.switch(DataState)
                reader.pushback()
            }
        }
    }
}

private fun findBackwardValidUrl(reader: Reader): Int {
    var position = reader.position
    while (position > 0) {
        if (reader.readAt(position - 1) !in asciiAlphanumeric + '-' + '.') {
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
        if (reader.isFollowedBy("twitter", ignoreCase = true)) {
            val start = findBackwardSpace(reader)
            tokenizer.emitRange(TokenCharacterType.TwitterUser, start, reader.position + "twitter".length)
            reader.consume("twitter".length)
            tokenizer.switch(DataState)
        } else if (reader.isFollowedBy("lens", ignoreCase = true)) {
            val start = findBackwardSpace(reader)
            tokenizer.emitRange(TokenCharacterType.LensterUser, start, reader.position + "lens".length)
            reader.consume("lens".length)
            tokenizer.switch(DataState)
        } else {
            // treat as headless url
            val start = findBackwardValidUrl(reader)
            tokenizer.emitRange(TokenCharacterType.Url, start, reader.position)
            tokenizer.switch(HeadlessUrlState)
        }
    }
}

internal data object HeadlessUrlState : State {
    override fun read(tokenizer: Tokenizer, reader: Reader) {
        when (val current = reader.consume()) {
            !in asciiAlphanumeric + '-' + '.' -> {
                val start = findBackwardValidUrl(reader)
                val value = reader.readAt(start, reader.position - start).split('.').lastOrNull()
                if (value != null && DomainList.any { it.equals(value, ignoreCase = true) }) {
                    tokenizer.emitRange(TokenCharacterType.Url, start, reader.position)
                } else {
                    tokenizer.emitRange(TokenCharacterType.Character, start, reader.position)
                }
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