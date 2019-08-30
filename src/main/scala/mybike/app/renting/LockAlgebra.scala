package mybike.app.renting

import mybike.domain.{Lock, LockId}

trait LocksStoreAlg[F[_]] {
  def findAll: F[List[Lock]]
  def find(id: LockId): F[Option[Lock]]
  def save(lock: Lock): F[Unit]
  def disable(id: LockId): F[Unit]
}

import cats.effect.concurrent.Ref
import cats.effect.Sync

class MemLocksStoreStore[F[_]](ref: Ref[F, Map[LockId, Lock]])(
  implicit S: Sync[F]
) extends LocksStoreAlg[F] {
  import cats.implicits._

  override def findAll: F[List[Lock]] = ref.get.map(_.values.toList)

  override def find(id: LockId): F[Option[Lock]] = findAll.map(_.find(_.id == id))

  override def save(lock: Lock): F[Unit] = S.delay {
    ref.modify { previous: Map[LockId, Lock] =>
      (previous + (lock.id -> lock), previous)
    }
  }

  override def disable(id: LockId): F[Unit] = find(id).map {
    case Some(lock) =>
      val newLock = lock.copy(open = false)
      save(newLock)
    case None => S.unit
  }
}
