package moe.tlaster.farcaster.parser

import moe.tlaster.farcaster.parser.tokenizer.StringReader
import moe.tlaster.farcaster.parser.tokenizer.Tokenizer
import moe.tlaster.farcaster.parser.tree.Node
import moe.tlaster.farcaster.parser.tree.TreeBuilder

class FarcasterParser {
    fun parse(input: String): List<Node> {
        val tokenizer = Tokenizer()
        val tokenCharacterTypes = tokenizer.parse(StringReader(input))
        return TreeBuilder().build(StringReader(input), tokenCharacterTypes)
    }
}