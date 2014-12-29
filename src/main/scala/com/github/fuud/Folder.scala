package com.github.fuud

import scala.collection.GenTraversableOnce
import scala.collection.immutable.Seq

case class Folder[Key, Item](key: Key, items: Seq[Item], subFolders: Seq[Folder[Key, Item]]) {

  def isEmpty: Boolean = items.isEmpty && subFolders.isEmpty
  def nonEmpty: Boolean = !isEmpty

  def map[A](func: Item => A): Folder[Key, A] =
    Folder(
      key,
      items.map(func),
      subFolders.map(_.map(func)))

  def flatMap[A](func: Item => GenTraversableOnce[A]): Folder[Key, A] =
    Folder(
      key,
      items.flatMap(func),
      subFolders.map(_.flatMap(func)))

  def mapKeys[A](func: Key => A): Folder[A, Item] =
    Folder(
      func(key),
      items,
      subFolders.map(_.mapKeys(func)))

  def filter(pred: Item => Boolean): Folder[Key, Item] =
    Folder(
      key,
      items.filter(pred),
      subFolders.map(_.filter(pred)))

  def cleanEmpty: Folder[Key, Item] =
    Folder(
      key,
      items,
      subFolders.map(_.cleanEmpty).filter(_.nonEmpty))

  def print(indent: Int = 0): Unit = {
    println(("  " * indent) + s"[$key]")
    items.foreach(i => println(("  " * (indent + 1)) + i))
    subFolders.foreach(_.print(indent + 1))
  }
}
