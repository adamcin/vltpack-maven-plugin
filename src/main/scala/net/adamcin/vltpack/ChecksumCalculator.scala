package net.adamcin.vltpack

import java.io.File
import java.security.MessageDigest

import scalax.io.Resource

class ChecksumCalculator {
  final val md = MessageDigest.getInstance("SHA-1")
  final val nullBytes = "null".getBytes("UTF-8")

  private def update(item: Array[Byte]): ChecksumCalculator = {
    md.update(item)
    this
  }

  def addNull(): ChecksumCalculator = {
    md.update(nullBytes)
    this
  }

  def add(item: AnyVal): ChecksumCalculator = {
    Option(item) match {
      case Some(v) => add(v.toString)
      case None => addNull()
    }
  }

  def add(item: String): ChecksumCalculator = {
    Option(item) match {
      case Some(s) => update(s.getBytes("UTF-8"))
      case None => addNull()
    }
  }

  def add(item: File): ChecksumCalculator = {
    Option(item) match {
      case Some(file) => add(file.getAbsolutePath)
      case None => addNull()
    }
  }

  def addContents(item: File): ChecksumCalculator = {
    Option(item) match {
      case Some(file) => {
        if (file.exists()) {
          Resource.fromFile(file).bytes.sliding(1024).foreach((bytes) => update(bytes.toArray))
          this
        } else {
          addNull()
        }
      }
      case None => addNull()
    }
  }

  def add(item: Map[String, String]): ChecksumCalculator = {
    Option(item) match {
      case Some(map) => add(map.toString)
      case None => addNull()
    }
  }

  def calculate(): String = md.digest().map(0xFF & _).map { "%02x".format(_) }.mkString("")
}
