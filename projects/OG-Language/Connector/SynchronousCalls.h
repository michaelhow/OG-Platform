/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

#ifndef __inc_og_language_connector_synchronouscalls_h
#define __inc_og_language_connector_synchronouscalls_h

// Blocking slots for synchronous calls

class CSynchronousCalls;

class CSynchronousCallSlot {
private:
	CSynchronousCalls *m_poOwner;
	CAtomicPointer<FudgeMsg> m_msg;
	int m_nIdentifier;
	CAtomicInt m_oSequence;
	CSemaphore m_sem;
	friend class CSynchronousCalls;
	CSynchronousCallSlot (CSynchronousCalls *poOwner, int nIdentifier);
	~CSynchronousCallSlot ();
	void ResetSemaphore () { m_sem.Wait (0); }
	void SignalSemaphore () { m_sem.Signal (); }
	void PostAndRelease (FudgeMsg msg);
public:
	fudge_i32 GetHandle ();
	int GetIdentifier () { return m_nIdentifier; }
	int GetSequence () { return m_oSequence.Get (); }
	FudgeMsg GetMessage (unsigned long lTimeout);
	void Release ();
};

class CSynchronousCalls {
private:
	friend class CSynchronousCallSlot;
	CMutex m_mutex;
	CSynchronousCallSlot **m_ppoSlots;
	int m_nAllocatedSlots;
	CSynchronousCallSlot **m_ppoFreeSlots;
	int m_nFreeSlots;
	int m_nMaxFreeSlots;
	void Release (CSynchronousCallSlot *poSlot);
public:
	CSynchronousCalls ();
	~CSynchronousCalls ();
	void ClearAllSemaphores ();
	void SignalAllSemaphores ();
	CSynchronousCallSlot *Acquire ();
	void PostAndRelease (fudge_i32 handle, FudgeMsg msg);
};

#endif /* ifndef __inc_og_language_connector_synchronouscalls_h */