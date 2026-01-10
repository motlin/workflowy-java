package com.workflowy.dropwizard.application.cli;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

public class JsonPrettyPrinter extends DefaultPrettyPrinter
{
    private static final DefaultIndenter TWO_SPACE_INDENTER =
            new DefaultIndenter("  ", DefaultIndenter.SYS_LF);

    public JsonPrettyPrinter()
    {
        this._arrayIndenter = TWO_SPACE_INDENTER;
        this._objectIndenter = TWO_SPACE_INDENTER;
        this._separators = Separators.createDefaultInstance()
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator("");
        this._objectEmptySeparator = "";
        this._arrayEmptySeparator = "";
    }

    @Override
    public DefaultPrettyPrinter createInstance()
    {
        return this;
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException
    {
        g.writeRaw(this._separators.getObjectFieldValueSeparator() + " ");
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException
    {
        super.writeEndObject(g, nrOfEntries);
        if (this._nesting == 0)
        {
            g.writeRaw(DefaultIndenter.SYS_LF);
        }
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException
    {
        super.writeEndArray(g, nrOfValues);
        if (this._nesting == 0)
        {
            g.writeRaw(DefaultIndenter.SYS_LF);
        }
    }
}
