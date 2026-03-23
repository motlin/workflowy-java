package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateNodeRequest(@Nullable String name, @Nullable String note, @Nullable String layoutMode) {}
