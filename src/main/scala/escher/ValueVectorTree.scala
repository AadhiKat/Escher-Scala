package escher

import collection.mutable
import ValueVectorTree._
import escher.Synthesis.{IndexValueMap, ValueVector}

/**
  * A tree with fixed depth, each path from root to leaf represents a ValueVector,
  * can be used to efficiently find ValueVectors that match a goal (partial vector)
  */
object ValueVectorTree {

  sealed trait TreeNode[A]{
  }

  case class LeafNode[A](term: A) extends TreeNode[A]


  class InternalNode[A](val children: mutable.Map[TermValue, TreeNode[A]]) extends TreeNode[A] {
    def addTerm(term: A, valueVector: List[TermValue]): Boolean = valueVector match {
      case Nil => throw new Exception("Empty valueVector!")
      case List(v) =>
        if(children.contains(v)) false
        else {
          children(v) = LeafNode(term)
          true
        }
      case v :: tail =>
        val n1 = children.get(v) match {
          case Some(n1: InternalNode[A]) => n1
          case None => new InternalNode[A](mutable.Map())
          case _ => throw new Exception()
        }
        children(v) = n1
        n1.addTerm(term, tail)
        true
    }

    def searchTerms(valueVector: List[Either[Unit ,TermValue]]): Iterator[A] = valueVector match {
      case Nil => throw new Exception("Empty valueVector!")
      case List(v) => v match {
        case Left(_) =>
          children.values.toIterator.collect {
            case LeafNode(t) => t
          }
        case Right(tv) =>
          children.get(tv) match {
            case Some(LeafNode(t)) => Iterator(t)
            case _ => Iterator()
          }
      }
      case v :: tail => v match {
        case Left(_) =>
          children.values.toIterator.flatMap {
            case n1: InternalNode[A] =>
              n1.searchTerms(tail)
            case _ => throw new Exception()
          }
        case Right(tv) =>
          children.get(tv) match {
            case Some(n1: InternalNode[A]) => n1.searchTerms(tail)
            case _ => Iterator()
          }
      }
    }

    def searchATerm(valueVector: List[Either[Unit ,TermValue]]): Option[A] = valueVector match {
      case Nil => throw new Exception("Empty valueVector!")
      case List(v) => v match {
        case Left(_) =>
          children.values.foreach {
            case LeafNode(t) => return Some(t)
            case _ => sys.error("ill-formed tree.")
          }
          None
        case Right(tv) =>
          children.get(tv) match {
            case Some(LeafNode(t)) => Some(t)
            case _ => None
          }
      }
      case v :: tail => v match {
        case Left(_) =>
          children.values.foreach {
            case n1: InternalNode[A] =>
              n1.searchATerm(tail).foreach(t => return Some(t))
            case _ => throw new Exception()
          }
          None
        case Right(tv) =>
          children.get(tv) match {
            case Some(n1: InternalNode[A]) => n1.searchATerm(tail)
            case _ => None
          }
      }
    }

  }

  def print[T](tree: TreeNode[T], show: T => String, indent: Int): Unit = tree match {
    case LeafNode(term) =>
      println("  " * indent + s"* ${show(term)}")
    case in : InternalNode[T] =>
      in.children.foreach{
        case (tv, tree1) =>
          println("  " * indent + s"- ${tv.show}")
          print(tree1, show, indent + 1)
      }
  }

}

/**
  * A tree with fixed depth, each path from root to leaf represents a ValueVector,
  * can be used to efficiently find ValueVectors that match a goal (partial vector)
  * @param depth the length of each ValueVector
  */
class ValueVectorTree[A](depth: Int, thresholdToUseTree: Double = 10){
  private var _size = 0
  def size: Int = _size

  private val root = new InternalNode[A](mutable.Map())
  private val valueTermMap = mutable.Map[ValueVector, A]()

  def elements: Iterable[(ValueVector, A)] = valueTermMap

  private def shouldUseTree(): Boolean = size.toDouble/depth >= thresholdToUseTree

  /** @return whether this term was added into this ValueVectorTree
    *         (it will not be added if a term with the same ValueVector already exists) */
  def addTerm(term: A, valueVector: ValueVector): Boolean = {
    if(shouldUseTree()) {
      val added = root.addTerm(term, valueVector.toList)
      if (added) {
        _size += 1
        valueTermMap(valueVector) = term
      }
      added
    }else{
      valueTermMap.get(valueVector) match {
        case None =>
          root.addTerm(term, valueVector.toList)
          _size += 1
          valueTermMap(valueVector) = term
          true
        case Some(_) => false
      }
    }
  }

  def update(valueVector: ValueVector, term: A): Boolean = addTerm(term, valueVector)

  private def valueMapToVector(valueMap: IndexValueMap): List[Either[Unit, TermValue]] ={
    (0 until depth).toList.map{ i =>
      valueMap.get(i) match{
        case Some(tv) => Right(tv)
        case None => Left(())
      }
    }
  }

  def searchTerms(valueMap: IndexValueMap): Iterator[A] = {
    if(shouldUseTree()) {
      val vv = valueMapToVector(valueMap)
      root.searchTerms(vv)
    }else{
      valueTermMap.toIterator.collect {
        case (vv, term) if IndexValueMap.matchVector(valueMap, vv) =>
          term
      }
    }
  }

  def searchATerm(valueMap: IndexValueMap): Option[A] = {
    if(shouldUseTree()) {
      val vv = valueMapToVector(valueMap)
      root.searchATerm(vv)
    }else{
      valueTermMap.foreach{
        case (vv, term) =>
          if(IndexValueMap.matchVector(valueMap, vv))
            return Some(term)
      }
      None
    }
  }

  def get(valueVector: ValueVector): Option[A] = {
    valueTermMap.get(valueVector)
  }

  def printRoot(show: A => String): Unit ={
    println("---Root Status---")
    print(root, show, 0)
  }
}