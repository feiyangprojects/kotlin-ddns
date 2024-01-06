package ddns

import io.ktor.client.HttpClient
import kotlinx.cli.ArgParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.io.path.readText

object App {
    const val Name = "ddnskt"

    val HttpClient = HttpClient()
}

fun main(args: Array<String>) {
    val argParser = ArgParser(App.Name)
    val logger = LoggerFactory.getLogger(App.Name)

    class Check : CommonSubcommand("check", "Check DDNS updating tasks") {
        override fun execute() {
            Json.decodeFromString<Configuration>(config.readText())
        }
    }

    class Run : CommonSubcommand("run", "Run DDNS updating tasks") {
        override fun execute() {
            val config = Json.decodeFromString<Configuration>(config.readText())

            runBlocking {
                do {
                    config.records.map {
                        async {
                            try {
                                val address = it.address.match()

                                logger.info("${it.name} - Get local address $address")
                                if (it.get().none { it == address }) {
                                    logger.info("${it.name} - Currently outdated or missing")
                                    if (it.update(address)) {
                                        logger.info("${it.name} - Updated to $address")
                                    }
                                } else {
                                    logger.info("${it.name} - currently up to date")
                                }
                            } catch (e: Exception) {
                                if (e.message != null) {
                                    logger.error("${it.name} - ${e.message}")
                                } else {
                                    logger.error("${it.name} - ${e.javaClass.simpleName} occurred")
                                }

                            }
                        }
                    }.awaitAll()

                    delay(config.interval)
                } while (config.mode == Mode.SIMPLE)
            }
        }
    }

    argParser.subcommands(Run())
    argParser.parse(args)
}
