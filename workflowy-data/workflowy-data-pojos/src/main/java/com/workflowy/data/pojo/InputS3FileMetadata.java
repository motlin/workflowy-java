package com.workflowy.data.pojo;

import javax.annotation.Nullable;

public record InputS3FileMetadata(
	Boolean isFile,

	@Nullable String fileName,

	@Nullable String fileType,

	@Nullable String objectFolder,

	@Nullable Boolean isAnimatedGIF,

	@Nullable Integer imageOriginalWidth,

	@Nullable Integer imageOriginalHeight,

	@Nullable Integer imageOriginalPixels
) {}
