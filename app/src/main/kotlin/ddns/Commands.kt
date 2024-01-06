package ddns

import kotlinx.cli.ArgType
import kotlinx.cli.ParsingException
import kotlinx.cli.Subcommand
import kotlinx.cli.required
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

abstract class ArgTypeExtended<T : Any>(hasParameter: kotlin.Boolean) : ArgType<T>(hasParameter) {
    object Path : ArgType<java.nio.file.Path>(true) {
        override val description: kotlin.String
            get() = "{ java.nio.file.Path }"

        override fun convert(value: kotlin.String, name: kotlin.String): java.nio.file.Path =
            java.nio.file.Paths.get(value)
    }

    object ReadablePath : ArgType<java.nio.file.Path>(true) {
        override val description: kotlin.String
            get() = "{ java.nio.file.Path }"

        override fun convert(value: kotlin.String, name: kotlin.String): java.nio.file.Path {
            val path = java.nio.file.Paths.get(value)
            if (path.isReadable()) {
                return path
            }
            throw ParsingException("Option $name is expected to be a path to a readable file. $value can't be read.")
        }
    }

    object WritablePath : ArgType<java.nio.file.Path>(true) {
        override val description: kotlin.String
            get() = "{ java.nio.file.Path }"

        override fun convert(value: kotlin.String, name: kotlin.String): java.nio.file.Path {
            val path = java.nio.file.Paths.get(value)
            if (path.isReadable() && path.isWritable()) {
                return path
            }
            throw ParsingException("Option $name is expected to be a path to a read-and-writable file. $value can't be read.")
        }
    }
}

abstract class CommonSubcommand(name: String, actionDescription: String) :
    Subcommand(name, actionDescription) {
    val config by option(
        ArgTypeExtended.ReadablePath,
        "config",
        "c",
        "Configuration file"
    ).required()
}