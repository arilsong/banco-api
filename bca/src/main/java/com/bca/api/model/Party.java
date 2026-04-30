package com.bca.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Party {
    private String idType;
    private String idValue;
    private String displayName;
    private String firstName;
    private String lastName;
    private String fspId;
    private String dateOfBirth;
    private List<Map<String, String>> extensionList;
}

