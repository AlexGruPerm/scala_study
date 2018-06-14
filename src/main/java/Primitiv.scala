object Primitiv extends App {

  def op1 = {
    println("op1")
    false
  }

  def op2 = {
    println("op2")
    false
  }

  if (op1 & op2)
    println("[TRUE]")
  else
    println("[ELSE]")

}
