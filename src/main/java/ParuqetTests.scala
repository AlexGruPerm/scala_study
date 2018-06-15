import com.sksamuel.avro4s.RecordFormat
import org.slf4j.LoggerFactory
import org.apache.hadoop.fs.Path
import org.apache.avro.{Schema, SchemaBuilder}
//import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetWriter}

case class Person(id:Int,name: String)

object ParuqetTests extends App {
  val logger = LoggerFactory.getLogger(this.getClass)

  def writeToFile[T](data: Iterable[T], schema: Schema, path: String): Unit = {

    //val sPath = new Path(path)
    val writer = AvroParquetWriter.builder[T](new Path(path))
      .withSchema(schema)
      .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
      .build()
      .asInstanceOf[ParquetWriter[T]]

    val format = RecordFormat[Person]
      data.foreach(x => writer.write(format.to(x.asInstanceOf[Person]).asInstanceOf[T]))
      writer.close()
  }

  val schema = SchemaBuilder
    .record("Person")
    .fields()
    .requiredInt("id")
    .requiredString("name")
    .endRecord()

  val personData = for(i <- 1 to 1000000) yield {new Person(i,(i*10).toString)}

    writeToFile(personData,schema,"prq/test.parquet")
}
