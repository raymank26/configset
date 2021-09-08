package com.configset.client


class ConfigPropertyLinkProcessor {

    companion object {
        val INSTANCE = ConfigPropertyLinkProcessor()
    }

    fun parse(value: String): TokenList {
        var index = 0
        var state = ParseState.IN_TEXT
        val currentBuffer = StringBuilder()
        val tokens = mutableListOf<ValueToken>()
        var appName = ""
        while (index < value.length) {
            when (state) {
                ParseState.IN_TEXT -> {
                    val nextChar = value[index++]
                    if (value.length - index < 2) {
                        currentBuffer.append(nextChar)
                        continue
                    }
                    if (nextChar == '$' && value[index] == '{') {
                        index++
                        tokens.add(Text(currentBuffer.toString()))
                        currentBuffer.clear()
                        state = ParseState.IN_LINK_START
                    } else {
                        currentBuffer.append(nextChar)
                    }
                }

                ParseState.IN_LINK_START -> {
                    val nextChar = value[index++]
                    if (nextChar == '\\') {
                        appName = currentBuffer.toString()
                        require(appName.isNotEmpty())
                        currentBuffer.clear()
                        state = ParseState.IN_LINK_END
                    } else {
                        currentBuffer.append(nextChar)
                    }
                }
                ParseState.IN_LINK_END -> {
                    val nextChar = value[index++]
                    if (nextChar == '}') {
                        require(currentBuffer.isNotEmpty())
                        tokens.add(Link(appName, currentBuffer.toString()))
                        currentBuffer.clear()
                        state = ParseState.IN_TEXT
                    } else {
                        currentBuffer.append(nextChar)
                    }
                }
            }
        }
        if (currentBuffer.isNotEmpty()) {
            require(state == ParseState.IN_TEXT)
            tokens.add(Text(currentBuffer.toString()))
        }
        return TokenList(tokens)
    }

    fun evaluate(token: ValueToken, resolver: PropertyFullResolver): String {
        return when (token) {
            is Link -> resolver.getConfProperty(token.appName, token.propertyName).getValue()
                ?: error("Dependency property is not found during resolving for token = $token")
            is Text -> token.str
            is TokenList -> token.tokens.joinToString("") { evaluate(it, resolver) }
        }
    }
}

sealed class ValueToken

data class TokenList(val tokens: List<ValueToken>) : ValueToken()
data class Text(val str: String) : ValueToken()
data class Link(val appName: String, val propertyName: String) : ValueToken()

private enum class ParseState {
    IN_TEXT,
    IN_LINK_START,
    IN_LINK_END
}


