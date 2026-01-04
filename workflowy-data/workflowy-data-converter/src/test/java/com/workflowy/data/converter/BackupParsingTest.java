package com.workflowy.data.converter;

import java.io.File;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflowy.data.pojo.InputItem;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Uses real backup files not in VCS")
class BackupParsingTest
{
    private static final String BACKUPS_PATH = "/Users/craig/projects/workflowy/backups/Data";

    @Test
    void parseBackupFile() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        File backupsDir = new File(BACKUPS_PATH);
        File[] backupFiles = backupsDir.listFiles((dir, name) -> name.endsWith(".workflowy.backup"));
        assertNotNull(backupFiles, "No backup files found");
        assertTrue(backupFiles.length > 0, "No backup files found");

        for (File backupFile : backupFiles)
        {
            System.out.println("Parsing: " + backupFile.getName());
            List<InputItem> items = objectMapper.readValue(backupFile, new TypeReference<>() {});
            System.out.println("  Parsed " + items.size() + " root items");
        }
    }
}
