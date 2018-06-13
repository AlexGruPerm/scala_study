import akka.actor._

object TestCollectFunctions extends App {

  def test_parts_simple: Unit = {
    val v: Seq[Int] = for (i <- 1 to 10) yield i
    println(v)

    def get_rest(acc: Int, s: Seq[Int], limitV: Int): Seq[Int] = {
      if (acc + s.head < limitV) get_rest(acc + s.head, s.tail, limitV)
      else s.tail
    }

    val r: Seq[Int] = get_rest(0, v, 10)

    println(r)
  }

  def test_divide_seq_on_bars: Unit = {
    val src: Seq[Int] = for (i <- 1 to 95) yield i
    println("src=" + src)
    println(" ")
    val barSize: Int = 10
    val bars: Seq[Seq[Int]] = src.sliding(barSize, barSize).filter(x => (x.size == barSize)).toSeq
    println("bars.size=" + bars.size)
    bars.foreach(println)

  }

  def test_zip: Unit = {
    val s1: Seq[String] = Seq("a", "b", "c")
    val s2: Seq[Int] = Seq(3, 4, 5)

    for (x <- s1.zip(s2)) println(x._1 + "  " + x._2)

  }

  def test_dw: Unit = {
    val s: Seq[Int] = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9)
    println(s.partition(x => (x < 5))._2)
    println(s.dropWhile(x => (x < 5)))
    println(s.span(x => (x < 5))._2)
  }

  //test_parts_simple
  //test_divide_seq_on_bars
  //test_zip
  //test_dw


  def test_recursive_search: Unit = {
    //for(i <- 1 to 10) println("("+i+","+math.round(math.random()*10)+"),")
    //for(i <- 1 to 10) println("("+i+","+i+"),")

    var hist: Seq[Tuple2[Int, Int]] = Seq(
      (1, 1),
      (2, 2),
      (3, 4),
      (4, 6),
      (5, 5),
      (6, 4),
      (7, 5)
      /*,
      (8,11),
      (9,8),
      (10,5)
    */
    )

    val hValueHight: Int = 4
    var curr: Seq[Tuple2[Int, Int]] = Seq((1, 1), (3, 4))

    // (1,1) -> begin with (2,2) result (4,6) because 6 > 1+4 =5
    // (3,4) -> begin with resule (8,10) because 10 > 4+4=8

    def rec_search(seqToSearch: Seq[Tuple2[Int, Int]], seqWhatSearchFirstElm: Seq[Tuple2[Int, Int]]): Seq[Tuple2[Tuple2[Int, Int], Tuple2[Int, Int]]] = {
      if (seqWhatSearchFirstElm.size == 1) {
        //last iteration
        Seq(
          ((seqWhatSearchFirstElm.head), seqToSearch.find(x => (x._2 > seqWhatSearchFirstElm.head._2 + hValueHight || x._2 < seqWhatSearchFirstElm.head._2 - hValueHight)).getOrElse(Tuple2(0, 0)))
        )
      } else {
        Seq(
          ((seqWhatSearchFirstElm.head), seqToSearch.find(x => (x._2 > seqWhatSearchFirstElm.head._2 + hValueHight || x._2 < seqWhatSearchFirstElm.head._2 - hValueHight)).getOrElse(Tuple2(0, 0)))
        ) ++
          rec_search(seqToSearch.span(x => (x._1 < seqWhatSearchFirstElm.head._1))._2, seqWhatSearchFirstElm.tail)
      }
    }

    val res: Seq[Tuple2[Tuple2[Int, Int], Tuple2[Int, Int]]] = rec_search(hist, curr)

    for (pair <- res) {
      println(pair._1 + " - " + pair._2)
    }

  }



  trait HumanFuncs {
    val who : String
    def whoami = who
    override def toString = who
  }



  class Man extends HumanFuncs {
    val who: String = "man"
  }


  class Woman extends HumanFuncs {
    val who : String = "Woman"
  }

  class Child extends HumanFuncs {
    val who : String = "Child"
  }



  abstract class Car {
  }


    object Car {
      def apply(model: String) = model match {
        case "toyota" => new Man()
        case "bmw"    => new Woman()
        case _        => new Child()
      }
    }


  def compare_class_cclass : Unit ={

    val seqObjs : Seq[Any] = Seq(Car("toyota"),Car("bmw"),Car("mersedes"))

    /*
    val human1 = Car("toyota")
    println("human1.getClass.getName="+human1.getClass.getName)

    println("------------------------------------------------")

    val human2 = Car("bmw")
    println("human1.getClass.getName="+human2.getClass.getName)
    println(human2)

    println("call whoami - "+
      (human1 match {
        case x: Man   => x.whoami
        case x: Woman => x.whoami
        case x: Child => x.whoami
        case _ => "others"
      })
    )
    */
    for(elm <- seqObjs) {
      print("elmClass="+elm.getClass.getName)
      print("   WHOAMI : "+
        (elm match {
          case x: Man   => x.whoami
          case x: Woman => x.whoami
          case x: Child => x.whoami
          case _ => "others"
        }))
      println(" ")
    }

  }





  class HelloActor(myName: String) extends Actor {


    def receive = {
      case "hello" => {
        println("("+myName+") hello back at you")
        //Thread.sleep(3000)
      }
      case _       => {
        println("("+myName+") huh?")
        //Thread.sleep(3000)
      }
    }

  }


  def parallel_test : Unit ={

    val system = ActorSystem("HelloSystem")

    val helloActor = system.actorOf(Props(new HelloActor("Parameter as String")), name = "helloactor")
    val helloActor2 = system.actorOf(Props(new HelloActor("xxxxxxxx")), name = "helloactor2")

    println("helloActor.getClass.getName="+helloActor.getClass.getName)

    helloActor.tell(msg = "y1",sender = helloActor2)
    helloActor.tell(msg = "y2",sender = helloActor2)
    helloActor.tell(msg = "y3",sender = helloActor2)

    //helloActor ! "hello"
    //helloActor ! "buenos dias"

  }






  parallel_test
  //compare_class_cclass

}
