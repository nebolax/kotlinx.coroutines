package chat

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.scheduling.ExperimentalCoroutineDispatcher
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import java.util.concurrent.ForkJoinPool

// Benchmarks configuration to monitor

/**
 * Underlying thread pool size for coroutines.
 */
val THREADS = listOf(4, 8, 16, 1)
/**
 * Chat users count.
 */
val USER_COUNT = listOf(10000)
/**
 * Maximum percentage of all chat users a user can be friends with.
 */
val MAX_FRIENDS_PERCENTAGE = listOf(0.2)
/**
 * Coroutines channels type.
 */
val CHANNEL_TYPES = listOf(ChannelType.UNLIMITED, ChannelType.RENDEZVOUS, ChannelType.BUFFERED_1, ChannelType.BUFFERED_16, ChannelType.BUFFERED_256)
/**
 * The average amount work that will be executed on CPU.
 */
val AVERAGE_WORK = listOf(80, 40)
/**
 * Implementations of users that contain different ways to find a user to send a message.
 */
val BENCHMARK_MODE = listOf(BenchmarkModes.USER_WITH_FRIENDS, BenchmarkModes.USER_WITHOUT_FRIENDS)
/**
 * Coroutines dispatcher types
 */
val DISPATCHER_TYPES = listOf(DispatcherTypes.EXPERIMENTAL, DispatcherTypes.FORK_JOIN)

// Benchmark configuration that are not shown in the output file

/**
 * Execution time of each benchmark.
 */
const val BENCHMARK_TIME_MS = 2000L
/**
 * Warm up iterations count
 */
const val WARM_UP_ITERATIONS = 5
/**
 * Benchmark iterations count
 */
const val BENCHMARK_ITERATIONS = 5
/**
 * CSV file containing the configurations and final metrics of the executed benchmarks
 */
const val BENCHMARK_OUTPUT_FILE = "results.csv"
/**
 * Folder containing all benchmark output files
 */
const val BENCHMARK_OUTPUT_FOLDER = "src/main/kotlin/chat/benchmarkresults/"
/**
 * The header for the output CSV file. It should be coordinated with [chat.BenchmarkConfiguration.toCSV] method.
 */
const val BENCHMARK_CONFIGURATION_FIELDS = "threads,userCount,maxFriendsPercentage,channelType,averageWork,benchmarkMode,dispatcherType,avgSentMessages,stdSentMessages,avgReceivedMessages,stdReceivedMessages"

/**
 * Class containing all the benchmark configuration and metrics of the run benchmark
 */
class BenchmarkConfiguration(
        var threads: Int,
        var users: Int,
        var maxFriendsPercentage: Double,
        var channelType: ChannelType,
        var averageWork: Int,
        var benchmarkMode: BenchmarkModes,
        var dispatcherType: DispatcherTypes
) {
    val sentMessagesPerRun = ArrayList<Double>()

    val receivedMessagesPerRun = ArrayList<Double>()

    override fun toString(): String {
        return "BenchmarkConfiguration(threads=$threads, users=$users, maxFriendsPercentage=$maxFriendsPercentage, channelType=$channelType, averageWork=$averageWork, benchmarkMode=$benchmarkMode, dispatcherType=$dispatcherType, sentMessagesPerRun=$sentMessagesPerRun, receivedMessagesPerRun=$receivedMessagesPerRun)"
    }

    fun configurationToString() : String {
        return "[threads=$threads, users=$users, maxFriendsPercentage=$maxFriendsPercentage, channelType=$channelType, averageWork=$averageWork, benchmarkMode=$benchmarkMode, dispatcherType=$dispatcherType]"
    }

    fun toCSV(): String {
        val standardDeviation = StandardDeviation()
        val avgSentMessages = sentMessagesPerRun.average()
        val avgReceivedMessages = receivedMessagesPerRun.average()
        val stdSentMessages = standardDeviation.evaluate(sentMessagesPerRun.toDoubleArray())
        val stdReceivedMessages = standardDeviation.evaluate(receivedMessagesPerRun.toDoubleArray())
        return "$threads,$users,$maxFriendsPercentage,$channelType,$averageWork,$benchmarkMode,$dispatcherType,%.2f,%.2f,%.2f,%.2f".format(avgSentMessages, stdSentMessages, avgReceivedMessages, stdReceivedMessages)
    }

    fun toArray() : Array<String> {
        return arrayOf(threads.toString(), users.toString(), maxFriendsPercentage.toString(), channelType.toString(), averageWork.toString(), benchmarkMode.toString(), dispatcherType.toString())
    }

    companion object {
        fun parseArray(array : Array<String>) : BenchmarkConfiguration {
            val threads = array[0].toInt()
            val userCount = array[1].toInt()
            val maxFriendsPercentage = array[2].toDouble()
            val channelType = ChannelType.valueOf(array[3])
            val averageWork = array[4].toInt()
            val benchmarkMode = BenchmarkModes.valueOf(array[5])
            val dispatcherType = DispatcherTypes.valueOf(array[6])
            return BenchmarkConfiguration(threads, userCount, maxFriendsPercentage, channelType, averageWork, benchmarkMode, dispatcherType)
        }

        fun defaultConfiguration() : BenchmarkConfiguration {
            return BenchmarkConfiguration(4, 10000, 0.2, ChannelType.UNLIMITED,
                    80, BenchmarkModes.USER_WITHOUT_FRIENDS, DispatcherTypes.FORK_JOIN)
        }
    }
}

enum class BenchmarkModes {
    USER_WITH_FRIENDS,
    USER_WITHOUT_FRIENDS
}

enum class ChannelType {
    RENDEZVOUS {
        override fun <T> createChannel(): Channel<T> {
            return Channel(Channel.RENDEZVOUS)
        }
    },
    UNLIMITED {
        override fun <T> createChannel(): Channel<T> {
            return Channel(Channel.UNLIMITED)
        }
    },
    BUFFERED_1 {
        override fun <T> createChannel(): Channel<T> {
            return Channel(1)
        }
    },
    BUFFERED_16 {
        override fun <T> createChannel(): Channel<T> {
            return Channel(16)
        }
    },
    BUFFERED_256 {
        override fun <T> createChannel(): Channel<T> {
            return Channel(256)
        }
    };

    abstract fun <T> createChannel() : Channel<T>
}

enum class DispatcherTypes(val create: (parallelism: Int) -> CoroutineDispatcher) {
    FORK_JOIN({ parallelism -> ForkJoinPool(parallelism).asCoroutineDispatcher() }),
    EXPERIMENTAL({ parallelism -> ExperimentalCoroutineDispatcher(corePoolSize = parallelism, maxPoolSize = parallelism) })
}

fun createBenchmarkConfigurationsList() : List<BenchmarkConfiguration> {
    val list = ArrayList<BenchmarkConfiguration>()
    for (threads in THREADS) {
        for (userCount in USER_COUNT) {
            for (maxFriendsPercentage in MAX_FRIENDS_PERCENTAGE) {
                for (channelType in CHANNEL_TYPES) {
                    for (tokens in AVERAGE_WORK) {
                        for (userType in BENCHMARK_MODE) {
                            for (dispatcherType in DISPATCHER_TYPES) {
                                list.add(
                                        BenchmarkConfiguration(
                                                threads,
                                                userCount,
                                                maxFriendsPercentage,
                                                channelType,
                                                tokens,
                                                userType,
                                                dispatcherType
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    return list
}