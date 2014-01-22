/*
 *  @author Philip Stutz
 *  
 *  Copyright 2014 University of Zurich
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

import scala.concurrent.Promise
import com.signalcollect.GraphEditor
import com.signalcollect.triplerush.CardinalityReply
import com.signalcollect.triplerush.CardinalityRequest
import com.signalcollect.triplerush.optimizers.Optimizer
import com.signalcollect.triplerush.QueryParticle
import com.signalcollect.triplerush.QueryParticle.arrayToParticle
import com.signalcollect.triplerush.QuerySpecification
import com.signalcollect.triplerush.TriplePattern
import com.signalcollect.triplerush.util.ArrayOfArraysTraversable
import com.signalcollect.triplerush.QueryIds
import com.signalcollect.triplerush.ChildIdRequest
import com.signalcollect.triplerush.ChildIdReply

final class IndexQueryVertex(
  val indexId: TriplePattern,
  val resultPromise: Promise[Set[Int]]) extends BaseVertex[Int, Any, Nothing] {

  val id = QueryIds.nextQueryId

  override def afterInitialization(graphEditor: GraphEditor[Any, Any]) {
    graphEditor.sendSignal(ChildIdRequest(id), indexId, None)
  }

  override def deliverSignal(signal: Any, sourceId: Option[Any], graphEditor: GraphEditor[Any, Any]): Boolean = {
    signal match {
      case ChildIdReply(intSet) =>
        resultPromise.success(intSet)
        graphEditor.removeVertex(id)
    }
    true
  }

}