import com.sksamuel.avro4s.RecordFormat
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.apache.hadoop.fs.Path
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.format.converter.ParquetMetadataConverter
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetReader, ParquetWriter}
import org.apache.parquet.io.{InputFile, SeekableInputStream}

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


  def readFromFile(schema: Schema, path: String) = {

    val configuration = new Configuration
    val file = HadoopInputFile.fromPath(new Path(path), configuration);

    val reader = AvroParquetReader.builder[Person](file).build().asInstanceOf[ParquetReader[Person]]

    val format = RecordFormat[Person]

    //val iter = Iterator.continually(reader.read).takeWhile(_ != null).map(format.from)

    Iterator.continually(reader.read).takeWhile(_ != null).foreach(x => println(x.getClass.getName))
    /*
    val iter = Iterator.continually(reader.read).takeWhile(_ != null).map(x => format.from(x.asInstanceOf[GenericRecord]).asInstanceOf[Person])
    val list = iter.toList
    list
    */
  }

  val schema = SchemaBuilder
    .record("Person")
    .fields()
    .requiredInt("id")
    .requiredString("name")
    .endRecord()

    //val personData = for(i <- 1 to 1000000) yield {new Person(i,(i*10).toString)}
    //writeToFile(personData,schema,"prq/test.parquet")

     readFromFile(schema,"prq/test.parquet")


}
