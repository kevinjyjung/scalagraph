package object graph {
    type FeatVec = Seq[Float]
    type FeatMat = Seq[FeatVec]
    type Id = Int

    case class Node(id: Id)
}
