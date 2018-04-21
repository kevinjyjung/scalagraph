package interpreter

import data.{RDD, SingleRDD}
import graph._

trait Op {
    val id: String
    protected var _value: RDD[Node] = _
    def value: RDD[Node] = _value
    def isVal: Boolean = value != null
    def eval: RDD[Node] = if (isVal) value else doEval()
    def doEval(): RDD[Node]
}

class SamplerOp(val adj: RDD[(Node, Seq[Node])], val id: String, val n: Int, val inp: Op) extends Op {
    def doEval(): RDD[Node] = {_value = op.ns(adj, inp.eval, n); _value}
}

object SamplerOp {
    def apply(adj: RDD[(Node, Seq[Node])])(id: String, n: Int, inp: Op): SamplerOp = new SamplerOp(adj, id, n, inp)
}

class NextOp(val id: String, val n: Int) extends Op {
    def getNext: RDD[Node] = new SingleRDD(Seq.fill(n)(Node(1))) // dummy get next method
    def doEval(): RDD[Node] = {_value = getNext; _value}
}

object NextOp {
    def apply()(id: String, n: Int): NextOp = new NextOp(id, n)
}
