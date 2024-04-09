package moe.tlaster.farcaster.parser.tokenizer

internal enum class TokenCharacterType {
    Eof,
    Character,
    Url,
    Cash,
    UserName,
    Channel,
    TwitterUser,
    LensterUser,

    UnKnown,
}
