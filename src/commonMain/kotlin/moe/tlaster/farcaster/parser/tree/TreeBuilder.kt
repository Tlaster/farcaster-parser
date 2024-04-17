package moe.tlaster.farcaster.parser.tree

import moe.tlaster.farcaster.parser.tokenizer.Reader
import moe.tlaster.farcaster.parser.tokenizer.TokenCharacterType

internal class TreeBuilder {
    fun build(reader: Reader, tokenCharacterTypes: List<TokenCharacterType>): List<Node> {
        val nodes = mutableListOf<Node>()
        var currentType: TokenCharacterType? = null
        var currentStart = 0
        for (i in 0 until reader.length) {
            val type = tokenCharacterTypes[i]
            if (currentType == null) {
                currentType = type
                currentStart = i
            } else if (currentType != type) {
                nodes.add(buildNode(currentType, reader, currentStart, i))
                currentType = type
                currentStart = i
            }
        }
        if (currentType != null) {
            nodes.add(buildNode(currentType, reader, currentStart, reader.length))
        }
        return nodes - EofNode
    }

    private fun buildNode(currentType: TokenCharacterType, reader: Reader, currentStart: Int, i: Int): Node {
        return when (currentType) {
            TokenCharacterType.Character -> TextNode(reader.readAt(currentStart, i - currentStart))
            TokenCharacterType.Url -> UrlNode(reader.readAt(currentStart, i - currentStart))
            TokenCharacterType.Cash -> CashNode(reader.readAt(currentStart, i - currentStart))
            TokenCharacterType.UserName -> UserNode(reader.readAt(currentStart, i - currentStart))
            TokenCharacterType.Channel -> ChannelNode(reader.readAt(currentStart, i - currentStart))
            TokenCharacterType.CustomUser -> CustomUserNode(reader.readAt(currentStart, i - currentStart))
            TokenCharacterType.Eof -> EofNode
            TokenCharacterType.UnKnown -> TextNode(reader.readAt(currentStart, i - currentStart))
            TokenCharacterType.HashTag -> HashTagNode(reader.readAt(currentStart, i - currentStart))
        }
    }
}
