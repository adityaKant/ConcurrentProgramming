#ifndef THREADLOCAL_H_
#define THREADLOCAL_H_


#include <iostream>
#include <thread>
#include <map>
#include <mutex>
#include<iterator>

using namespace std;

namespace cop5618 {


template <class T>
class threadLocal {
public:

	threadLocal() {
		nReaders = 0;
	}
	~threadLocal(){}

	//disable copy, assign, move, and move assign constructors
	 threadLocal(const threadLocal&)=delete;
	 threadLocal& operator=(const threadLocal&)=delete;
	 threadLocal(threadLocal&&)=delete;
	 threadLocal& operator=(const threadLocal&&)=delete;

	 /**
	 * Returns the current thread's value.
	 * If no value has been previously set by this
	 * thread, an out_of_range exception is thrown.
	 */
	const T get() const;


	/**
	 * Sets the value of the threadLocal for the current thread
	 * to val.
	 */
	void set(T val);


	void remove();

	int getMapSize();

	/**
	 * Friend function.  Useful for debugging only
	 */
	template <typename U>
	friend std::ostream& operator<< (std::ostream& os, const threadLocal<U>& obj);

private:
//ADD PRIVATE MEMBERS
	map<thread::id,T> threadLocalMap;
	mutable mutex readerWriter, numReaders;
	mutable int nReaders;

};

//ADD DEFINITIONS
template <class T>
const T threadLocal<T>:: get() const
{
	T value;

	thread::id current_thread_id = this_thread::get_id();

	numReaders.lock();

	if(nReaders++ == 0){
		readerWriter.lock();
	}

	numReaders.unlock();

	if(threadLocalMap.find(current_thread_id) != threadLocalMap.end()){

		numReaders.lock();
		value = threadLocalMap.find(current_thread_id)->second;

		if(--nReaders == 0){
			readerWriter.unlock();
		}

		numReaders.unlock();
		return value;
	}
	else{
		numReaders.lock();
		if(--nReaders == 0)
			readerWriter.unlock();
		numReaders.unlock();
		throw std::out_of_range("");
	}

	return value;
}
template <typename T>
void threadLocal<T>::set(T value)
{
	thread::id current_thread_id = this_thread::get_id();

	readerWriter.lock();

	threadLocalMap[current_thread_id] = value;

	readerWriter.unlock();

}

template <typename T>
void threadLocal<T>::remove()
{
	thread::id current_thread_id = this_thread::get_id();

	typename map<thread::id,T>::iterator variableMapIterator;

	readerWriter.lock();

	variableMapIterator = threadLocalMap.find(current_thread_id);

	if(variableMapIterator != threadLocalMap.end()){
		threadLocalMap.erase(current_thread_id);
	}
	else{
		readerWriter.unlock();
		throw underflow_error("");
	}

	readerWriter.unlock();

}

template <typename T>
int threadLocal<T>::getMapSize(){
	return threadLocalMap.size();
}


} /* namespace cop5618 */

#endif /* THREADLOCAL_H_ */
