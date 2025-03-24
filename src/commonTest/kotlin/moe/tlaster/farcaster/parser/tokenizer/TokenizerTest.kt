package moe.tlaster.farcaster.parser.tokenizer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TokenizerTest {
    @Test
    fun testAtUserName() {
        val tokenizer = Tokenizer()
        val content = "@test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testCash() {
        val tokenizer = Tokenizer()
        val content = "\$test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Cash,
                TokenCharacterType.Cash,
                TokenCharacterType.Cash,
                TokenCharacterType.Cash,
                TokenCharacterType.Cash,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testHashtag() {
        val tokenizer = Tokenizer()
        val content = "#test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.HashTag,
                TokenCharacterType.HashTag,
                TokenCharacterType.HashTag,
                TokenCharacterType.HashTag,
                TokenCharacterType.HashTag,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testChannel() {
        val tokenizer = Tokenizer()
        val content = "/test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testNotChannel() {
        val tokenizer = Tokenizer()
        val content = "/test/test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            content.map { TokenCharacterType.Character } + TokenCharacterType.Eof,
            result,
        )
    }

    @Test
    fun testNotChannel2() {
        val tokenizer = Tokenizer()
        val content = "/-test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            content.map { TokenCharacterType.Character } + TokenCharacterType.Eof,
            result,
        )
    }

    @Test
    fun testChannel2() {
        val tokenizer = Tokenizer()
        val content = "/test-test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Channel,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testChannel3() {
        val tokenizer = Tokenizer()
        val content = "/test."
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            "/test".map { TokenCharacterType.Channel } +
                ".".map { TokenCharacterType.Character } +
                TokenCharacterType.Eof,
            result,
        )
    }

    @Test
    fun testCustomUserName() {
        val tokenizer = Tokenizer()
        val content = "test.twitter"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.CustomUser,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testNonCustomUserName() {
        val tokenizer = Tokenizer()
        val content = ".twitter"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testNonCustomUserName2() {
        val tokenizer = Tokenizer()
        val content = "1 .twitter"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testUrl() {
        val tokenizer = Tokenizer()
        val content = "https://test.com"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testHeadlessUrl() {
        val tokenizer = Tokenizer()
        val content = "test.com"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testHeadlessUrl2() {
        val tokenizer = Tokenizer()
        val content = "test.host.com"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Url,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }

    @Test
    fun testHeadlessUrl3() {
        val tokenizer = Tokenizer()
        val content = "vision.io/0x/dos"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            content.map { TokenCharacterType.Url } + TokenCharacterType.Eof,
            result,
        )
    }

    @Test
    fun testMixed() {
        val tokenizer = Tokenizer()
        val content = "test test.com @test \$test /test test.twitter test.lens https://test.com test.host.com #test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        val expected = "test ".map { TokenCharacterType.Character } +
            "test.com".map { TokenCharacterType.Url } +
            " ".map { TokenCharacterType.Character } +
            "@".map { TokenCharacterType.UserName } +
            "test".map { TokenCharacterType.UserName } +
            " ".map { TokenCharacterType.Character } +
            "\$".map { TokenCharacterType.Cash } +
            "test".map { TokenCharacterType.Cash } +
            " ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "test".map { TokenCharacterType.Channel } +
            " ".map { TokenCharacterType.Character } +
            "test".map { TokenCharacterType.CustomUser } +
            ".twitter".map { TokenCharacterType.CustomUser } +
            " ".map { TokenCharacterType.Character } +
            "test".map { TokenCharacterType.CustomUser } +
            ".lens".map { TokenCharacterType.CustomUser } +
            " ".map { TokenCharacterType.Character } +
            "https://test.com".map { TokenCharacterType.Url } +
            " ".map { TokenCharacterType.Character } +
            "test.host.com".map { TokenCharacterType.Url } +
            " ".map { TokenCharacterType.Character } +
            "#".map { TokenCharacterType.HashTag } +
            "test".map { TokenCharacterType.HashTag } +
            listOf(TokenCharacterType.Eof)
        assertContentEquals(
            expected,
            result,
        )
    }

    @Test
    fun testMixed2() {
        val tokenizer = Tokenizer()
        val content = "crypto-anarchist · DeFi degen · DeSoc explorer, advisor · hosting /luo · writing 0xluo.eth.limo"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        val expected =
            "crypto-anarchist · DeFi degen · DeSoc explorer, advisor · hosting ".map { TokenCharacterType.Character } +
                "/".map { TokenCharacterType.Channel } +
                "luo".map { TokenCharacterType.Channel } +
                " · writing ".map { TokenCharacterType.Character } +
                "0xluo.eth.limo".map { TokenCharacterType.Url } +
                listOf(TokenCharacterType.Eof)
        assertContentEquals(
            expected,
            result,
        )
    }

    @Test
    fun testMixed3() {
        val tokenizer = Tokenizer()
        val content = "Mask.io / suji_yan.twitter checkout /firefly-garden"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        val expected = "Mask.io".map { TokenCharacterType.Url } +
            " / ".map { TokenCharacterType.Character } +
            "suji_yan".map { TokenCharacterType.CustomUser } +
            ".twitter".map { TokenCharacterType.CustomUser } +
            " checkout ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "firefly-garden".map { TokenCharacterType.Channel } +
            listOf(TokenCharacterType.Eof)
        assertContentEquals(
            expected,
            result,
        )
    }

    @Test
    fun testMixed4() {
        val tokenizer = Tokenizer()
        val content =
            "(she/her) Web3 enthusiast. Learning in Public. Built /animeoutcasts Unofficial Hambassasor |General of the North In /japan and /kyoto for /sakura"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        val expected = "(".map { TokenCharacterType.Character } +
            "she".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Character } +
            "her".map { TokenCharacterType.Character } +
            ") Web".map { TokenCharacterType.Character } +
            "3 enthusiast. Learning in Public. Built ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "animeoutcasts".map { TokenCharacterType.Channel } +
            " Unofficial Hambassasor |General of the North In ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "japan".map { TokenCharacterType.Channel } +
            " and ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "kyoto".map { TokenCharacterType.Channel } +
            " for ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "sakura".map { TokenCharacterType.Channel } +
            listOf(TokenCharacterType.Eof)
        assertContentEquals(
            expected,
            result,
        )
    }

    @Test
    fun testMixed5() {
        val tokenizer = Tokenizer()
        val content =
            "dad • sr full stack dev • Building /360 • ENS degen • vision.io/0x/dos • /journal /black /btw /king"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        val expected = "dad • sr full stack dev • Building ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "360".map { TokenCharacterType.Channel } +
            " • ENS degen • ".map { TokenCharacterType.Character } +
            "vision.io/".map { TokenCharacterType.Url } +
            "0x".map { TokenCharacterType.Url } +
            "/".map { TokenCharacterType.Url } +
            "dos".map { TokenCharacterType.Url } +
            " • ".map { TokenCharacterType.Character } +
            "/".map { TokenCharacterType.Channel } +
            "journal".map { TokenCharacterType.Channel } +
            " ".map { TokenCharacterType.Character } +
            "/black".map { TokenCharacterType.Channel } +
            " ".map { TokenCharacterType.Character } +
            "/btw".map { TokenCharacterType.Channel } +
            " ".map { TokenCharacterType.Character } +
            "/king".map { TokenCharacterType.Channel } +
            listOf(TokenCharacterType.Eof)
        assertContentEquals(
            expected,
            result,
        )
    }

    @Test
    fun testNumberDollar() {
        val tokenizer = Tokenizer()
        val content = "test $123test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            content.map { TokenCharacterType.Character } + TokenCharacterType.Eof,
            result,
        )
    }

    @Test
    fun testDotAtUserNameLast() {
        val tokenizer = Tokenizer()
        val content = "@test. test"
        val result = tokenizer.parse(StringReader(content))
        assertEquals(content.length, result.size - 1)
        assertContentEquals(
            listOf(
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.UserName,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Character,
                TokenCharacterType.Eof,
            ),
            result,
        )
    }
}
