package com.zrs.correct_payment_dates.exception_handling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Class {@code GlobalExceptionHandler} represents global exception handler of application.
 *
 * @author Roman Zaichenko
 * @version 1.0
 * @since 2024-12-13
 */
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    public void handle(Exception e) {
        System.err.println("Произошла ошибка: " + e.getMessage());
    }
}
