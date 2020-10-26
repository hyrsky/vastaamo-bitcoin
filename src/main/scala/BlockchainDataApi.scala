import akka.http.scaladsl.model._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.Unmarshaller
import spray.json._

final case class BlockSummary(val height: Int,
                              val hash: String,
                              val time: Long,
                              val main_chain: Boolean)

final case class Blocks(val blocks: List[BlockSummary])

/**
 * @see https://en.bitcoin.it/wiki/Transaction
 */
final case class TransactionInput(val sequence: Int,
                                  val witness: String,
                                  val script: String,
                                  val index: Int)

/**
 * @see https://en.bitcoin.it/wiki/Transaction
 */
final case class TransactionOutput(val `type`: Int,
                                   val spent: Boolean,
                                   val value: Int,
                                   val addr: String,
                                   val n: Int,
                                   val tx_index: Int,
                                   val script: String)

/**
 * @see https://en.bitcoin.it/wiki/Transaction
 */
final case class Transaction(val hash: String,
                             val ver: Int,
                             val vin_sz: Int,
                             val vout_sz: Int,
                             val size: Int,
                             val weight: Int,
                             val fee: Int,
                             val relayed_by: String,
                             val lock_time: Int,
                             val tx_index: Int,
                             val double_spend: Boolean,
                             val result: Int,
                             val balance: Int,
                             val time: Long,
                             val block_index: Int,
                             val block_height: Int,
                             val inputs: List[TransactionInput],
                             val out: List[TransactionOutput])

/**
 * @see https://en.bitcoin.it/wiki/Block
 */
final case class Block(val hash: String,
                       val ver: Int,
                       val prev_block: String,
                       val mrkl_root: String,
                       val time: Long,
                       val bits: Int,
                       val nonce: Int,
                       val fee: Int,
                       val n_tx: Int,
                       val size: Int,
                       val block_index: Int,
                       val main_chain: Boolean,
                       val height: Int,
                       val weight: Int,
                       val tx: List[Transaction])

// collect your json format instances into a support trait:
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val blockSummaryFormat = jsonFormat4(BlockSummary)
  implicit val blocksFormat = jsonFormat1(Blocks)
  implicit val blockInputFormat = jsonFormat4(TransactionInput)
  implicit val blockOutputFormat = jsonFormat7(TransactionOutput)
  implicit val transactionFormat = jsonFormat18(Transaction)
  implicit val blockFormat = jsonFormat15(Block)

}

/**
 * Blockchain.com Blockchain Data API
 *
 * @see https://www.blockchain.com/api/blockchain_api
 */
class BlockchainDataApi extends RestApi("https://blockchain.info") with JsonSupport {
  /**
   * Get single block
   *
   * Warning: This is pretty slow
   *
   * @param hash block hash
   */
  def block(hash: String) = {
    this.makeRequest[Block](s"/rawblock/${hash}")
  }

  /**
   * Get blocks for one day
   *
   * @param timeInMilliseconds day timestamp in millisecond
   */
  def blocks(timeInMilliseconds: Long) = {
    this.makeRequest[Blocks](s"/blocks/${timeInMilliseconds}")
  }

  protected override def makeRequest[T: JsonFormat](url: String, requestType: HttpMethod, entity: RequestEntity)(implicit m: Unmarshaller[HttpResponse, T]) = {
    super.makeRequest[T](s"${url}?format=json", requestType, entity)
  }
}
