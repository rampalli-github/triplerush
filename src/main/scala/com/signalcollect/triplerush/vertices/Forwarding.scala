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

import com.signalcollect.GraphEditor
import com.signalcollect.triplerush.QueryParticle.arrayToParticle
import com.signalcollect.triplerush.EfficientIndexPattern
import com.signalcollect.triplerush.OperationIds

trait Forwarding[StateType] {

  this: IndexVertex[StateType] =>

  override def targetIds: Traversable[Long] = {
    new Traversable[Long] {
      def foreach[U](f: Long => U): Unit = {
        foreachChildDelta { delta =>
          f(nextRoutingAddress(delta))
        }
      }
    }
  }

  def nextRoutingAddress(childDelta: Int): Long

  override def processQuery(query: Array[Int], graphEditor: GraphEditor[Long, Any]): Unit = {
    val edges = edgeCount
    val totalTickets = query.tickets
    val absoluteValueOfTotalTickets = if (totalTickets < 0) -totalTickets else totalTickets // inlined math.abs
    val avg = absoluteValueOfTotalTickets / edges
    val complete = avg > 0 && totalTickets > 0
    var extras = absoluteValueOfTotalTickets % edges
    val averageTicketQuery = query.copyWithTickets(avg, complete)
    val aboveAverageTicketQuery = query.copyWithTickets(avg + 1, complete)
    def sendTo(childDelta: Int): Unit = {
      val routingAddress = nextRoutingAddress(childDelta)
      if (extras > 0) {
        extras -= 1
        graphEditor.sendSignal(aboveAverageTicketQuery, routingAddress)
      } else if (avg > 0) {
        graphEditor.sendSignal(averageTicketQuery, routingAddress)
      }
    }
    foreachChildDelta(sendTo)
  }

}
