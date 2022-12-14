package com.sam.ross.smoovie.objects.subtitles;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitlesSearchResponse {
    private String total_pages;
    private String total_count;
    private String per_page;
    private String page;
    private JsonNode data;
}
