package edu.iu.terracotta.controller.app;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/obsolete")
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ObsoleteController {

    @GetMapping("/assignment")
    public String assignment() {
        return "redirect:/app/app.html?obsolete=true&type=assignment";
    }

}
