package com.workflowy.dropwizard.application.cli;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.fusesource.jansi.Ansi;

public final class JsonSyntaxHighlighter
{
    private JsonSyntaxHighlighter()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    public static String highlight(String json) throws IOException
    {
        StringBuilder result = new StringBuilder();
        JsonFactory factory = new JsonFactory();

        try (JsonParser parser = factory.createParser(json))
        {
            int jsonIndex = 0;
            JsonToken token;

            while ((token = parser.nextToken()) != null)
            {
                long tokenStart = parser.currentTokenLocation().getCharOffset();

                if (tokenStart > jsonIndex)
                {
                    result.append(json, jsonIndex, (int) tokenStart);
                }

                String tokenText = parser.getText();
                String coloredText = colorizeToken(token, tokenText);
                result.append(coloredText);

                jsonIndex = (int) tokenStart + getTokenLength(token, tokenText, json, (int) tokenStart);
            }

            if (jsonIndex < json.length())
            {
                result.append(json.substring(jsonIndex));
            }
        }

        return result.toString();
    }

    private static String colorizeToken(JsonToken token, String text)
    {
        Ansi ansi = Ansi.ansi();

        switch (token)
        {
            case FIELD_NAME:
                return ansi.fgCyan().a('"').a(text).a('"').reset().toString();

            case VALUE_STRING:
                return ansi.fgGreen().a('"').a(text).a('"').reset().toString();

            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
                return ansi.fgBlue().a(text).reset().toString();

            case VALUE_TRUE:
            case VALUE_FALSE:
                return ansi.fgYellow().a(text).reset().toString();

            case VALUE_NULL:
                return ansi.fgMagenta().a(text).reset().toString();

            default:
                return text;
        }
    }

    private static int getTokenLength(JsonToken token, String tokenText, String json, int start)
    {
        switch (token)
        {
            case FIELD_NAME:
            case VALUE_STRING:
                return tokenText.length() + 2;

            case START_OBJECT:
            case END_OBJECT:
            case START_ARRAY:
            case END_ARRAY:
                return 1;

            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NULL:
                return tokenText.length();

            default:
                return tokenText.length();
        }
    }
}
