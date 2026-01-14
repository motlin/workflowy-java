package com.workflowy.data.pojo;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Mirror metadata indicating this item is a mirror of another item.
 */
public record InputMirrorMetadata(
	@Nullable String originalId,

	@Nullable Boolean isMirrorRoot,

	@Nullable Map<String, Boolean> mirrorRootIds,

        @Nullable
        Map<String, Boolean> backlinkMirrorRootIds
)
{
    public Set<String> getMirrorSourceIds()
    {
        if (mirrorRootIds != null)
        {
            return mirrorRootIds.keySet();
        }
        return Set.of();
    }

    public Set<String> getBacklinkMirrorIds()
    {
        if (backlinkMirrorRootIds != null)
        {
            return backlinkMirrorRootIds.keySet();
        }
        return Set.of();
    }
}
