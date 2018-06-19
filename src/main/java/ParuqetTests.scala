import com.sksamuel.avro4s.RecordFormat
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory
import org.apache.hadoop.fs.Path
import org.apache.avro.{Schema, SchemaBuilder}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.{IntWritable, WritableComparable}
import org.apache.parquet.avro.AvroParquetReader
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.format.converter.ParquetMetadataConverter
import org.apache.parquet.hadoop.util.HadoopInputFile
import org.apache.parquet.hadoop.{ParquetFileWriter, ParquetReader, ParquetWriter}
import org.apache.parquet.io.{InputFile, SeekableInputStream}

//case class Person(id:Int,name: Int)

/*

  object Person {
    def id: Int = 0
    def name: Int = 0
    def apply(id:Int,name: Int): Person = new Person(id, name)
  }

*/

case class Person(id:Int,name: Int)


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


  def readFromFile(path: String) = {

    val conf = new Configuration
    val file = HadoopInputFile.fromPath(new Path(path), conf);

/*
BASIC EXAMPLE -------------------------------------

val reader = AvroParquetReader.builder[GenericRecord](path).build().asInstanceOf[ParquetReader[GenericRecord]]

val iter = Iterator.continually(reader.read).takeWhile(_ != null)

val list = iter.toList

case class Bibble(name: String, location: String)

val format = RecordFormat[Bibble]

val bibble = format.from(record)


val reader = AvroParquetReader.builder[GenericRecord](path).build().asInstanceOf[ParquetReader[GenericRecord]]
val format = RecordFormat[Bibble]
// iter is now an Iterator[Bibble]
val iter = Iterator.continually(reader.read).takeWhile(_ != null).map(format.from)
// and list is now a List[Bibble]
val list = iter.toList
*/

   // val reader = AvroParquetReader.builder[GenericRecord](file).build().asInstanceOf[ParquetReader[GenericRecord]]

    val reader = AvroParquetReader.builder[GenericRecord](file).build()//.asInstanceOf[ParquetReader[GenericRecord]]

    //val format = RecordFormat[Person]
    //val iter = Iterator.continually(reader.read).takeWhile(_ != null).map(x =>  format.from(x.asInstanceOf[GenericRecord]))//.map(x => format.from(x.asInstanceOf[GenericRecord])).toList

    var r: GenericRecord = reader.read()

    //val iter = Iterator.continually(reader.read).takeWhile(_ != null).toList
    //println("iter.size="+iter.size)


    //.foreach(r => println(r))

    //Iterator.continually(reader.read)/*.takeWhile(_ != null)*/.foreach(x => println("1")/*x.getClass.getName)*/)
    /*
    val iter = Iterator.continually(reader.read).takeWhile(_ != null).map(x => format.from(x.asInstanceOf[GenericRecord]).asInstanceOf[Person])
    val list = iter.toList
    list
    */
  }




/*
  val schema = SchemaBuilder
    .record("Person")
    .fields()
    .optionalInt("id")
    .optionalInt("name")
    .endRecord()
  val personData = for(i <- 1 to 1000000) yield {new Person(i,123.toInt)}
  writeToFile(personData,schema,"prq/test.parquet")
*/

/*
  val schemaRead = SchemaBuilder
    .record("Person")
    .fields()
    .optionalInt("id")
    .optionalInt("name")
    .endRecord()
*/

  readFromFile(/*schemaRead,*/"prq/test.parquet")


}
