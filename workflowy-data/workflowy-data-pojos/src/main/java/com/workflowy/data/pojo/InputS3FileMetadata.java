package com.workflowy.data.pojo;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * S3 file attachment metadata.
 */
public record InputS3FileMetadata(
	@JsonProperty("isFile") Boolean isFile,

	@JsonProperty("fileName") @Nullable String fileName,

	@JsonProperty("fileType") @Nullable String fileType,

	@JsonProperty("objectFolder") @Nullable String objectFolder,

	@JsonProperty("isAnimatedGIF") @Nullable Boolean isAnimatedGIF,

	@JsonProperty("imageOriginalWidth") @Nullable Integer imageOriginalWidth,

	@JsonProperty("imageOriginalHeight") @Nullable Integer imageOriginalHeight,

	@JsonProperty("imageOriginalPixels") @Nullable Integer imageOriginalPixels
) {}
