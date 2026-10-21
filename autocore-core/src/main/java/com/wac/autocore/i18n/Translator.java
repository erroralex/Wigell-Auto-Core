package com.wac.autocore.i18n;

import java.util.Locale;

/**
 * <b>Translator</b>
 * <p>Ansvar: Översätter språknycklar till text på aktivt språk. Kontraktet för all
 * översättning i applikationen, utan beroende till JavaFX.</p>
 * <p>Saknad nyckel ger {@code !nyckel!} i stället för ett undantag, så att ett glömt
 * språkuttryck syns i gränssnittet utan att krascha det.</p>
 */
public interface Translator {

    String get(String key, Object... args);
    Locale getLocale();
    void setLocale(Locale locale);
}
