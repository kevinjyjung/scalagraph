package interpreter

import play.api.libs.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object parser {

    case class OpConstructor(id: String, op: String, args: Seq[Int], inp: Option[String])
    case class OpGraph(graph: String, target: Seq[String], ops: Seq[OpConstructor])

    object OpGraphJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
        implicit val formats1 = jsonFormat4(OpConstructor)
        implicit val formats2 = jsonFormat3(OpGraph)
    }

    def buildMap(targets: Seq[String], ops: Seq[OpConstructor])
                (sampler: (String, Int, Op) => SamplerOp, next: (String, Int) => NextOp): Map[String, Op] = {
        def addToRes(opc: OpConstructor, res: Map[String, Op]): Map[String, Op] =
            res + (opc.id -> (opc.op match {
                case "NEXT" => next(opc.id, opc.args.head)
                case "SAMPLE" => sampler(opc.id, opc.args.head, res(opc.inp.get))
            }))
        def opIter(target: Option[String], res: Map[String, Op]): Map[String, Op] =
            if (target.isEmpty) res
            else if (res.exists(_._1 == target.get)) res
            else {
                val targetOpc = ops.find(_.id == target.get).get
                addToRes(targetOpc, opIter(targetOpc.inp, res))
            }
        def targetIter(targets: Seq[String], res: Map[String, Op]): Map[String, Op] =
            if (targets.isEmpty) res else targetIter(targets.tail, opIter(Option(targets.head), res))
        targetIter(targets, Map())
    }

    def parse(sampler: (String, Int, Op) => SamplerOp, next: (String, Int) => NextOp)(opGraph: OpGraph): Seq[Op] = {
        val opsMap = buildMap(opGraph.target, opGraph.ops)(sampler, next)
        opGraph.target.map(opsMap)
    }

//    def parse(sampler: (String, Int, Op) => SamplerOp, next: (String, Int) => NextOp)(json: String): Seq[Op] = {
//        implicit val opConstructorReads: Reads[OpConstructor] = Json.reads[OpConstructor]
//        implicit val opGraphReads: Reads[OpGraph] = Json.reads[OpGraph]
//        val opGraph = opGraphReads.reads(Json.parse(json)).get
//        val opsMap = buildMap(opGraph.target, opGraph.ops)(sampler, next)
//        opGraph.target.map(opsMap)
//    }
}
