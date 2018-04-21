package data

import scala.collection.immutable

abstract class RDD[T] {
    def join[U](other: RDD[U], joinOn: (T, U) => Boolean, joinType: String): RDD[T]
    def map[U](f: T => U): RDD[U]
    def flatMap[U](f: T => Seq[U]): RDD[U]
    def collect: Seq[T]
}

class PairRDD[T, V](val data: immutable.Map[T, V]) extends RDD[(T, V)] {
    def map[U](f: ((T, V)) => U): RDD[U] = new SingleRDD((data map f).toSeq)
    def join[U](other: RDD[U], joinOn: ((T, V), U) => Boolean, joinType: String): RDD[(T, V)] =
        if (joinType == "right_outer") rightOuterJoin(other, joinOn)
        else throw new IllegalArgumentException("bad join")
    def rightOuterJoin[U](other: RDD[U], joinOn: ((T, V), U) => Boolean): RDD[(T, V)] =
        other.map(u => {
            val res = data.toSeq.filter(tup => joinOn(tup, u))
            assert(res.length == 1)
            res.head
        })
    def flatMap[U](f: ((T, V)) => Seq[U]): RDD[U] = new SingleRDD((data flatMap f).toSeq)
    def collect: Seq[(T, V)] = data.toSeq
}

class SingleRDD[T](val data: Seq[T]) extends RDD[T] {
    def map[U](f: T => U): RDD[U] = new SingleRDD(data.map(f))
    def join[U](other: RDD[U], joinOn: (T, U) => Boolean, joinType: String): RDD[T] = null
    def flatMap[U](f: T => Seq[U]): RDD[U] = new SingleRDD(data.flatMap(f))
    def collect: Seq[T] = data
}
