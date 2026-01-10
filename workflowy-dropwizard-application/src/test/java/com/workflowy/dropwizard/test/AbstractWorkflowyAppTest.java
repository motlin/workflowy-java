package com.workflowy.dropwizard.test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;

import com.workflowy.dropwizard.application.WorkflowyApplication;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.liftwizard.dropwizard.testing.junit.AbstractDropwizardAppTest;
import io.liftwizard.junit.extension.app.LiftwizardAppExtension;
import io.liftwizard.junit.extension.match.file.FileMatchExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class AbstractWorkflowyAppTest extends AbstractDropwizardAppTest
{
    protected static final Instant START_TIME = Instant.parse("2000-12-31T23:59:59Z");

    @RegisterExtension
    public final FileMatchExtension fileMatchExtension = new FileMatchExtension(this.getClass());

    @Nonnull
    @Override
    protected LiftwizardAppExtension<?> getDropwizardAppExtension()
    {
        return new LiftwizardAppExtension<>(
                WorkflowyApplication.class,
                ResourceHelpers.resourceFilePath("config-test.json5"),
                ConfigOverride.config("clock.instant", this.getClockTime().toString()));
    }

    protected Instant getClockTime()
    {
        return START_TIME.plus(this.advanceClockNDays(), ChronoUnit.DAYS);
    }

    protected int advanceClockNDays()
    {
        return 0;
    }
}
