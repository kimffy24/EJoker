package pro.jk.ejoker.common.framework.enhance;

import java.util.concurrent.locks.ReadWriteLock;

import pro.jk.ejoker.common.system.wrapper.RWLockWrapper;

public abstract class EasyCleanMailbox {

	private final ReadWriteLock rwLock = RWLockWrapper.createRWLock();
	
	/**
	 * 包装读锁为使用锁
	 * @return
	 */
	public boolean tryUse() {
		return rwLock.readLock().tryLock();
//		return true;
	}
	
	public void releaseUse() {
		rwLock.readLock().unlock();
	}

	/**
	 * 包装写锁为清理锁
	 * @return
	 */
	public boolean tryClean() {
		return rwLock.writeLock().tryLock();
//		return false;
	}
	
	public void releaseClean() {
		rwLock.writeLock().unlock();
	}
	
}
