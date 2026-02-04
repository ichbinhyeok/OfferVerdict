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

    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoHandler(org.springframework.web.servlet.NoHandlerFoundException ex) {
        return new ModelAndView("error/404");
    }

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

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return new ModelAndView("error/404");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneric(Exception ex, jakarta.servlet.http.HttpServletResponse response) {
        // Safety check: If it's a 404-type exception, return 404 immediately
        if (ex instanceof org.springframework.web.servlet.NoHandlerFoundException ||
                ex instanceof org.springframework.web.servlet.resource.NoResourceFoundException) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return new ModelAndView("error/404");
        }

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
