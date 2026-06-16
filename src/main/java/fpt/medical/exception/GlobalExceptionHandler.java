package fpt.medical.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ModelAndView handleUnauthorized(UnauthorizedException ex) {
        ModelAndView mav = new ModelAndView("error/403");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex) {
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", ex.getMessage());
        return mav;
    }
}
