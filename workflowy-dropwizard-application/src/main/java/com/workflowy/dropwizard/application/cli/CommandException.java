package com.workflowy.dropwizard.application.cli;

public class CommandException extends Exception
{
    private final String code;

    public CommandException(String code, String message)
    {
        super(message);
        this.code = code;
    }

    public String getCode()
    {
        return this.code;
    }
}
