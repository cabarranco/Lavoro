package com.asbresearch.sofascore.inplay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class SofaScoreTeam {
    String name;
    String slug;
    String nameCode;
    boolean national;
    long id;

    @JsonCreator(mode = PROPERTIES)
    public SofaScoreTeam(@JsonProperty("name") String name,
                         @JsonProperty("slug") String slug,
                         @JsonProperty("nameCode") String nameCode,
                         @JsonProperty("national") boolean national,
                         @JsonProperty("id") long id) {
        this.name = name;
        this.slug = slug;
        this.nameCode = nameCode;
        this.national = national;
        this.id = id;
    }

}
