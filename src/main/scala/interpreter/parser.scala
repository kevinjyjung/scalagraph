package interpreter

import play.api.libs.json._

object parser {

    val example_json: JsValue = Json.parse("""
{
  "graph": "abcd",
  "target": ["1", "2", "3"],
  "ops": [
    {
      "id": "1",
      "op": "NEXT",
      "args": [1]
    },
    {
      "id": "2",
      "op": "SAMPLE",
      "args": [2],
      "input": "1"
    },
    {
      "id": "3",
      "op": "SAMPLE",
      "args": [4],
      "input": "2"
    }
  ]
}
""")

    case class OpConstructor(id: String, op: String, args: Seq[Int], input: Option[String])
    case class OpGraph(graph: String, target: Seq[String], ops: Seq[OpConstructor])

    def buildMap(targets: Seq[String], ops: Seq[OpConstructor])
                (sampler: (String, Int, Op) => SamplerOp, next: (String, Int) => NextOp): Map[String, Op] = {
        def addToRes(opc: OpConstructor, res: Map[String, Op]): Map[String, Op] =
            res + (opc.id -> (opc.op match {
                case "NEXT" => next(opc.id, opc.args.head)
                case "SAMPLE" => sampler(opc.id, opc.args.head, res(opc.input.get))
            }))
        def opIter(target: Option[String], res: Map[String, Op]): Map[String, Op] =
            if (target.isEmpty) res
            else if (res.exists(_._1 == target.get)) res
            else {
                val targetOpc = ops.find(_.id == target.get).get
                addToRes(targetOpc, opIter(targetOpc.input, res))
            }
        def targetIter(targets: Seq[String], res: Map[String, Op]): Map[String, Op] =
            if (targets.isEmpty) res else targetIter(targets.tail, opIter(Option(targets.head), res))
        targetIter(targets, Map())
    }

    def parse(sampler: (String, Int, Op) => SamplerOp, next: (String, Int) => NextOp)(json: Option[String]): Seq[Op] = {
        implicit val opConstructorReads: Reads[OpConstructor] = Json.reads[OpConstructor]
        implicit val opGraphReads: Reads[OpGraph] = Json.reads[OpGraph]
        val opGraph = opGraphReads.reads(if (json.isEmpty) example_json else Json.parse(json.get)).get
        val opsMap = buildMap(opGraph.target, opGraph.ops)(sampler, next)
        opGraph.target.map(opsMap)
    }
}
