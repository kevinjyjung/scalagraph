package example

import data.{PairRDD, RDD}
import graph.{FeatMat, Node}
import interpreter.{NextOp, Op, SamplerOp, parser}

import scala.io.Source

object test {

    def readQuery(fileName: String): String =
        Source.fromFile(fileName).getLines.mkString

    val adjRDD = new PairRDD(Map(
        Node(1) -> Seq(Node(2), Node(3), Node(4)),
        Node(2) -> Seq(Node(3), Node(3)),
        Node(3) -> Seq(Node(4)),
        Node(4) -> Seq(Node(5), Node(5), Node(5)),
        Node(5) -> Seq(Node(1), Node(2), Node(3))
    ))
    val featRDD = new PairRDD(Map(
        Node(1) -> Seq(0.1f, 0.1f, 0.1f),
        Node(2) -> Seq(0.2f, 0.2f, 0.2f),
        Node(3) -> Seq(0.3f, 0.3f, 0.3f),
        Node(4) -> Seq(0.4f, 0.4f, 0.4f),
        Node(5) -> Seq(0.5f, 0.5f, 0.5f)
    ))

    def printBatch(batch: Seq[FeatMat]): Boolean = {
        batch.foreach(fm => println(fm))
        true
    }

    def main(args: Array[String]): Unit = {
        def sampler: (String, Int, Op) => SamplerOp = SamplerOp(adjRDD)(_,_,_)
        def next: (String, Int) => NextOp = NextOp()(_,_)
        val outs = parser.parse(sampler, next)(readQuery("example_query.json"))
        val nodes: Seq[RDD[Node]] = outs map (_.eval)

        printBatch(nodes.map(graph.op.feats(featRDD, _)))
    }



}
