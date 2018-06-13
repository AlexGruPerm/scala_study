import java.io._
import play._
import play.api.libs.json.{JsValue, Json, JsPath, Reads}
import play.Application

//Original question: https://stackoverflow.com/questions/25432588/why-do-you-need-to-create-these-json-read-write-when-in-java-you-didnt-have-to

//Docs: https://www.playframework.com/documentation/2.6.x/ScalaJson

case class oneConf(id: Int, pair: String, file_path: String)

case class properties(data_sources: Seq[oneConf])

object properties {
  import play.api.libs.functional.syntax._

  implicit val oneConfV: Reads[oneConf] = (
      (JsPath \ "id").read[Int] and
      (JsPath \ "pair").read[String] and
      (JsPath \ "file_path").read[String]
    )(oneConf.apply _)

  implicit val propertiesV: Reads[properties] = Json.reads[properties]

}

object TestJSON extends App {

  val stream = new FileInputStream("conf/conf.json")
  val json = try {  Json.parse(stream) } finally { stream.close() }
  val r = json.as[properties]

  for(conf <- r.data_sources.toSeq){
    println(conf.id+"  "+conf.pair+"  "+conf.file_path)
  }

}
