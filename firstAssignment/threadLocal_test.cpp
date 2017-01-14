#include <iostream>
#include <atomic>
#include <thread>
#include <mutex>
#include <vector>
#include <algorithm>
#include <exception>
#include <list>
#include "threadLocal.h"

using namespace std;
using namespace cop5618;

int test_threadLocal_0(int num_threads);
int test_threadLocal_1(int num_threads);
int test_threadLocal_2(int num_threads);
int test_threadLocal_3(int num_threads);


int test_threadLocal() {
	int num_errs = 0;

	num_errs += test_threadLocal_0(10);
	num_errs += test_threadLocal_1(10);
	num_errs += test_threadLocal_2(10);
	num_errs += test_threadLocal_3(10);

	return num_errs;
}

//test to check if get() is returning correct value and remove is able to delete existing value.
int test_threadLocal_0(const int num_threads)
{

	std::vector<std::thread> threadsVector;
	threadLocal <int> threadLocalVar1;
	threadLocal <int> threadLocalVar2;


	for (int t = 0; t < num_threads; t++) {
		threadsVector.push_back(
				std::thread(
						[&threadLocalVar1, &threadLocalVar2, t]() mutable ->void {
							{

								threadLocalVar1.set(t);
								threadLocalVar2.set(t*2);
							}
						}));

	}

	//join all the threads
	std::for_each(threadsVector.begin(), threadsVector.end(),
			std::mem_fn(&std::thread::join));


	if (threadLocalVar1.getMapSize() != num_threads) {
		return 1;
	}

	if (threadLocalVar2.getMapSize() != num_threads) {
		return 1;
	}

	return 0;
}


int test_threadLocal_1(const int num_threads) {

	std::vector<std::thread> threadsVector;
	threadLocal<int> threadLocalVar1;
	atomic<int> errors;
	errors = 0;

	for (int i = 0; i < num_threads; i++) {
		threadsVector.push_back(
				std::thread(
						[&threadLocalVar1, i,&errors]() mutable ->void {
							{
								threadLocalVar1.set(i);
								try
								{
									int setValue = i;

									if(threadLocalVar1.get() != setValue)
										errors++;


									//modifying threadLocalVar1 and checking if get returns the same value
									setValue = i*5;
									threadLocalVar1.set(i*5);
									if(threadLocalVar1.get() != setValue)
										errors++;

								}
								catch(out_of_range &e)
								{
									errors++;
								}

								try
								{
									threadLocalVar1.remove();
									//getting the value after performing the remove
									if(threadLocalVar1.get())
										errors++;

								}
								catch(out_of_range &e)
								{
									//expected behavior when getting the value after the value
								}
								catch(underflow_error &e){
									errors++;
								}

							}
						}));

	}

	//join all the threads
	std::for_each(threadsVector.begin(), threadsVector.end(),
			std::mem_fn(&std::thread::join));


	return errors;
}

int test_threadLocal_2(const int num_threads) {

	std::vector<std::thread> threadsVector;
	threadLocal<int> threadLocalVar1;
	atomic<int> errors;
	errors = 0;
	int var1forThread[2];
	for (int i = 0; i < num_threads; i++) {
		threadsVector.push_back(
				std::thread(
						[&threadLocalVar1, i,&errors, &var1forThread]() mutable ->void {
							{
								threadLocalVar1.set(i);
								threadLocalVar1.set(i*2);
								try
								{
									threadLocalVar1.remove();
								}
								catch(underflow_error &e)
								{
									errors++;
								}

							}
						}));

	}

	//join all the threads
	std::for_each(threadsVector.begin(), threadsVector.end(),
			std::mem_fn(&std::thread::join));

	return errors;
}

int test_threadLocal_3(const int num_threads)
{

	std::vector<std::thread> threadsVector;
	threadLocal <int> threadLocalVar1;
	atomic<int> errors;
	errors = 0;

	for (int i = 0; i < num_threads; i++) {
		threadsVector.push_back(
				std::thread(
						[&threadLocalVar1, i, &errors]() mutable ->void {
							{
								//again setting the value and performed remove twice.
								threadLocalVar1.set(i);
								try{
									threadLocalVar1.remove();
									threadLocalVar1.remove();
									errors++;
								}catch(underflow_error &e){
//									cout<<"expected behavior when performing remove twice";
								}
							}
						}));

	}

	//join all the threads
	std::for_each(threadsVector.begin(), threadsVector.end(),
			std::mem_fn(&std::thread::join));


	return errors;
}
