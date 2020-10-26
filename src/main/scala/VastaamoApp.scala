import java.io.File

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import com.github.tototoshi.csv._

object VastaamoApp extends App with ExecutionContext  {
  val blockchain = new BlockchainDataApi()
  val timeout = 10.minutes

  /**
   * Get bitcoin blocks between startTime and endTime
   *
   * @param startTime
   * @param endTime
   *
   * @return LazyList of Block summaries
   */
  def getBlocks(startTime: ZonedDateTime, endTime: ZonedDateTime) = LazyList.iterate(startTime)(_.plusDays(1))
    .map(time => Await.result(blockchain.blocks(time.toInstant.toEpochMilli), timeout))
    // Take chunks while they are in range.
    .takeWhile(blocks => blocks.blocks.headOption.exists(_.time < endTime.toInstant.getEpochSecond))
    // Flatten chunks to get BlockSummary
    .flatMap(_.blocks)
    // Filter blocks that are between startTime and endTime.
    .filter(block => block.time > startTime.toInstant.getEpochSecond && block.time < endTime.toInstant.getEpochSecond)

  // Ransomer started sending ransom notices around  7.30pm Finnish time allowing 24 hours to pay 200€.
  // Source: https://www.hs.fi/kotimaa/art-2000006698803.html
  val startTime = LocalDateTime.of(2020, 10, 24, 19, 30, 0, 0).atZone(ZoneId.of("Europe/Helsinki"))
  val endTime = LocalDateTime.of(2020, 10, 25, 20, 0, 0, 0).atZone(ZoneId.of("Europe/Helsinki"))

  // Approximately 200 € in bits during startTime and endTime.
  val valueRange =  (1700000 to 1900000)

  // Write suspected transactions to csv file.
  val csvWriter = CSVWriter.open(new File("./possible-transactions.csv"))
  csvWriter.writeRow(List("timestamp", "value", "total value", "out_address", "transaction")) // CSV Header

  // println("Ave, bitcoin!")

  var blocks = getBlocks(startTime, endTime)

  blocks.foreach(println)

  // Format block content to csv.
  blocks.foreach(block => {
    val possibleTransactions = Await
      // Querying blocks is pretty slow and should be cached ¯\_(ツ)_/¯.
      .result(blockchain.block(block.hash), timeout)
      // Get all (transaction, output) tuples where output value is in valueRange.
      .tx.flatMap(transaction => transaction.out.filter(out => valueRange.contains(out.value)).map(out => (transaction, out)))

    println(s" Block contains ${possibleTransactions.size} possible transactions")

    // Write to csv file
    possibleTransactions.foreach {
      case (tx, out) => csvWriter.writeRow(List(tx.time, out.value, tx.out.map(_.value).sum, out.addr, tx.hash).map(_.toString))
    }

  })

  // Cleanup
  csvWriter.close()
  blockchain.terminate()
  this.terminate()
}
