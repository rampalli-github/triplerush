/*
 *  @author Philip Stutz
 *  @author Mihaela Verman
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

package com.signalcollect.triplerush.vertices

import com.signalcollect._

/**
 * Base implementation for all TripleRush vertices.
 */
abstract class BaseVertex[StateType]
    extends Vertex[Long, StateType, Long, Any] {

  @transient var state: StateType = _

  def setState(s: StateType): Unit = {
    state = s
  }

  def targetIds: Traversable[Long] = None
  
  override def deliverSignalWithSourceId(
    signal: Any, sourceId: Long,
    graphEditor: GraphEditor[Long, Any]): Boolean =
    throw new Exception("TripleRush only uses messages without the source ID.")

  override def scoreCollect: Double = 0

  override def scoreSignal: Double = 0

  override def toString: String = s"${this.getClass.getName}(id=$id)"

  override def executeSignalOperation(graphEditor: GraphEditor[Long, Any]): Unit = {
    throw new Exception("Should not be called, signals on delivery.")
  }

  override def executeCollectOperation(graphEditor: GraphEditor[Long, Any]): Unit = {
    throw new Exception("Should not be called, collects and signals on delivery.")
  }

  override def afterInitialization(graphEditor: GraphEditor[Long, Any]): Unit = {}

  override def beforeRemoval(graphEditor: GraphEditor[Long, Any]): Unit = {}

  override def edgeCount: Int = 0

  override def addEdge(e: Edge[Long], graphEditor: GraphEditor[Long, Any]): Boolean = {
    throw new UnsupportedOperationException
  }

  override def removeEdge(targetId: Long, graphEditor: GraphEditor[Long, Any]): Boolean = {
    throw new UnsupportedOperationException
  }

  override def removeAllEdges(graphEditor: GraphEditor[Long, Any]): Int = 0
}
