package moe.tlaster.farcaster.parser.tree

sealed interface Node

data class TextNode(val value: String) : Node

data class UrlNode(val value: String) : Node

data class CashNode(val value: String) : Node

data class UserNode(val value: String) : Node

data class ChannelNode(val value: String) : Node

data class TwitterUserNode(val value: String) : Node

data class LensterUserNode(val value: String) : Node

data object EofNode : Node

data class HashTagNode(val value: String) : Node
