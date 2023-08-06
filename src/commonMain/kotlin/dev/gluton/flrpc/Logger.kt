package dev.gluton.flrpc

import korlibs.logger.Console
import korlibs.logger.Logger

val FLRpcLogger = Logger("FLRPC").apply {
    output = object : Logger.Output {
        override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
            val formatted = "${logger.name}/${level.name.lowercase().replaceFirstChar(Char::uppercaseChar)}: $msg"
            when (level) {
                Logger.Level.ERROR -> Console.error(formatted)
                Logger.Level.WARN -> Console.warn(formatted)
                else -> Console.log(formatted)
            }
        }
    }
}
