package fpt.medical.validator;

import fpt.medical.dto.AppointmentDTO;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class AppointmentValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return AppointmentDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        AppointmentDTO dto = (AppointmentDTO) target;
        // Custom validation logic will go here
    }
}
