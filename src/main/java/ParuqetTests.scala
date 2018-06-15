import org.slf4j.LoggerFactory
import org.apache.hadoop.fs.Path
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetWriter}

case class Person(id:Int,name: String)

object ParuqetTests extends App {
  val logger = LoggerFactory.getLogger(this.getClass)
  println("Hello")

  def writeToFile[Person](data: Iterable[Person], schema: Schema, path: String): Unit = {

    val sPath = new Path(path)

    val writer = AvroParquetWriter.builder[Person](sPath)
      //.withConf(conf)
      .withSchema(schema)
      .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
      .build()
      .asInstanceOf[ParquetWriter[Person]]

    data.foreach(x => println(x+" type="+x.getClass.getName))
    // data.foreach(writer.write)
    // writer.close()
  }

  val schema = SchemaBuilder
    .record("Person")
    .fields()
    .requiredInt("id")
    .requiredString("name")
    .endRecord()

  val personData = Seq(Person(1,"A"),Person(2,"B"))

  writeToFile(personData,schema,"prq/test.parquet")
}
