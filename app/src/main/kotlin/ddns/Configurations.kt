package ddns


import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException

private object Common {
    val RECORD_ADDRESS_REGEX_IPV6_ZERO = Regex("((?::0){2,})(?!.*$1:0):?(.*)")

    // DynDNS API documentation: https://help.dyn.com/remote-access-api/
    // Suffix `.*` is compatibility measure No-IP.com
    val RECORD_BASIC_REGEX_SUCCESS = Regex("^(?:good|nochg).*")
    val RECORD_BASIC_REGEX_ERROR = Regex("^(?:nohost|badauth|badagent|!donator|abuse)")
}

@Serializable
class Configuration(
    val mode: Mode,
    val interval: Long = 0,
    val records: Array<Record>
)

@Serializable
enum class Mode {
    @SerialName("oneshot")
    ONESHOT,

    @SerialName("simple")
    SIMPLE
}

@Serializable
sealed class Record {
    abstract val name: String
    abstract val address: RecordAddress

    fun get() = sequence<String> {
        try {
            for (inetAddress in InetAddress.getAllByName(name)) {
                yield(inetAddress.hostAddress)
            }
            // Ignore UnknownHostException for possible(?) empty record
        } catch (_: UnknownHostException) {
        }
    }

    abstract suspend fun update(address: String): Boolean
}

class RecordException(message: String) : Exception(message)

@Serializable
sealed class RecordAddress {
    fun get() = sequence {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()

        while (networkInterfaces.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            val inetAddresses = networkInterface.inetAddresses
            while (inetAddresses.hasMoreElements()) {
                yield(
                    // Remove interface names from IPv6 addresses
                    (inetAddresses.nextElement()).hostAddress.substringBefore('%')
                        // Compress repeated zeros of IPv6 addresses according to rfc5952
                        .replace(Common.RECORD_ADDRESS_REGEX_IPV6_ZERO, "::$2")
                )
            }
        }
    }

    @Throws(RecordAddressException::class)
    abstract suspend fun match(): String
}

class RecordAddressException(message: String) : Exception(message)

@Serializable
@SerialName("basic")
class RecordAddressBasic(private val endpoint: String) : RecordAddress() {
    @Throws(RecordAddressException::class)
    override suspend fun match(): String {
        val response = App.HttpClient.request(endpoint)

        if (response.status == HttpStatusCode.OK) {
            return response.body<String>()
        }

        throw RecordAddressException("Endpoint isn't available")
    }
}

@Serializable
@SerialName("regex")
class RecordAddressRegexp(private val pattern: String) : RecordAddress() {
    @kotlinx.serialization.Transient
    private val regex = Regex(pattern)

    @Throws(RecordAddressException::class)
    override suspend fun match(): String {
        for (inetAddress in get()) {
            if (inetAddress.matches(regex)) {
                return inetAddress
            }
        }

        throw RecordAddressException("Address with matching condition can't be found")
    }
}


@Serializable
@SerialName("basic")
class RecordBasic(
    override val name: String,
    override val address: RecordAddress,
    private val username: String,
    private val password: String,
    private val endpoint: String,
    @SerialName("name_parameter")
    private val nameParameter: String,
    @SerialName("address_parameter")
    private val addressParameter: String,
    @SerialName("other_parameters")
    private val otherParameters: Map<String, String>? = null
) : Record() {
    @Throws(RecordException::class)
    override suspend fun update(address: String): Boolean {
        val response = App.HttpClient.request(endpoint) {
            headers {
                basicAuth(username, password)
            }
            url {
                parameters.append(nameParameter, name)
                parameters.append(addressParameter, address)
                // These parameters are compatibility measures for deSEC.io and DuckDNS.org
                otherParameters?.map {
                    parameters.append(it.key, it.value)
                }
            }
        }
        val body = response.body<String>()

        if (response.status == HttpStatusCode.OK && body.matches(Common.RECORD_BASIC_REGEX_SUCCESS)) {
            return true
        } else if (body.matches(Common.RECORD_BASIC_REGEX_ERROR)) {
            throw RecordException("Update failed due to $body")
        }

        throw RecordException("Endpoint isn't available")
    }
}