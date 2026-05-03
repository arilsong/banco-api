package com.bni.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

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
    private String type;
    private String displayName;
    private String firstName;
    private String lastName;
    private String fspId;
    private String dateOfBirth;
    private List<Map<String, String>> extensionList;
}
