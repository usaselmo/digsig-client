package br.mil.eb.sermil.assinatura.client.util;

import java.util.Locale;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.3-GO
 * @since 1.1
 */
@Slf4j
@Component
public class Messages {

  @Autowired
  private MessageSource messageSource;

  private MessageSourceAccessor accessor;

  private static final String DEFAULT_MSG = "Erro indefinido";

  @PostConstruct
  private void init() {
    accessor = new MessageSourceAccessor(messageSource, Locale.forLanguageTag("pt-br"));
    log.info("MessageSourceAccessor iniciado: {}", accessor.getClass().getName());
  }

  public String get(String code) {
    return accessor.getMessage(code, DEFAULT_MSG);
  }

  public String get(String code, String... args) {
    return accessor.getMessage(code, new Object[] { args }, DEFAULT_MSG);
  }

}