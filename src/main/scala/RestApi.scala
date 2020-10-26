import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

trait ExecutionContext {
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def terminate() = Http().shutdownAllConnectionPools().andThen {
    case _ => system.terminate()
  }
}

class RestApi(val endpoint: String, val headers: Seq[HttpHeader] = Seq()) extends ExecutionContext {

  /**
   * Send request
   *
   * @see: https://stackoverflow.com/a/43966056/12476997
   *
   * @param url API Url
   * @param requestType HTTP Method
   * @param entity Request body
   * @param m in-scope Unmarshaller
   * @tparam T Return type
   */
  protected def makeRequest[T: JsonFormat](url: String, requestType: HttpMethod = HttpMethods.GET, entity: RequestEntity = HttpEntity.Empty)(implicit m: Unmarshaller[HttpResponse, T]): Future[T] = {
    val uri = endpoint + url
    val request = HttpRequest(method = requestType, uri = uri, entity = entity, headers = this.headers)
    val response: Future[HttpResponse] = Http().singleRequest(request)

    println(s"${requestType} ${uri}")

    response.flatMap {
      case response @ HttpResponse(StatusCodes.OK, _, _, _) =>
        Unmarshal(response).to[T]
      case response => {
        println(s"Request failed with code ${response.status}")
        throw new Exception("The call failed!!")
      }
    }
  }
}
