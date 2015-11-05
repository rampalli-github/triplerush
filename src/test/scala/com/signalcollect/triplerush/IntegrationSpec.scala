/*
 *  @author Philip Stutz
 *
 *  Copyright 2013 University of Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.signalcollect.triplerush

import com.signalcollect.triplerush.jena.Jena
import org.scalacheck.Gen._
import org.scalacheck.{ Arbitrary, Gen, Prop }
import org.scalatest.fixture.{ FlatSpec, UnitFixture }
import org.scalatest.prop.Checkers
import com.signalcollect.triplerush.TripleGenerators._
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.fixture.NoArg

class IntegrationSpec extends FlatSpec with UnitFixture with Checkers with ScalaFutures {

  implicit lazy val arbTriples = Arbitrary(tripleSet)
  implicit lazy val arbQuery = Arbitrary(queryPatterns)

  "TripleRush" should "correctly answer a query for data that is not in the store" in new TestStore {
    val trResults = TestHelper.execute(
      tr,
      Set(TriplePattern(1, 2, 3)),
      List(TriplePattern(-1, 4, -1)))
    assert(Set[Map[Int, Int]]() === trResults)
  }

  it should "correctly answer a query for a specific pattern that exists" in new TestStore {
    val trResults = TestHelper.execute(
      tr,
      Set(TriplePattern(1, 2, 3)),
      List(TriplePattern(1, 2, 3)))
    assert(Set[Map[Int, Int]](Map()) === trResults)
  }

  it should "correctly answer a query for a specific pattern that does not exist" in new TestStore {
    val trResults = TestHelper.execute(
      tr,
      Set(TriplePattern(1, 2, 3)),
      List(TriplePattern(1, 4, 3)))
    assert(Set[Map[Int, Int]]() === trResults)
  }

  it should "correctly answer a simple query 1" in new TestStore {
    val trResults = TestHelper.execute(
      tr,
      Set(TriplePattern(4, 3, 4)),
      List(TriplePattern(-1, 3, -1)))
    assert(Set(Map(-1 -> 4)) === trResults)
  }

  it should "correctly answer a simple query 2" in new TestStore {
    val trResults = TestHelper.execute(
      tr,
      Set(TriplePattern(3, 4, 2), TriplePattern(3, 4, 4), TriplePattern(2, 3, 3),
        TriplePattern(3, 3, 3), TriplePattern(1, 1, 2), TriplePattern(3, 3, 4),
        TriplePattern(4, 4, 1), TriplePattern(4, 4, 3)),
      List(TriplePattern(-2, -1, 3)))
    assert(Set(Map(-1 -> 3, -2 -> 2), Map(-1 -> 3, -2 -> 3),
      Map(-1 -> 4, -2 -> 4)) === trResults)
  }

  it should "correctly answer a simple query, where one pattern is fully bound and that triple exists" in new TestStore {
    val trResults = TestHelper.execute(
      tr,
      Set(TriplePattern(3, 4, 2), TriplePattern(3, 4, 4), TriplePattern(2, 3, 3),
        TriplePattern(3, 3, 3)),
      List(TriplePattern(3, 4, 2), TriplePattern(-1, -2, -3)))
    assert(Set(Map(-1 -> 3, -2 -> 4, -3 -> 2), Map(-1 -> 3, -2 -> 4, -3 -> 4),
      Map(-1 -> 2, -2 -> 3, -3 -> 3), Map(-1 -> 3, -2 -> 3, -3 -> 3)) === trResults)
  }

  it should "correctly answer a simple query, where one pattern is fully bound and that triple does not exist" in new TestStore {
    val trResults = TestHelper.execute(
      tr,
      Set(TriplePattern(3, 4, 2), TriplePattern(3, 4, 4), TriplePattern(2, 3, 3),
        TriplePattern(3, 3, 3), TriplePattern(1, 1, 2), TriplePattern(3, 3, 4),
        TriplePattern(4, 4, 1), TriplePattern(4, 4, 3)),
      List(TriplePattern(1, 2, 3), TriplePattern(-2, -1, 3)))
    assert(Set[Map[Int, Int]]() === trResults)
  }

  it should "correctly answer queries after blocking triple additions" in new TestStore {
    for (i <- (1 to 100)) {
      tr.addEncodedTriple(1, 2, i)
      val count = tr.count(Seq(TriplePattern(1, 2, -1)))
      assert(count.toInt == i)
    }
  }

  it should "correctly answer a simple query over a reasonable amount of data" in new TestStore {
    val jena = new Jena
    try {
      val query = List(TriplePattern(-1, 1, -1), TriplePattern(-1, 2, -2), TriplePattern(-1, -3, 10))
      val triples = {
        for {
          s <- 1 to 3
          p <- 1 to 3
          o <- 1 to 10
        } yield TriplePattern(s, p, o)
      }.toSet
      val trResults = TestHelper.execute(
        tr,
        triples,
        query)
      val jenaResults = TestHelper.execute(
        jena,
        triples,
        query)
      assert(jenaResults === trResults, s"Jena results $jenaResults did not equal our results $trResults.")
    } finally {
      jena.shutdown
    }
  }

}

object TestHelper {

  def prepareStore(qe: QueryEngine,
                   triples: Set[TriplePattern]) {
    qe.addTriplePatterns(triples.iterator, 60.seconds)
  }

  def count(tr: TripleRush,
            triples: Set[TriplePattern],
            query: List[TriplePattern]): Long = {
    prepareStore(tr, triples)
    tr.count(query)
  }

  def resultsToBindings(results: Iterator[Array[Int]]): Set[Map[Int, Int]] = {
    results.map({ binding: Array[Int] =>
      // Only keep variable bindings that have an assigned value.
      val filtered: Map[Int, Int] = {
        (-1 to -binding.length by -1).
          zip(binding).
          filter(_._2 > 0).
          toMap
      }
      filtered
    }).toSet
  }

  def execute(
    qe: QueryEngine,
    triples: Set[TriplePattern],
    query: List[TriplePattern]): Set[Map[Int, Int]] = {
    prepareStore(qe, triples)
    val results = qe.resultIteratorForQuery(query)
    val bindings = resultsToBindings(results)
    bindings
  }
}
