package escher

/**
  * Created by weijiayi on 04/04/2017.
  */
object Main {

  def testDivide(): Unit ={
    import escher.Synthesis._

    val a = divideNumberAsSum(2000,3)
    println{
      a.length
    }
  }

  def testCartesianProduct(): Unit = {
    import escher.Synthesis._
    val r = cartesianProduct(IndexedSeq(Map(1->2,3->4), Map(5->6), Map(7->8, 9->10)))
    println(
      r.toList
    )
  }

  def testSynthesis(): Unit ={
    import escher._
    import DSL._

    import Synthesis._
    val IS = IndexedSeq

    synthesize("length", IS(tyList(tyInt)),
      IS("xs"), tyInt
    )(compMap = CommonlyUsedComponents.allMap,
      compCostFunction = (_ => 1),
      IS(IS(listValue()), IS(listValue(2)), IS(listValue(1,2))),
      IS(0,1,2)
    )
  }

  def main(args: Array[String]): Unit = {
    testSynthesis()
  }
}
