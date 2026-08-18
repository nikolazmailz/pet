package com.pet.common.reqreply.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessagePostRequestDto {
    private String requestUID;
    private String messageName;
    private String messageContent;
}
