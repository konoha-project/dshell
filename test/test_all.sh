#!/usr/bin/env bash

REC=""
if [ $# -eq 1 ]; then
	REC="--rec $1"
fi

LOG="./test-result.log"
echo > $LOG
for test_dir in `ls ${TEST_DIR}/tests`; do
	for test_file in `ls ${TEST_DIR}/tests/$test_dir`; do
		echo "====== Test: ${test_file} ======" >> $LOG 2>&1
		dshell $REC $TEST_DIR/tests/$test_dir/$test_file >> $LOG 2>&1
		ret=$?
		prefix="[fail]:"
		if [ $ret -eq 0 ]; then
			prefix="[pass]:"
		fi
		echo "${prefix} ${test_file}"
		echo "${prefix} ${test_file}" >> $LOG 2>&1
	done
done
