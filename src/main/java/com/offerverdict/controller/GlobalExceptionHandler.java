package com.offerverdict.controller;

import com.offerverdict.exception.BadRequestException;
import com.offerverdict.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBadRequest(BadRequestException ex) {
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex, jakarta.servlet.http.HttpServletResponse response) {
        // Log the full stack trace for debugging
        logger.error("Internal Server Error (Potential Resource Issue): ", ex);

        // SEO SAFETY: Instead of 500, return 503 (Service Unavailable)
        // This tells GoogleBot "I'm busy, come back in an hour" instead of "I'm
        // broken".
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader("Retry-After", "3600"); // 1 hour

        ModelAndView mav = new ModelAndView("error/500"); // Still use 500 template
        mav.addObject("message", "Server is currently under heavy load. Please try again soon.");
        return mav;
    }
}
