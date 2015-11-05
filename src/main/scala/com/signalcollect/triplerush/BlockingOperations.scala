/*
 *  @author Philip Stutz
 *
 *  Copyright 2015 Cotiviti
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

import java.io.InputStream

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.apache.jena.graph.{ Triple => JenaTriple }
import org.apache.jena.riot.Lang

trait BlockingOperations {
  this: TripleRush with ConvenienceOperations =>

  import ConvenienceOperations._
  import TripleRush.defaultBlockingOperationTimeout

  private[this] def combineWithShutdownFuture[G](f: Future[G]): Future[G] = Future.firstCompletedOf[G](List(tripleRushShutdown, f))

  def addTriplePatterns(i: Iterator[TriplePattern], timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(combineWithShutdownFuture(asyncAddTriplePatterns(i)), timeout)
  }

  def addEncodedTriple(sId: Int, pId: Int, oId: Int, timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(asyncAddEncodedTriple(sId, pId, oId), timeout)
  }

  def count(q: Seq[TriplePattern], timeout: Duration = defaultBlockingOperationTimeout): Long = {
    Await.result(combineWithShutdownFuture(asyncCount(q)), timeout)
      .getOrElse(throw new Exception("Insufficient tickets to completely execute counting query."))
  }

  def getIndexAt(indexId: Long, timeout: Duration = defaultBlockingOperationTimeout): Array[Int] = {
    Await.result(combineWithShutdownFuture(asyncGetIndexAt(indexId)), timeout)
  }

  def loadFromStream(
    inputStream: InputStream,
    timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(combineWithShutdownFuture(asyncLoadFromStream(inputStream)), timeout)
  }

  /**
   * String encoding:
   * By default something is interpreted as an IRI.
   * If something starts with a hyphen or a digit, it is interpreted as an integer literal
   * If something starts with '"' it is interpreted as a string literal.
   * If something has an extra '<' prefix, then the remainder is interpreted as an XML literal.
   * If something starts with '_', then the remainder is assumed to be a blank node ID where uniqueness is the
   * responsibility of the caller.
   */
  def addStringTriple(s: String, p: String, o: String, timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(combineWithShutdownFuture(asyncAddStringTriple(s, p, o)), timeout)
  }

  /**
   * String encoding:
   * By default something is interpreted as an IRI.
   * If something starts with a hyphen or a digit, it is interpreted as an integer literal
   * If something starts with '"' it is interpreted as a string literal.
   * If something has an extra '<' prefix, then the remainder is interpreted as an XML literal.
   * If something starts with '_', then the remainder is assumed to be a blank node ID where uniqueness is the
   * responsibility of the caller.
   */
  def addStringTriples(i: Iterator[(String, String, String)], timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(combineWithShutdownFuture(asyncAddStringTriples(i)), timeout)
  }

  def addTriple(triple: JenaTriple, timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(combineWithShutdownFuture(asyncAddTriple(triple)), timeout)
  }

  def addTriples(i: Iterator[JenaTriple], timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(combineWithShutdownFuture(asyncAddTriples(i)), timeout)
  }

  def addTriplePattern(tp: TriplePattern, timeout: Duration = defaultBlockingOperationTimeout): Unit = {
    Await.result(combineWithShutdownFuture(asyncAddTriplePattern(tp)), timeout)
  }

}
