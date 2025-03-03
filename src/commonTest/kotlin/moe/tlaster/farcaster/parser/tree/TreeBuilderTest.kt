package moe.tlaster.farcaster.parser.tree

import moe.tlaster.farcaster.parser.FarcasterParser
import moe.tlaster.farcaster.parser.tokenizer.StringReader
import moe.tlaster.farcaster.parser.tokenizer.Tokenizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TreeBuilderTest {

    @Test
    fun testUserName() {
        val tokenizer = Tokenizer()
        val content = "@test"
        val result = tokenizer.parse(StringReader(content))
        val builder = TreeBuilder()
        val builderResult = builder.build(StringReader(content), result)
        assertEquals(
            listOf(
                UserNode("@test"),
            ),
            builderResult,
        )
    }

    @Test
    fun testChannel() {
        val tokenizer = Tokenizer()
        val content = "/test"
        val result = tokenizer.parse(StringReader(content))
        val builder = TreeBuilder()
        val builderResult = builder.build(StringReader(content), result)
        assertEquals(
            listOf(
                ChannelNode("/test"),
            ),
            builderResult,
        )
    }

    @Test
    fun testCustomUser() {
        val tokenizer = Tokenizer()
        val content = "test.twitter"
        val result = tokenizer.parse(StringReader(content))
        val builder = TreeBuilder()
        val builderResult = builder.build(StringReader(content), result)
        assertEquals(
            listOf(
                CustomUserNode("test.twitter"),
            ),
            builderResult,
        )
    }

    @Test
    fun testUrl() {
        val tokenizer = Tokenizer()
        val content = "https://test.com"
        val result = tokenizer.parse(StringReader(content))
        val builder = TreeBuilder()
        val builderResult = builder.build(StringReader(content), result)
        assertEquals(
            listOf(
                UrlNode("https://test.com"),
            ),
            builderResult,
        )
    }

    @Test
    fun testCash() {
        val tokenizer = Tokenizer()
        val content = "\$TEST"
        val result = tokenizer.parse(StringReader(content))
        val builder = TreeBuilder()
        val builderResult = builder.build(StringReader(content), result)
        assertEquals(
            listOf(
                CashNode("\$TEST"),
            ),
            builderResult,
        )
    }

    @Test
    fun testMixed() {
        val tokenizer = Tokenizer()
        val content = "Hello @test /test test.twitter test.lens https://test.com \$TEST"
        val result = tokenizer.parse(StringReader(content))
        val builder = TreeBuilder()
        val builderResult = builder.build(StringReader(content), result)
        assertEquals(
            listOf(
                TextNode("Hello "),
                UserNode("@test"),
                TextNode(" "),
                ChannelNode("/test"),
                TextNode(" "),
                CustomUserNode("test.twitter"),
                TextNode(" "),
                CustomUserNode("test.lens"),
                TextNode(" "),
                UrlNode("https://test.com"),
                TextNode(" "),
                CashNode("\$TEST"),
            ),
            builderResult,
        )
    }

    @Test
    fun testNotCash() {
        val parser = FarcasterParser()
        val url = "$123"
        val content = "test $url."
        val result = parser.parse(content)
        assertEquals(1, result.size)
        val node = result[0]
        assertIs<TextNode>(node)
        assertEquals(content, node.value)
    }

    @Test
    fun testNotUrl() {
        val parser = FarcasterParser()
        val content = "In 2024, around 10% of Anguilla’s GDP will come from fees for its .ai domain name."
        val result = parser.parse(content)
        assertEquals(1, result.size)
        val node = result[0]
        assertIs<TextNode>(node)
        assertEquals(content, node.value)
    }
}
