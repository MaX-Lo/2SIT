package de.twoSIT.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import de.twoSIT.const.defaultLogFile
import de.twoSIT.const.logDir
import org.slf4j.LoggerFactory
import java.io.File


fun getLogger(cls: Class<*>, file: String = defaultLogFile): Logger {
    val logger: Logger = LoggerFactory.getLogger(cls) as Logger
    logger.setLevel(Level.INFO)
    logger.setAdditive(false) /* set to true if root should log too */

    val lc = LoggerFactory.getILoggerFactory() as LoggerContext
    val ple = PatternLayoutEncoder()
    ple.pattern = "%date %level %logger{10} [%file:%line] %msg%n"
    ple.context = lc
    ple.start()

    val consoleAppender = ConsoleAppender<ILoggingEvent>()
    consoleAppender.setEncoder(ple)
    consoleAppender.context = lc
    consoleAppender.start()
    logger.addAppender(consoleAppender)

    val dirName = logDir
    File(dirName).mkdir()
    val fileAppender = FileAppender<ILoggingEvent>()
    fileAppender.file = "$dirName/$file"
    fileAppender.setEncoder(ple)
    fileAppender.context = lc
    fileAppender.start()
    logger.addAppender(fileAppender)

    return logger
}