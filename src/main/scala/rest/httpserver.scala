package rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import data.{PairRDD, RDD}
import graph.{FeatMat, Node}
import interpreter.parser.OpGraphJsonSupport._
import interpreter.parser.OpGraph
import interpreter.{NextOp, Op, SamplerOp, parser}
import play.api.libs.json.{Json, Reads, Writes}

import scala.concurrent.ExecutionContextExecutor
import scala.io.{Source, StdIn}

object httpserver {

    case class JsonFeatMats(data: Seq[FeatMat])

    val adjRDD = new PairRDD(Map(
        Node(1) -> Seq(Node(2), Node(3), Node(4)),
        Node(2) -> Seq(Node(3), Node(3)),
        Node(3) -> Seq(Node(4)),
        Node(4) -> Seq(Node(5), Node(5), Node(5)),
        Node(5) -> Seq(Node(1), Node(2), Node(3))
    ))
    val featRDD = new PairRDD(Map(
        Node(1) -> Seq(0.1, 0.1, 0.1),
        Node(2) -> Seq(0.2, 0.2, 0.2),
        Node(3) -> Seq(0.3, 0.3, 0.3),
        Node(4) -> Seq(0.4, 0.4, 0.4),
        Node(5) -> Seq(0.5, 0.5, 0.5)
    ))

    def jsonStringify(featMats: Seq[FeatMat]): String = {
        implicit val jsonFeatMatsWrites: Writes[JsonFeatMats] = Json.writes[JsonFeatMats]
        jsonFeatMatsWrites.writes(JsonFeatMats(featMats)).toString()
    }

    def evalQuery(opGraph: OpGraph): String = {
        def sampler: (String, Int, Op) => SamplerOp = SamplerOp(adjRDD)(_,_,_)
        def next: (String, Int) => NextOp = NextOp()(_,_)
        val outs = parser.parse(sampler, next)(opGraph)
        val nodes: Seq[RDD[Node]] = outs map (_.eval)
        jsonStringify(nodes.map(graph.op.feats(featRDD, _)))
    }

    def main(args: Array[String]): Unit = {

        implicit val system: ActorSystem = ActorSystem("my-system")
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher

        val route =
            path("query") {
                post {
                    entity(as[OpGraph]) { opGraph =>
                        complete(HttpEntity(ContentTypes.`application/json`, evalQuery(opGraph)))
                    }
                }
            }

        val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

        println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
        StdIn.readLine() // let it run until user presses return
        bindingFuture
            .flatMap(_.unbind()) // trigger unbinding from the port
            .onComplete(_ => system.terminate()) // and shutdown when done
    }
}
