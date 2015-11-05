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

import org.scalacheck.{ Arbitrary, Prop }
import org.scalatest.Finders
import org.scalatest.fixture.{ FlatSpec, UnitFixture }
import org.scalatest.prop.Checkers

import com.signalcollect.triplerush.TripleGenerators.{ genTriples, queryPatterns, tripleSet }
import com.signalcollect.triplerush.jena.Jena

class CountingQuerySpec extends FlatSpec with UnitFixture with Checkers {

  implicit lazy val arbTriples = Arbitrary(genTriples map (_.toSet))
  implicit lazy val arbQuery = Arbitrary(queryPatterns)

  "Counting Query" should "correctly answer a query for data that is not in the store" in new TestStore {
    val triples = Set(TriplePattern(1, 2, 3))
    val query = List(TriplePattern(-1, 4, -1))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query).size
    assert(trResults === trCount)
  }

  it should "correctly answer a query for a specific pattern that exists" in new TestStore {
    val triples = Set(TriplePattern(1, 2, 3))
    val query = List(TriplePattern(1, 2, 3))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount)
  }

  it should "count zero results for an empty query" in new TestStore {
    val triples = Set(TriplePattern(8, 23, 19), TriplePattern(13, 25, 5), TriplePattern(6, 23, 18))
    val query = List()
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount)
  }

  it should "correctly answer a query for a specific pattern that does not exist" in new TestStore {
    val triples = Set(TriplePattern(1, 2, 3))
    val query = List(TriplePattern(1, 4, 3))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount)
  }

  it should "correctly answer a query that successfully binds the same variable twice" in new TestStore {
    val triples = Set(TriplePattern(1, 2, 1))
    val query = List(TriplePattern(-1, 2, -1))
    val trCount = TestHelper.count(tr, triples, query)
    assert(1 === trCount)
  }

  it should "correctly answer a query that successfully binds the same variable three times" in new TestStore {
    val triples = Set(TriplePattern(1, 1, 1))
    val query = List(TriplePattern(-1, -1, -1))
    val trCount = TestHelper.count(tr, triples, query)
    assert(trCount === 1)
  }

  it should "correctly answer a query that unsuccessfully binds the same variable twice" in new TestStore {
    val triples = Set(TriplePattern(1, 2, 3))
    val query = List(TriplePattern(-1, 2, -1))
    val trCount = TestHelper.count(tr, triples, query)
    assert(trCount === 0)
  }

  it should "correctly answer a query that unsuccessfully binds the same variable three times" in new TestStore {
    val triples = Set(TriplePattern(1, 1, 2))
    val query = List(TriplePattern(-1, -1, -1))
    val trCount = TestHelper.count(tr, triples, query)
    assert(trCount === 0)
  }

  it should "correctly answer a simple query 1" in new TestStore {
    val triples = Set(TriplePattern(4, 3, 4))
    val query = List(TriplePattern(-1, 3, -1))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount)
  }

  it should "correctly answer a simple query 2" in new TestStore {
    val triples = Set(TriplePattern(3, 4, 2), TriplePattern(3, 4, 4), TriplePattern(2, 3, 3),
      TriplePattern(3, 3, 3), TriplePattern(1, 1, 2), TriplePattern(3, 3, 4),
      TriplePattern(4, 4, 1), TriplePattern(4, 4, 3))
    val query = List(TriplePattern(-2, -1, 3))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount)
  }

  it should "correctly answer a simple query, where one pattern is fully bound and that triple exists" in new TestStore {
    val triples = Set(TriplePattern(3, 4, 2), TriplePattern(3, 4, 4), TriplePattern(2, 3, 3),
      TriplePattern(3, 3, 3), TriplePattern(1, 1, 2), TriplePattern(3, 3, 4),
      TriplePattern(4, 4, 1), TriplePattern(4, 4, 3))
    val query = List(TriplePattern(3, 4, 2), TriplePattern(-2, -1, -3))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount, s"Bindings found: ${trResults.size}, counting query results: $trCount")
  }

  it should "correctly answer a simple query, where one pattern is fully bound and that triple does not exist" in new TestStore {
    val triples = Set(TriplePattern(3, 4, 2), TriplePattern(3, 4, 4), TriplePattern(2, 3, 3),
      TriplePattern(3, 3, 3), TriplePattern(1, 1, 2), TriplePattern(3, 3, 4),
      TriplePattern(4, 4, 1), TriplePattern(4, 4, 3))
    val query = List(TriplePattern(1, 2, 3), TriplePattern(-2, -1, 3))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount)
  }

  it should "correctly answer a simple query over a lot of data" in new TestStore {
    val triples = {
      for {
        s <- 1 to 25
        p <- 1 to 25
        o <- 1 to 25
      } yield TriplePattern(s, p, o)
    }.toSet

    val query = List(TriplePattern(-1, 1, -1), TriplePattern(-1, 2, -2), TriplePattern(-1, -3, 25))
    val trCount = TestHelper.count(tr, triples, query)
    val trResults = TestHelper.execute(tr, Set(), query)
    assert(trResults.size === trCount)
  }

  it should "compute predicate selectivities over some triples" in new TestStore {
    val jena = new Jena
    try {
      val triples = {
        for {
          s <- 1 to 2
          p <- 1 to 2
          o <- 1 to 10
        } yield TriplePattern(s, p, o)
      }.toSet
      val query = List(TriplePattern(-1, 1, -1), TriplePattern(-1, 2, -2), TriplePattern(-1, -3, 10))
      val trResults = TestHelper.execute(
        tr,
        triples,
        query)
    } finally {
      jena.shutdown
    }
  }

  it should "also work with encoded triples" in new TestStore {
    tr.addStringTriple("Elvis", "inspired", "Dylan")
    tr.addStringTriple("Dylan", "inspired", "Jobs")
    val encodedInspired = tr.dictionary("inspired")
    val query = Seq(TriplePattern(-1, encodedInspired, -2))
    val count = tr.count(query)
    assert(count === 2)
  }

  it should "correctly answer random queries with basic graph patterns" in new TestStore {
    check(
      Prop.forAllNoShrink(tripleSet, queryPatterns) {
        (triples: Set[TriplePattern], query: List[TriplePattern]) =>
          val trCount = TestHelper.count(tr, triples, query)
          val trResults = TestHelper.execute(tr, Set(), query)
          assert(trResults.size === trCount, s"Bindings found: ${trResults.size}, counting query results: $trCount")
          trResults.size === trCount
      }, minSuccessful(5))
  }

}
