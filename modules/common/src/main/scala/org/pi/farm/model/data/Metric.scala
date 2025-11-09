package org.pi.farm.model.data

trait Metric[+T] {
  def value: T
  def units: String
}
