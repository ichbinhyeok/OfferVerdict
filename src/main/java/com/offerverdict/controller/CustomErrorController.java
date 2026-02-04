package com.offerverdict.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return new ModelAndView("error/404");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                ModelAndView mav = new ModelAndView("error/500");
                mav.addObject("message", "Server is currently under heavy load. Please try again soon.");
                return mav;
            }
        }
        // Fallback
        return new ModelAndView("error/500");
    }
}
