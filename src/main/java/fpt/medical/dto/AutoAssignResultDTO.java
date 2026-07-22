package fpt.medical.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoAssignResultDTO {
    private int createdCount;
    private List<String> unfilledWarnings;
}
