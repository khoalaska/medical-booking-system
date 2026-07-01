package fpt.medical.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    @GetMapping("/doctors")
    public String doctorList() {
        return "patient/doctor-list";
    }
}
