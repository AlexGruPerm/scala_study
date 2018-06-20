import com.sksamuel.avro4s.RecordFormat
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.apache.hadoop.fs.Path
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetWriter}
import scala.util.Random

case class Person(id :Int,name: String)

object ParquetWriteRead extends App {

  val logger = LoggerFactory.getLogger(this.getClass)
  val format = RecordFormat[Person]

  def writeToFile[T](data: Iterable[T], schema: Schema, path: String): Unit = {
    val writer = AvroParquetWriter.builder[T](new Path(path))
      .withSchema(schema)
      .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
      .build()
      .asInstanceOf[ParquetWriter[T]]

    data.foreach(x => writer.write(format.to(x.asInstanceOf[Person]).asInstanceOf[T]))
    writer.close()
  }

  def readFromFile(path: String) = {
    val conf = new Configuration
    val file = HadoopInputFile.fromPath(new Path(path), conf)
    val reader = AvroParquetReader.builder[GenericRecord](file)
      .build()
    val iter = Iterator.continually(reader.read).takeWhile(_ != null).map(format.from).toList
    iter
  }



  val personData = for(i <- 1 to 10) yield {
    new Person(i,Random.alphanumeric.take(10).mkString)
  }

  val schema = SchemaBuilder
    .record("PersonRecord")
    .fields()
    .optionalInt("id")
    .optionalString("name")
    .endRecord()

  writeToFile(personData,schema,"prq/test.parquet")
  val rows = readFromFile("prq/test.parquet")

  println("rows.size="+rows.size)
  rows.foreach(p => println("id:"+p.id+" name:"+p.name))

}
