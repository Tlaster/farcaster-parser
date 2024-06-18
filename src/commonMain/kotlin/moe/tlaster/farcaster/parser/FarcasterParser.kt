package moe.tlaster.farcaster.parser

import moe.tlaster.farcaster.parser.tokenizer.StringReader
import moe.tlaster.farcaster.parser.tokenizer.Tokenizer
import moe.tlaster.farcaster.parser.tree.Node
import moe.tlaster.farcaster.parser.tree.TreeBuilder

class FarcasterParser(
    private val enableDotInUserName: Boolean = false,
    private val customUserSuffix: List<String> = listOf("twitter", "lens", "github", "telegram", "eth"),
) {
    fun parse(input: String): List<Node> {
        val tokenizer = Tokenizer(
            enableDotInUserName = enableDotInUserName,
            customUserSuffix = customUserSuffix,
        )
        val tokenCharacterTypes = tokenizer.parse(StringReader(input))
        return TreeBuilder().build(StringReader(input), tokenCharacterTypes)
    }
}
