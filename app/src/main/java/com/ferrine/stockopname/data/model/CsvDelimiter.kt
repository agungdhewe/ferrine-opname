package com.ferrine.stockopname.data.model

enum class CsvDelimiter(
    val character: Char,
    val displayName: String
) {
    COMMA(
        character = ',',
        displayName = "Comma (,)"
    ),
    SEMICOLON(
        character = ';',
        displayName = "Semicolon (;)"
    ),
    PIPE(
        character = '|',
        displayName = "Pipe (|)"
    ),
    TAB(
        character = '\t',
        displayName = "Tab"
    );
}