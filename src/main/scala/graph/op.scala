package graph

import scala.util.Random
import data.RDD


object op {
    private def uniformSample(nodes: Seq[Node], n: Int): Seq[Node] =
        Seq.fill(n)(nodes(Random.nextInt(nodes.length)))

    private def joinFunc[T](tup: (Node, T), node: Node): Boolean = tup._1.id == node.id

    def ns(adj: RDD[(Node, Seq[Node])], nodes: RDD[Node], n: Int): RDD[Node] =
        adj join (nodes, joinFunc, "right_outer") flatMap (tup => uniformSample(tup._2, n))

    def feats(feat: RDD[(Node, FeatVec)], nodes: RDD[Node]): FeatMat =
        (feat join (nodes, joinFunc, "right_outer") map (tup => tup._2)).collect
}
