package fpt.medical.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveScheduleResultDTO {
    private boolean success;
    private String message;
}
