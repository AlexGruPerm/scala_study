
C:\Users\Yakushev>cd C:\spark-2.3.0-bin-hadoop2.7\bin

C:\spark-2.3.0-bin-hadoop2.7\bin>spark-shell
2018-06-18 16:59:47 WARN  NativeCodeLoader:62 - Unable to load native-hadoop library for your platform... using builtin-java classes where applicable
Setting default log level to "WARN".
To adjust logging level use sc.setLogLevel(newLevel). For SparkR, use setLogLevel(newLevel).
Spark context Web UI available at http://PRM-WS-0006.MOON.LAN:4040
Spark context available as 'sc' (master = local[*], app id = local-1529323198472).
Spark session available as 'spark'.
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 2.3.0
      /_/

Using Scala version 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_161)
Type in expressions to have them evaluated.
Type :help for more information.

scala>

================================================================================================

import java.io.File
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
val warehouseLocation = new File("C:\\scala_study\\prq").getAbsolutePath
println(warehouseLocation)
val pDF =  spark.read.parquet(warehouseLocation)

pDF: org.apache.spark.sql.DataFrame = [id: int, name: string]

pDF.printSchema

scala> pDF.printSchema
root
 |-- id: integer (nullable = true)
 |-- name: string (nullable = true)

scala> pDF.show
+---+----+
| id|name|
+---+----+
|  1|  10|
|  2|  20|
|  3|  30|
|  4|  40|
|  5|  50|
|  6|  60|
|  7|  70|
|  8|  80|
|  9|  90|
| 10| 100|
| 11| 110|
| 12| 120|
| 13| 130|
| 14| 140|
| 15| 150|
| 16| 160|
| 17| 170|
| 18| 180|
| 19| 190|
| 20| 200|
+---+----+
only showing top 20 rows








