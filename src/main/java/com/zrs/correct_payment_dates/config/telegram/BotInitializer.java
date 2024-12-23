package com.zrs.correct_payment_dates.config.telegram;

import com.zrs.correct_payment_dates.exception_handling.GlobalExceptionHandler;
import com.zrs.correct_payment_dates.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@RequiredArgsConstructor
public class BotInitializer {

    private final TelegramBotService bot;
    private final GlobalExceptionHandler globalExceptionHandler;

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            globalExceptionHandler.handle(e);
        }
    }
}
