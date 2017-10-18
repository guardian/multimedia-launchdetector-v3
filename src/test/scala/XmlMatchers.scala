//import org.scalatest.matchers.{BeMatcher, MatchResult}
//
//import scala.xml.{Node, NodeSeq}
//
//trait XmlMatchers {
//  class NodeSeqMatcher(right:NodeSeq) extends BeMatcher[NodeSeq] {
//    private def compareNode(actual:Node, expected:Node):MatchResult = {
////      for {
////        a <- MatchResult(actual.canEqual(expected),s"${actual} is a different object type to ${expected}",s"${actual} is the same object type to ${expected}")
////        b <- MatchResult(actual.isAtom==expected.isAtom, s"$actual and $expected are not both atoms", s"$actual and $expected are both atoms")
////        c <- MatchResult(actual.attributes.asAttrMap==expected.attributes.asAttrMap, s"$actual and $expected attributes don't match", s"$actual and $expected attributes do match")
////        d <- MatchResult(actual.isEmpty==expected.isEmpty, s"One of $actual and $expected is empty and the others aren't", s"$actual and $expected are both empty")
////
////      } yield (a,b,c,d)
//
//      val r=MatchResult(actual.canEqual(expected),s"${actual} is a different object type to ${expected}",s"${actual} is the same object type to ${expected}")
//
//      val failedAssertions = Seq(
//        MatchResult(actual.canEqual(expected),s"${actual} is a different object type to ${expected}",s"${actual} is the same object type to ${expected}"),
//        MatchResult(actual.isAtom==expected.isAtom, s"$actual and $expected are not both atoms", s"$actual and $expected are both atoms"),
//        MatchResult(actual.attributes.asAttrMap==expected.attributes.asAttrMap, s"$actual and $expected attributes don't match", s"$actual and $expected attributes do match"),
//        MatchResult(actual.isEmpty==expected.isEmpty, s"One of $actual and $expected is empty and the others aren't", s"$actual and $expected are both empty")
//
//      //      actual.isAtom must be(expected.isAtom)
////      actual.attributes.asAttrMap must be(expected.attributes.asAttrMap)
////      actual.isEmpty must be(expected.isEmpty)
//      ).filter(_.matches==false)
//
//      if(failedAssertions.nonEmpty)
//        MatchResult(matches = false, failedAssertions.map(_.failureMessage).mkString("; "), failedAssertions.map(_.negatedFailureMessage).mkString("; "))
//      else
//        MatchResult(matches = true, failedAssertions.map(_.failureMessage).mkString("; "), failedAssertions.map(_.negatedFailureMessage).mkString("; "))
//    }
//
//    override def apply(left: NodeSeq): MatchResult = {
//      val failedAssertions = for {
//        leftNodeEntry <- left
//        rightNodeEntry <- right
//        result <- this.compareNode(leftNodeEntry, rightNodeEntry)
//      } yield result
//
//      //val failedAssertions = left.map(this.compareNode(_,right)).filter(_.matches==false)
//      if(failedAssertions.nonEmpty)
//        return MatchResult(matches = false, failedAssertions.map(_.failureMessage).mkString("; "), failedAssertions.map(_.negatedFailureMessage).mkString("; "))
//
//      val childNodeSeq = left.flatMap(_.child)
//      if(childNodeSeq.length>0)
//        this.apply(childNodeSeq)
//      else
//        MatchResult(matches = true,"The nodes were not equal", "The nodes were equal")
//    }
//  }
//
//  def matchXml(right:NodeSeq) = new NodeSeqMatcher(right)
//}
