package ru.ifmo.backend_2021.utils

object FinallyClose {
  def apply[A <: AutoCloseable, R](o: A)(f: A => R): R = try f(o) finally o.close()
  def apply[A, R](close: A => Unit)(o: A)(f: A => R): R = try f(o) finally close(o)
}