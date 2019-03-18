package com.ilyak.pifarm.flow.configuration

import com.ilyak.pifarm.flow.configuration.Connection.{External, TConnection}
import com.ilyak.pifarm.flow.configuration.ShapeConnections.{ExternalInputs, ExternalOutputs, Inputs, Outputs}

import scala.language.higherKinds

object ConnectionHelper {

  private def mapConnections[C[_] <: TConnection[_]](s: Seq[C[_]]): Map[String, C[_]] =
    s.map(c => c.name -> c).toMap

  implicit class ToInputs(val in: Seq[Connection.In[_]]) extends AnyVal {
    def toInputs: Inputs = mapConnections(in)
  }

  implicit class ToOutputs(val out: Seq[Connection.Out[_]]) extends AnyVal {
    def toOutputs: Outputs = mapConnections(out)
  }

  implicit class ToIntInputs(val in: Seq[Connection.Out[_]]) extends AnyVal {
    def toIntInputs: Outputs = mapConnections(in)
  }

  implicit class ToIntOutputs(val in: Seq[Connection.In[_]]) extends AnyVal {
    def toIntOutputs: Inputs = mapConnections(in)
  }
  implicit class ToExtInputs(val in: Seq[External.In[_]]) extends AnyVal {
    def toExtInputs: ExternalInputs = mapConnections(in)
  }

  implicit class ToExtOutputs(val out: Seq[External.Out[_]]) extends AnyVal {
    def toExtOutputs: ExternalOutputs = mapConnections(out)
  }
}
